package cn.yinsheng.blog.rag.rag;

import cn.yinsheng.blog.rag.compute.AiComputeClient;
import cn.yinsheng.blog.rag.config.RagProperties;
import cn.yinsheng.blog.rag.model.RetrievedChunk;
import cn.yinsheng.blog.rag.qdrant.QdrantClient;
import java.util.List;
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
    return blogReranker.rerank(question, retrieved).stream()
        .limit(properties.topK())
        .toList();
  }
}
