package cn.yinsheng.blog.rag.tool;

public record ToolResult(
    String toolCallId,
    String name,
    String content,
    boolean success
) {
  public static ToolResult success(ToolCall call, String content) {
    return new ToolResult(call.id(), call.name(), content, true);
  }

  public static ToolResult failure(ToolCall call, String content) {
    return new ToolResult(call.id(), call.name(), content, false);
  }
}
