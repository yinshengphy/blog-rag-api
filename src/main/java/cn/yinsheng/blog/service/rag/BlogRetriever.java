package cn.yinsheng.blog.service.rag;

import cn.yinsheng.blog.service.compute.AiComputeClient;
import cn.yinsheng.blog.service.config.RagProperties;
import cn.yinsheng.blog.service.model.RetrievedChunk;
import cn.yinsheng.blog.service.qdrant.QdrantClient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
      if (blogReranker.lexicalScore(question, chunk) >= 0.08) {
        candidates.putIfAbsent(chunk.chunkId(), chunk);
      }
    }
    return blogReranker.rerank(question, candidates.values().stream().toList()).stream()
        .limit(properties.topK())
        .toList();
  }
}
