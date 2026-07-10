package cn.yinsheng.blog.rag.tool;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ToolRegistry {
  private final Map<String, ToolHandler> handlers = new LinkedHashMap<>();

  public ToolRegistry(List<ToolHandler> handlers) {
    handlers.forEach(this::register);
  }

  public void register(ToolHandler handler) {
    handlers.put(handler.definition().name(), handler);
  }

  public Optional<ToolHandler> find(String name) {
    return Optional.ofNullable(handlers.get(name));
  }

  public Collection<ToolDefinition> definitions() {
    return handlers.values().stream().map(ToolHandler::definition).toList();
  }

  public interface ToolHandler {
    ToolDefinition definition();

    ToolResult execute(ToolCall call, ToolExecutionContext context);
  }
}
