package cn.yinsheng.blog.service.tool;

import org.springframework.stereotype.Component;

@Component
public class ToolExecutor {
  private final ToolRegistry toolRegistry;

  public ToolExecutor(ToolRegistry toolRegistry) {
    this.toolRegistry = toolRegistry;
  }

  public ToolResult execute(ToolCall call, ToolExecutionContext context) {
    return toolRegistry.find(call.name())
        .map(handler -> {
          try {
            return handler.execute(call, context);
          } catch (Exception ex) {
            return ToolResult.failure(call, "工具执行失败：" + ex.getMessage());
          }
        })
        .orElseGet(() -> ToolResult.failure(call, "未知工具：" + call.name()));
  }

  public ToolResult execute(ToolCall call) {
    return execute(call, new ToolExecutionContext("", "", null));
  }
}
