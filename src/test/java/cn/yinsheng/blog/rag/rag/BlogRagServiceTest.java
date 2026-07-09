package cn.yinsheng.blog.rag.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.yinsheng.blog.rag.chat.ChatLimiter;
import cn.yinsheng.blog.rag.compute.AiComputeClient;
import cn.yinsheng.blog.rag.config.RagProperties;
import cn.yinsheng.blog.rag.model.ChatResponse;
import cn.yinsheng.blog.rag.model.RetrievedChunk;
import java.util.List;
import org.junit.jupiter.api.Test;

class BlogRagServiceTest {
  @Test
  void shouldFallbackWhenNoConfidentMatch() {
    AiComputeClient aiComputeClient = mock(AiComputeClient.class);
    BlogRetriever retriever = mock(BlogRetriever.class);
    when(retriever.retrieve("没有命中的问题")).thenReturn(List.of(chunk(0.1)));
    BlogRagService service = new BlogRagService(
        aiComputeClient,
        new ChatLimiter(ragProperties()),
        retriever,
        new BlogPromptBuilder(ragProperties()),
        new CitationBuilder(),
        new RelatedPostBuilder()
    );

    ChatResponse response = service.answer("没有命中的问题");

    assertThat(response.answer()).contains("没有在博客里找到足够确定");
    assertThat(response.citations()).isEmpty();
    verify(aiComputeClient, never()).chat(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
  }

  private RetrievedChunk chunk(double score) {
    return new RetrievedChunk(score, "chunk", "post", "标题", "小节", "/post/#section", "内容", List.of("tag"), "2026-01-01T00:00:00Z");
  }

  private RagProperties ragProperties() {
    return new RagProperties(
        "http://ai",
        "",
        "chat",
        "embed",
        "http://qdrant",
        "blog_chunks",
        1024,
        3,
        8000,
        600,
        1,
        0,
        120,
        "/content",
        "/tmp/index.db",
        6,
        "http://status",
        "/",
        "/tmp/rate.db",
        10,
        100
    );
  }
}
