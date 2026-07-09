package cn.yinsheng.blog.rag.skill;

import cn.yinsheng.blog.rag.intent.IntentResult;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SkillExecutor {
  private final SkillRegistry skillRegistry;

  public SkillExecutor(SkillRegistry skillRegistry) {
    this.skillRegistry = skillRegistry;
  }

  public Optional<ExecutedSkill> execute(String question, IntentResult intent, SkillContext context) {
    Optional<Skill> skill = skillRegistry.findVisible(intent.type(), context);
    if (skill.isEmpty()) {
      return Optional.empty();
    }
    SkillResult result = skill.get().execute(new SkillRequest(question, intent, context));
    return Optional.of(new ExecutedSkill(skill.get().id(), result));
  }

  public record ExecutedSkill(String skillId, SkillResult result) {
  }
}
