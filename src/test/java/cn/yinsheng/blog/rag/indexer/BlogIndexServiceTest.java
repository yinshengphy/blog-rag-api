package cn.yinsheng.blog.rag.indexer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.yinsheng.blog.rag.chat.ChatLimiter;
import cn.yinsheng.blog.rag.compute.AiComputeClient;
import cn.yinsheng.blog.rag.config.RagProperties;
import cn.yinsheng.blog.rag.model.ChunkRecord;
import cn.yinsheng.blog.rag.qdrant.QdrantClient;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BlogIndexServiceTest {
  private RagProperties properties;
  private MarkdownPostReader postReader;
  private MarkdownChunker chunker;
  private IndexStatusRepository statusRepository;
  private AiComputeClient aiComputeClient;
  private QdrantClient qdrantClient;
  private ChatLimiter chatLimiter;
  private BlogIndexService service;

  @BeforeEach
  void setUp() {
    properties = mock(RagProperties.class);
    postReader = mock(MarkdownPostReader.class);
    chunker = mock(MarkdownChunker.class);
    statusRepository = mock(IndexStatusRepository.class);
    aiComputeClient = mock(AiComputeClient.class);
    qdrantClient = mock(QdrantClient.class);
    chatLimiter = mock(ChatLimiter.class);
    when(properties.contentDir()).thenReturn("/content/posts");
    when(properties.indexDbPath()).thenReturn("/data/index.db");
    when(properties.embeddingDimension()).thenReturn(1024);
    when(properties.indexerBatchSize()).thenReturn(6);
    service = new BlogIndexService(
        properties, postReader, chunker, statusRepository, aiComputeClient, qdrantClient, chatLimiter
    );
  }

  @Test
  void 增量索引跳过内容未变化的文章() throws Exception {
    BlogPost post = post("same-hash");
    when(postReader.readPosts(any())).thenReturn(List.of(post));
    when(statusRepository.load("/data/index.db")).thenReturn(Map.of(
        post.slug(), new IndexStatusRepository.StatusRow(post.slug(), post.title(), "same-hash", "", "", 1)
    ));

    IndexResult result = service.synchronize();

    assertThat(result.completed()).isTrue();
    assertThat(result.skippedPosts()).isEqualTo(1);
    verify(aiComputeClient, never()).embed(any());
  }

  @Test
  void 全量模式重建集合并索引全部文章() throws Exception {
    BlogPost post = post("new-hash");
    ChunkRecord chunk = new ChunkRecord(
        "post-0", 0, "post", "文章", "正文", "正文", "/post/#正文", "内容",
        "new-hash", "chunk-hash", List.of("RAG"), "2026-01-01", "2026-01-01"
    );
    when(postReader.readPosts(any())).thenReturn(List.of(post));
    when(statusRepository.load("/data/index.db")).thenReturn(Map.of());
    when(chunker.chunk(post)).thenReturn(List.of(chunk));
    when(aiComputeClient.embed(any())).thenReturn(List.of(0.1, 0.2));

    IndexResult result = service.rebuild();

    assertThat(result.completed()).isTrue();
    assertThat(result.indexedSlugs()).containsExactly("post");
    verify(qdrantClient).recreateCollection(1024);
    verify(statusRepository).clear("/data/index.db");
    verify(qdrantClient).upsert(anyList(), anyList());
  }

  @Test
  void 聊天忙时不访问向量数据库() {
    when(chatLimiter.isBusy()).thenReturn(true);

    IndexResult result = service.synchronize();

    assertThat(result.status()).isEqualTo("BUSY");
    verify(qdrantClient, never()).ensureCollection(1024);
  }

  private BlogPost post(String hash) {
    return new BlogPost("post", "文章", "2026-01-01", "2026-01-01", List.of("RAG"), "", "内容", hash);
  }
}

