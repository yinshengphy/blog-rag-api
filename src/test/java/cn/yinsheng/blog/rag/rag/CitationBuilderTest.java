package cn.yinsheng.blog.rag.rag;

import static org.assertj.core.api.Assertions.assertThat;

import cn.yinsheng.blog.rag.model.Citation;
import cn.yinsheng.blog.rag.model.RetrievedChunk;
import java.util.List;
import org.junit.jupiter.api.Test;

class CitationBuilderTest {
  @Test
  void shouldFocusSnippetNearReferencedAnswerText() {
    CitationBuilder builder = new CitationBuilder();
    RetrievedChunk chunk = new RetrievedChunk(
        0.8,
        "rsa-0001",
        1,
        "rsa",
        "RSA 加密原理",
        "为什么需要 RSA？",
        "/rsa/#为什么需要-rsa",
        """
            在 RSA 出现之前，主流的加密思路是对称加密。
            对称加密最大的问题是密钥分发。
            RSA 由 Ron Rivest、Adi Shamir 和 Leonard Adleman 在 1977 年提出。
            RSA 这个名字，也正是来自他们三个人姓氏的首字母。
            """,
        List.of("RSA"),
        "2026-01-01T00:00:00Z"
    );

    List<Citation> citations = builder.build(
        List.of(chunk),
        "RSA 命名来源于三位发明者姓氏的首字母：Ron Rivest、Adi Shamir 和 Leonard Adleman [1]。"
    );

    assertThat(citations).hasSize(1);
    assertThat(citations.get(0).snippet()).contains("Ron Rivest", "姓氏的首字母");
  }
}
