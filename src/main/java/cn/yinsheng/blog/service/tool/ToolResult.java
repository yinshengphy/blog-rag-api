package cn.yinsheng.blog.service.tool;

import cn.yinsheng.blog.service.model.Citation;
import cn.yinsheng.blog.service.model.RelatedPost;
import java.util.List;
import java.util.Map;

public record ToolResult(
    String toolCallId,
    String name,
    String content,
    boolean success,
    List<Citation> citations,
    List<RelatedPost> relatedPosts,
    Map<String, Object> metadata
) {
  public static ToolResult success(ToolCall call, String content) {
    return success(call, content, List.of(), List.of(), Map.of());
  }

  public static ToolResult success(
      ToolCall call,
      String content,
      List<Citation> citations,
      List<RelatedPost> relatedPosts,
      Map<String, Object> metadata
  ) {
    return new ToolResult(call.id(), call.name(), content, true, citations, relatedPosts, metadata);
  }

  public static ToolResult failure(ToolCall call, String content) {
    return new ToolResult(call.id(), call.name(), content, false, List.of(), List.of(), Map.of());
  }
}
