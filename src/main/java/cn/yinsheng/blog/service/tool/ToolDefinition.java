package cn.yinsheng.blog.service.tool;

import java.util.Map;

public record ToolDefinition(
    String name,
    String description,
    Map<String, Object> parameters
) {
  public Map<String, Object> toOpenAiTool() {
    return Map.of(
        "type", "function",
        "function", Map.of(
            "name", name,
            "description", description,
            "parameters", parameters
        )
    );
  }
}
