package cn.yinsheng.blog.rag.skill;

import cn.yinsheng.blog.rag.intent.IntentResult;

public record SkillRequest(
    String question,
    IntentResult intent,
    SkillContext context
) {
}
