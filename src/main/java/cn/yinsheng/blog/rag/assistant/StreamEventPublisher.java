package cn.yinsheng.blog.rag.assistant;

import cn.yinsheng.blog.rag.model.ChatResponse;
import java.util.Map;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class StreamEventPublisher {
  private final SseEmitter emitter;

  public StreamEventPublisher(SseEmitter emitter) {
    this.emitter = emitter;
  }

  public void meta(ChatResponse response) {
    send("meta", Map.of(
        "mode", response.mode() == null ? "" : response.mode(),
        "intent", response.intent() == null ? "" : response.intent(),
        "usedSkills", response.usedSkills(),
        "usedTools", response.usedTools()
    ));
  }

  public void delta(String text) {
    send("delta", Map.of("text", text));
  }

  public void send(String name, Object data) {
    try {
      emitter.send(SseEmitter.event().name(name).data(data));
    } catch (Exception ex) {
      throw new IllegalStateException("发送 SSE 事件失败", ex);
    }
  }
}
