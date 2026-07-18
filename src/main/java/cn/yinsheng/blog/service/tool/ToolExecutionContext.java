package cn.yinsheng.blog.service.tool;

import cn.yinsheng.blog.service.model.PageContext;

public record ToolExecutionContext(
    String traceId,
    String sessionId,
    PageContext pageContext
) {
}
