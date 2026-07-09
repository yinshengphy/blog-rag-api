package cn.yinsheng.blog.rag.skill;

import cn.yinsheng.blog.rag.intent.IntentType;
import java.util.List;

public record CapabilityDescriptor(
    String id,
    String name,
    String category,
    String description,
    SkillRiskLevel riskLevel,
    SkillStatus status,
    List<IntentType> intents
) {
}
