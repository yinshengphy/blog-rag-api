package cn.yinsheng.blog.service.indexer;

import cn.yinsheng.blog.service.chat.ChatLimiter;
import cn.yinsheng.blog.service.compute.AiComputeClient;
import cn.yinsheng.blog.service.config.RagProperties;
import cn.yinsheng.blog.service.model.ChunkRecord;
import cn.yinsheng.blog.service.qdrant.QdrantClient;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BlogIndexService {
  private static final Logger log = LoggerFactory.getLogger(BlogIndexService.class);

  private final RagProperties properties;
  private final MarkdownPostReader postReader;
  private final MarkdownChunker chunker;
  private final IndexStatusRepository statusRepository;
  private final AiComputeClient aiComputeClient;
  private final QdrantClient qdrantClient;
  private final ChatLimiter chatLimiter;
  private final ReentrantLock indexLock = new ReentrantLock();

  public BlogIndexService(
      RagProperties properties,
      MarkdownPostReader postReader,
      MarkdownChunker chunker,
      IndexStatusRepository statusRepository,
      AiComputeClient aiComputeClient,
      QdrantClient qdrantClient,
      ChatLimiter chatLimiter
  ) {
    this.properties = properties;
    this.postReader = postReader;
    this.chunker = chunker;
    this.statusRepository = statusRepository;
    this.aiComputeClient = aiComputeClient;
    this.qdrantClient = qdrantClient;
    this.chatLimiter = chatLimiter;
  }

  public IndexResult synchronize() {
    return execute(IndexMode.INCREMENTAL);
  }

  public IndexResult rebuild() {
    return execute(IndexMode.REBUILD);
  }

  private IndexResult execute(IndexMode mode) {
    if (!indexLock.tryLock()) {
      return result(mode, "BUSY", 0, 0, 0, 0, List.of(), "已有索引任务正在执行");
    }
    try {
      if (chatLimiter.isBusy()) {
        return result(mode, "BUSY", 0, 0, 0, 0, List.of(), "聊天服务正在使用模型，请稍后重试");
      }
      return doIndex(mode);
    } catch (Exception ex) {
      log.error("博客索引失败，模式={}", mode, ex);
      return result(mode, "FAILED", 0, 0, 0, 0, List.of(), "索引失败：" + ex.getMessage());
    } finally {
      indexLock.unlock();
    }
  }

  private IndexResult doIndex(IndexMode mode) throws Exception {
    List<BlogPost> posts = postReader.readPosts(Path.of(properties.contentDir()));
    Map<String, BlogPost> currentPosts = new LinkedHashMap<>();
    posts.forEach(post -> currentPosts.put(post.slug(), post));

    if (mode == IndexMode.REBUILD) {
      qdrantClient.recreateCollection(properties.embeddingDimension());
      statusRepository.clear(properties.indexDbPath());
    } else {
      qdrantClient.ensureCollection(properties.embeddingDimension());
    }

    Map<String, IndexStatusRepository.StatusRow> statusRows = statusRepository.load(properties.indexDbPath());
    int deleted = 0;
    for (String indexedSlug : new ArrayList<>(statusRows.keySet())) {
      if (!currentPosts.containsKey(indexedSlug)) {
        qdrantClient.deleteBySlug(indexedSlug);
        statusRepository.delete(properties.indexDbPath(), indexedSlug);
        deleted++;
      }
    }

    int skipped = 0;
    List<String> indexedSlugs = new ArrayList<>();
    for (BlogPost post : posts) {
      IndexStatusRepository.StatusRow row = statusRows.get(post.slug());
      if (mode == IndexMode.INCREMENTAL && row != null && row.contentHash().equals(post.contentHash())) {
        skipped++;
        continue;
      }
      if (chatLimiter.isBusy()) {
        return result(
            mode,
            "BUSY",
            posts.size(),
            indexedSlugs.size(),
            skipped,
            deleted,
            indexedSlugs,
            "聊天请求开始执行，索引已暂停；再次调用会从未完成文章继续"
        );
      }
      indexPost(post);
      indexedSlugs.add(post.slug());
    }
    return result(
        mode,
        "COMPLETED",
        posts.size(),
        indexedSlugs.size(),
        skipped,
        deleted,
        indexedSlugs,
        mode == IndexMode.REBUILD ? "全部博客索引重建完成" : "博客索引同步完成"
    );
  }

  private void indexPost(BlogPost post) {
    List<ChunkRecord> chunks = chunker.chunk(post);
    List<List<Double>> vectors = new ArrayList<>();
    for (ChunkRecord chunk : chunks) {
      vectors.add(aiComputeClient.embed(chunk.retrievalText()));
    }
    if (!vectors.isEmpty()) {
      qdrantClient.ensureCollection(vectors.get(0).size());
    }
    qdrantClient.deleteBySlug(post.slug());
    int batchSize = Math.max(1, properties.indexerBatchSize());
    for (int start = 0; start < chunks.size(); start += batchSize) {
      int end = Math.min(chunks.size(), start + batchSize);
      qdrantClient.upsert(chunks.subList(start, end), vectors.subList(start, end));
    }
    statusRepository.upsert(properties.indexDbPath(), post, chunks.size());
    log.info("博客已建立索引，slug={}，分块数={}", post.slug(), chunks.size());
  }

  private IndexResult result(
      IndexMode mode,
      String status,
      int total,
      int indexed,
      int skipped,
      int deleted,
      List<String> slugs,
      String message
  ) {
    return new IndexResult(mode, status, total, indexed, skipped, deleted, List.copyOf(slugs), message);
  }
}

