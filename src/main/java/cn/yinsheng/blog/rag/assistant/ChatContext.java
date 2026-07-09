package cn.yinsheng.blog.rag.assistant;

import cn.yinsheng.blog.rag.intent.IntentResult;
import cn.yinsheng.blog.rag.skill.SkillContext;

public record ChatContext(
    String question,
    String traceId,
    IntentResult intent,
    SkillContext skillContext
) {
}
