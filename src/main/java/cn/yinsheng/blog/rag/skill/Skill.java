package cn.yinsheng.blog.rag.skill;

import cn.yinsheng.blog.rag.intent.IntentType;

public interface Skill {
  String id();

  CapabilityDescriptor descriptor();

  default boolean supports(IntentType intentType) {
    return descriptor().intents().contains(intentType);
  }

  SkillResult execute(SkillRequest request);
}
