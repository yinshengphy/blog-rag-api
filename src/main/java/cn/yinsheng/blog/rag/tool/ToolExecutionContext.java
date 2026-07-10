package cn.yinsheng.blog.rag.tool;

import cn.yinsheng.blog.rag.model.PageContext;

public record ToolExecutionContext(
    String traceId,
    String sessionId,
    PageContext pageContext
) {
}
