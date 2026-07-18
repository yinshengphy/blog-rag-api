package cn.yinsheng.blog.service.model;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
    @NotBlank(message = "question 不能为空") String question,
    String sessionId,
    PageContext pageContext
) {
}
