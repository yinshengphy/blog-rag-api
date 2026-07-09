package cn.yinsheng.blog.rag.tool;

import java.util.Map;

public record ToolCall(
    String id,
    String name,
    Map<String, Object> arguments
) {
}
