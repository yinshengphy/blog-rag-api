package cn.yinsheng.blog.service.model;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ChatRequest(
    @NotBlank(message = "question 不能为空") String question,
    String sessionId,
    PageContext pageContext,
    List<ConversationMessage> history
) {
  public ChatRequest(String question, String sessionId, PageContext pageContext) {
    this(question, sessionId, pageContext, List.of());
  }
}
