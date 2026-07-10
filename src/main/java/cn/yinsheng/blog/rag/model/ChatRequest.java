package cn.yinsheng.blog.rag.model;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ChatRequest(
    @NotBlank(message = "question 不能为空") String question,
    String sessionId,
    PageContext pageContext,
    List<ImageAttachment> images
) {
  public ChatRequest {
    images = images == null ? List.of() : List.copyOf(images);
  }
}
