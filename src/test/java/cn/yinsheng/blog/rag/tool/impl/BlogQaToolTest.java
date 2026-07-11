package cn.yinsheng.blog.rag.tool.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.yinsheng.blog.rag.model.PageContext;
import cn.yinsheng.blog.rag.model.RetrievedChunk;
import cn.yinsheng.blog.rag.rag.BlogCatalogService;
import cn.yinsheng.blog.rag.rag.BlogRetriever;
import cn.yinsheng.blog.rag.rag.CitationBuilder;
import cn.yinsheng.blog.rag.rag.RelatedPostBuilder;
import cn.yinsheng.blog.rag.tool.ToolCall;
import cn.yinsheng.blog.rag.tool.ToolExecutionContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BlogQaToolTest {
  @Test
  void shouldFilterRetrievalByCurrentPost() {
    BlogRetriever retriever = mock(BlogRetriever.class);
    RetrievedChunk chunk = new RetrievedChunk(0.8, "rsa-0001", 1, "rsa", "RSA", "命名", "/rsa/#命名", "RSA 名称来自三位作者。", List.of(), "");
    when(retriever.retrieve("名称怎么来的", "rsa")).thenReturn(List.of(chunk));
    BlogQaTool tool = new BlogQaTool(retriever, mock(BlogCatalogService.class), new CitationBuilder(), new RelatedPostBuilder());

    var result = tool.execute(
        new ToolCall("1", "blog_qa", Map.of("query", "名称怎么来的", "task", "LOCATE", "scope", "CURRENT_POST")),
        new ToolExecutionContext("trace", "session", new PageContext("BLOG_POST", "rsa", "RSA", "/rsa/", ""))
    );

    assertThat(result.success()).isTrue();
    assertThat(result.content()).startsWith("Task: LOCATE");
    assertThat(result.metadata()).containsEntry("task", "LOCATE");
    assertThat(result.metadata()).containsEntry("slug", "rsa");
    verify(retriever).retrieve("名称怎么来的", "rsa");
  }
}
