package cn.yinsheng.blog.rag.skill.impl;

import cn.yinsheng.blog.rag.intent.IntentType;
import cn.yinsheng.blog.rag.skill.CapabilityDescriptor;
import cn.yinsheng.blog.rag.skill.Skill;
import cn.yinsheng.blog.rag.skill.SkillContext;
import cn.yinsheng.blog.rag.skill.SkillRegistry;
import cn.yinsheng.blog.rag.skill.SkillRequest;
import cn.yinsheng.blog.rag.skill.SkillResult;
import cn.yinsheng.blog.rag.skill.SkillRiskLevel;
import cn.yinsheng.blog.rag.skill.SkillStatus;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class CapabilitySkill implements Skill {
  private final ObjectProvider<SkillRegistry> skillRegistryProvider;

  public CapabilitySkill(ObjectProvider<SkillRegistry> skillRegistryProvider) {
    this.skillRegistryProvider = skillRegistryProvider;
  }

  @Override
  public String id() {
    return "capability";
  }

  @Override
  public CapabilityDescriptor descriptor() {
    return new CapabilityDescriptor(
        id(),
        "能力说明",
        "互动能力",
        "说明当前公开可用的站点助手能力。",
        SkillRiskLevel.READ_ONLY_LOCAL,
        SkillStatus.ENABLED,
        List.of(IntentType.CAPABILITY_QUERY)
    );
  }

  @Override
  public SkillResult execute(SkillRequest request) {
    SkillContext context = request.context();
    List<CapabilityDescriptor> capabilities = skillRegistryProvider.getObject().visibleCapabilities(context);
    Map<String, List<CapabilityDescriptor>> grouped = new LinkedHashMap<>();
    for (String category : List.of("博客相关", "通用工具", "开发工具", "文本处理", "互动能力", "管理员能力")) {
      if (!context.admin() && "管理员能力".equals(category)) {
        continue;
      }
      List<CapabilityDescriptor> items = capabilities.stream()
          .filter(capability -> category.equals(capability.category()))
          .toList();
      if (!items.isEmpty()) {
        grouped.put(category, items);
      }
    }

    StringBuilder answer = new StringBuilder("我是这个博客的站点助手，目前公开可用的能力有：\n");
    grouped.forEach((category, items) -> {
      answer.append("\n").append(category).append("：\n");
      for (CapabilityDescriptor item : items) {
        answer.append("- ").append(item.name()).append("：").append(item.description()).append("\n");
      }
    });
    answer.append("\n我会按问题类型选择能力；命中博客内容时，会尽量带上可点击引用。");
    return SkillResult.answer(answer.toString().trim(), Map.of("capabilityCount", capabilities.size()));
  }
}
