package cn.yinsheng.blog.rag.skill;

import cn.yinsheng.blog.rag.config.AssistantProperties;
import cn.yinsheng.blog.rag.intent.IntentType;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SkillRegistry {
  private final List<Skill> skills;
  private final AssistantProperties properties;

  public SkillRegistry(List<Skill> skills, AssistantProperties properties) {
    this.skills = skills.stream()
        .sorted(Comparator.comparing(Skill::id))
        .toList();
    this.properties = properties;
  }

  public List<Skill> all() {
    return skills;
  }

  public Optional<Skill> findVisible(IntentType intentType, SkillContext context) {
    return skills.stream()
        .filter(skill -> skill.supports(intentType))
        .filter(skill -> isVisible(skill, context))
        .findFirst();
  }

  public List<CapabilityDescriptor> visibleCapabilities(SkillContext context) {
    return skills.stream()
        .filter(skill -> isVisible(skill, context))
        .map(this::effectiveDescriptor)
        .sorted(Comparator.comparing(CapabilityDescriptor::category).thenComparing(CapabilityDescriptor::name))
        .toList();
  }

  public boolean isVisible(Skill skill, SkillContext context) {
    CapabilityDescriptor descriptor = effectiveDescriptor(skill);
    if (descriptor.status() != SkillStatus.ENABLED) {
      return false;
    }
    if (context.admin()) {
      return true;
    }
    return descriptor.riskLevel() == SkillRiskLevel.SAFE_LLM
        || descriptor.riskLevel() == SkillRiskLevel.READ_ONLY_LOCAL
        || descriptor.riskLevel() == SkillRiskLevel.READ_ONLY_EXTERNAL;
  }

  public CapabilityDescriptor effectiveDescriptor(Skill skill) {
    CapabilityDescriptor descriptor = skill.descriptor();
    boolean enabled = properties.isSkillEnabled(descriptor.id(), descriptor.status() == SkillStatus.ENABLED);
    SkillStatus status = enabled ? descriptor.status() : SkillStatus.DISABLED;
    return new CapabilityDescriptor(
        descriptor.id(),
        descriptor.name(),
        descriptor.category(),
        descriptor.description(),
        descriptor.riskLevel(),
        status,
        descriptor.intents()
    );
  }
}
