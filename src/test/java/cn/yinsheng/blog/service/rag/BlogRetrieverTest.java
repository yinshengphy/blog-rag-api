package cn.yinsheng.blog.service.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.yinsheng.blog.service.compute.AiComputeClient;
import cn.yinsheng.blog.service.config.RagProperties;
import cn.yinsheng.blog.service.model.RetrievedChunk;
import cn.yinsheng.blog.service.qdrant.QdrantClient;
import java.util.List;
import org.junit.jupiter.api.Test;

class BlogRetrieverTest {
  @Test
  void shouldMergeLexicalCandidatesOutsideDenseTopResults() {
    RagProperties properties = mock(RagProperties.class);
    when(properties.topK()).thenReturn(3);
    AiComputeClient ai = mock(AiComputeClient.class);
    when(ai.embed("为什么叫 RSA")).thenReturn(List.of(0.1, 0.2));
    QdrantClient qdrant = mock(QdrantClient.class);
    RetrievedChunk semantic = chunk(0.55, "数学推导", "模逆元计算");
    RetrievedChunk exact = chunk(0, "为什么叫 RSA", "名称来自三位作者");
    when(qdrant.search(List.of(0.1, 0.2), 9, "rsa")).thenReturn(List.of(semantic));
    when(qdrant.listForRetrieval("rsa")).thenReturn(List.of(semantic, exact));
    BlogRetriever retriever = new BlogRetriever(properties, ai, qdrant, new BlogReranker());

    List<RetrievedChunk> result = retriever.retrieve("为什么叫 RSA", "rsa");

    assertThat(result).extracting(RetrievedChunk::section).contains("为什么叫 RSA");
    assertThat(result.get(0).section()).isEqualTo("为什么叫 RSA");
  }

  private RetrievedChunk chunk(double score, String section, String content) {
    return new RetrievedChunk(score, section, 1, "rsa", "RSA 加密原理", section, "/rsa/#" + section, content, List.of(), "");
  }
}
