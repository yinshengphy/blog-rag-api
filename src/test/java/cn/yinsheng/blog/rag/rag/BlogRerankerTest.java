package cn.yinsheng.blog.rag.rag;

import static org.assertj.core.api.Assertions.assertThat;

import cn.yinsheng.blog.rag.model.RetrievedChunk;
import java.util.List;
import org.junit.jupiter.api.Test;

class BlogRerankerTest {
  private final BlogReranker reranker = new BlogReranker();

  @Test
  void shouldPromoteExactSectionMatchAndWriteBackScore() {
    RetrievedChunk semantic = chunk(0.62, "数学推导", "欧拉函数和模逆元");
    RetrievedChunk exact = chunk(0.40, "为什么叫 RSA", "名称来自 Rivest、Shamir 和 Adleman");

    List<RetrievedChunk> result = reranker.rerank("为什么叫 RSA", List.of(semantic, exact));

    assertThat(result.get(0).section()).isEqualTo("为什么叫 RSA");
    assertThat(result.get(0).score()).isGreaterThan(exact.score());
  }

  private RetrievedChunk chunk(double score, String section, String content) {
    return new RetrievedChunk(score, section, 1, "rsa", "RSA 加密原理", section, "/rsa/#" + section, content, List.of(), "");
  }
}
