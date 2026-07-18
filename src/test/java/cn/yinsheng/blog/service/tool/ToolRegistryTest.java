package cn.yinsheng.blog.service.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolRegistryTest {
  @Test
  void shouldExposeOnlyRegisteredTools() {
    ToolRegistry.ToolHandler first = handler("blog_qa");
    ToolRegistry.ToolHandler second = handler("weather");
    ToolRegistry registry = new ToolRegistry(List.of(first, second));

    assertThat(registry.definitions()).extracting(ToolDefinition::name)
        .containsExactly("blog_qa", "weather");
  }

  private ToolRegistry.ToolHandler handler(String name) {
    return new ToolRegistry.ToolHandler() {
      @Override
      public ToolDefinition definition() {
        return new ToolDefinition(name, name, Map.of("type", "object"));
      }

      @Override
      public ToolResult execute(ToolCall call, ToolExecutionContext context) {
        return ToolResult.success(call, "ok");
      }
    };
  }
}
