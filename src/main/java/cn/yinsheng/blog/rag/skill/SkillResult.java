package cn.yinsheng.blog.rag.skill;

import cn.yinsheng.blog.rag.model.Citation;
import cn.yinsheng.blog.rag.model.RelatedPost;
import java.util.List;
import java.util.Map;

public record SkillResult(
    String answer,
    List<Citation> citations,
    List<RelatedPost> relatedPosts,
    List<String> usedTools,
    Map<String, Object> metadata
) {
  public static SkillResult answer(String answer) {
    return new SkillResult(answer, List.of(), List.of(), List.of(), Map.of());
  }

  public static SkillResult answer(String answer, Map<String, Object> metadata) {
    return new SkillResult(answer, List.of(), List.of(), List.of(), metadata);
  }
}
