package cn.yinsheng.blog.service.rag;

import cn.yinsheng.blog.service.compute.AiComputeClient;
import cn.yinsheng.blog.service.config.RagProperties;
import cn.yinsheng.blog.service.model.RetrievedChunk;
import cn.yinsheng.blog.service.qdrant.QdrantClient;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class BlogRetriever {
  private final RagProperties properties;
  private final AiComputeClient aiComputeClient;
  private final QdrantClient qdrantClient;
  private final BlogReranker blogReranker;

  public BlogRetriever(
      RagProperties properties,
      AiComputeClient aiComputeClient,
      QdrantClient qdrantClient,
      BlogReranker blogReranker
  ) {
    this.properties = properties;
    this.aiComputeClient = aiComputeClient;
    this.qdrantClient = qdrantClient;
    this.blogReranker = blogReranker;
  }

  public List<RetrievedChunk> retrieve(String question) {
    return retrieve(question, null);
  }

  public List<RetrievedChunk> retrieve(String question, String slug) {
    List<Double> questionVector = aiComputeClient.embed(question);
    int limit = Math.max(properties.topK() * 3, properties.topK());
    List<RetrievedChunk> retrieved = qdrantClient.search(questionVector, limit, slug);
    Map<String, RetrievedChunk> candidates = new LinkedHashMap<>();
    retrieved.forEach(chunk -> candidates.put(chunk.chunkId(), chunk));
    for (RetrievedChunk chunk : qdrantClient.listForRetrieval(slug)) {
      if (blogReranker.lexicalScore(question, chunk) >= 0.04) {
        candidates.putIfAbsent(chunk.chunkId(), chunk);
      }
    }
    List<RetrievedChunk> ranked = blogReranker.rerank(question, candidates.values().stream().toList());
    List<RetrievedChunk> selected = selectDiverse(ranked, slug, properties.topK());
    return expandAdjacentContext(selected, candidates.values().stream().toList());
  }

  private List<RetrievedChunk> selectDiverse(List<RetrievedChunk> ranked, String slug, int limit) {
    List<RetrievedChunk> selected = new ArrayList<>();
    Map<String, Integer> perPost = new HashMap<>();
    Set<String> sections = new HashSet<>();
    for (RetrievedChunk chunk : ranked) {
      String sectionKey = chunk.slug() + "|" + chunk.section();
      if (!sections.add(sectionKey)) continue;
      if ((slug == null || slug.isBlank()) && perPost.getOrDefault(chunk.slug(), 0) >= 2) continue;
      selected.add(chunk);
      perPost.merge(chunk.slug(), 1, Integer::sum);
      if (selected.size() >= Math.max(1, limit)) break;
    }
    return List.copyOf(selected);
  }

  private List<RetrievedChunk> expandAdjacentContext(
      List<RetrievedChunk> selected,
      List<RetrievedChunk> allChunks
  ) {
    Map<String, RetrievedChunk> byPosition = new HashMap<>();
    allChunks.forEach(chunk -> byPosition.put(chunk.slug() + "|" + chunk.chunkIndex(), chunk));
    return selected.stream().map(chunk -> {
      List<String> parts = new ArrayList<>();
      RetrievedChunk previous = byPosition.get(chunk.slug() + "|" + (chunk.chunkIndex() - 1));
      RetrievedChunk next = byPosition.get(chunk.slug() + "|" + (chunk.chunkIndex() + 1));
      if (sameSection(previous, chunk)) parts.add(previous.content());
      parts.add(chunk.content());
      if (sameSection(next, chunk)) parts.add(next.content());
      String expanded = String.join("\n\n", parts);
      if (expanded.length() > 1800) expanded = expanded.substring(0, 1800);
      return new RetrievedChunk(
          chunk.score(), chunk.chunkId(), chunk.chunkIndex(), chunk.slug(), chunk.title(), chunk.section(),
          chunk.headingPath(), chunk.url(), expanded, chunk.tags(), chunk.categories(), chunk.description(),
          chunk.date(), chunk.updatedAt()
      );
    }).toList();
  }

  private boolean sameSection(RetrievedChunk candidate, RetrievedChunk selected) {
    return candidate != null && candidate.section().equals(selected.section());
  }
}
