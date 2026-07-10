package cn.yinsheng.blog.rag.tool;

import static org.assertj.core.api.Assertions.assertThat;

import cn.yinsheng.blog.rag.BlogRagApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = BlogRagApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PublicToolSetTest {
  @Autowired
  private ToolRegistry registry;

  @Test
  void shouldExposeExactlyFourPublicTools() {
    assertThat(registry.definitions()).extracting(ToolDefinition::name)
        .containsExactlyInAnyOrder("blog_qa", "blog_summary", "weather", "web_research");
  }
}
