package cn.yinsheng.blog.rag.model;

import java.util.List;
import java.util.Map;

public record ChatResponse(
    String answer,
    List<Citation> citations,
    List<RelatedPost> relatedPosts,
    String mode,
    String intent,
    List<String> usedSkills,
    List<String> usedTools,
    Map<String, Object> metadata
) {
  public ChatResponse(String answer, List<Citation> citations, List<RelatedPost> relatedPosts) {
    this(answer, citations, relatedPosts, null, null, List.of(), List.of(), Map.of());
  }
}
