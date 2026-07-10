package cn.yinsheng.blog.rag.tool.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.yinsheng.blog.rag.compute.AiComputeClient;
import cn.yinsheng.blog.rag.model.PageContext;
import cn.yinsheng.blog.rag.model.RetrievedChunk;
import cn.yinsheng.blog.rag.qdrant.QdrantClient;
import cn.yinsheng.blog.rag.rag.BlogCatalogService;
import cn.yinsheng.blog.rag.rag.CitationBuilder;
import cn.yinsheng.blog.rag.rag.RelatedPostBuilder;
import cn.yinsheng.blog.rag.tool.ToolCall;
import cn.yinsheng.blog.rag.tool.ToolExecutionContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BlogSummaryToolTest {
  @Test
  void shouldReadAllOrderedChunksForCurrentPost() {
    QdrantClient qdrant = mock(QdrantClient.class);
    List<RetrievedChunk> chunks = List.of(
        chunk(1, "第一节", "第一段"),
        chunk(2, "第二节", "第二段"),
        chunk(3, "第三节", "第三段"),
        chunk(4, "第四节", "第四段")
    );
    when(qdrant.listBySlug("rsa")).thenReturn(chunks);
    AiComputeClient ai = mock(AiComputeClient.class);
    when(ai.chat(anyString(), anyString())).thenReturn("完整摘要");
    BlogSummaryTool tool = new BlogSummaryTool(
        qdrant, mock(BlogCatalogService.class), ai, new CitationBuilder(), new RelatedPostBuilder()
    );

    var result = tool.execute(
        new ToolCall("1", "blog_summary", Map.of()),
        new ToolExecutionContext("", "", new PageContext("BLOG_POST", "rsa", "RSA", "/rsa/", ""))
    );

    assertThat(result.success()).isTrue();
    assertThat(result.content()).contains("4 ordered chunks", "完整摘要");
    assertThat(result.metadata()).containsEntry("chunkCount", 4);
  }

  private RetrievedChunk chunk(int index, String section, String content) {
    return new RetrievedChunk(1, "rsa-%04d".formatted(index), index, "rsa", "RSA", section, "/rsa/#" + index, content, List.of(), "");
  }
}
