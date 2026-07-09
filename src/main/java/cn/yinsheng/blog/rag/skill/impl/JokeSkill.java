package cn.yinsheng.blog.rag.skill.impl;

import cn.yinsheng.blog.rag.intent.IntentType;
import cn.yinsheng.blog.rag.skill.CapabilityDescriptor;
import cn.yinsheng.blog.rag.skill.Skill;
import cn.yinsheng.blog.rag.skill.SkillRequest;
import cn.yinsheng.blog.rag.skill.SkillResult;
import cn.yinsheng.blog.rag.skill.SkillRiskLevel;
import cn.yinsheng.blog.rag.skill.SkillStatus;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class JokeSkill implements Skill {
  @Override
  public String id() {
    return "joke";
  }

  @Override
  public CapabilityDescriptor descriptor() {
    return new CapabilityDescriptor(
        id(),
        "程序员笑话",
        "互动能力",
        "讲一个短小的技术笑话。",
        SkillRiskLevel.SAFE_LLM,
        SkillStatus.ENABLED,
        List.of(IntentType.JOKE)
    );
  }

  @Override
  public SkillResult execute(SkillRequest request) {
    return SkillResult.answer("程序员最怕什么？不是 Bug，是修好了 Bug 以后不知道为什么好了。");
  }
}
