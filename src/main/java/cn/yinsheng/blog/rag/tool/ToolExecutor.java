package cn.yinsheng.blog.rag.tool;

import org.springframework.stereotype.Component;

@Component
public class ToolExecutor {
  private final ToolRegistry toolRegistry;

  public ToolExecutor(ToolRegistry toolRegistry) {
    this.toolRegistry = toolRegistry;
  }

  public ToolResult execute(ToolCall call) {
    return toolRegistry.find(call.name())
        .map(handler -> {
          try {
            return handler.execute(call);
          } catch (Exception ex) {
            return ToolResult.failure(call, "工具执行失败：" + ex.getMessage());
          }
        })
        .orElseGet(() -> ToolResult.failure(call, "未知工具：" + call.name()));
  }
}
