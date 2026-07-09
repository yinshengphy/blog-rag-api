package cn.yinsheng.blog.rag.skill;

import static org.assertj.core.api.Assertions.assertThat;

import cn.yinsheng.blog.rag.config.AssistantProperties;
import cn.yinsheng.blog.rag.intent.IntentType;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SkillRegistryTest {
  @Test
  void shouldHideAdminOnlySkillsForPublicUsers() {
    SkillRegistry registry = new SkillRegistry(
        List.of(skill("public", SkillRiskLevel.READ_ONLY_LOCAL), skill("admin", SkillRiskLevel.ADMIN_ONLY)),
        new AssistantProperties()
    );

    List<String> ids = registry.visibleCapabilities(publicContext()).stream()
        .map(CapabilityDescriptor::id)
        .toList();

    assertThat(ids).contains("public");
    assertThat(ids).doesNotContain("admin");
  }

  @Test
  void shouldHideDisabledSkills() {
    AssistantProperties properties = new AssistantProperties();
    AssistantProperties.SkillSettings settings = new AssistantProperties.SkillSettings();
    settings.setEnabled(false);
    properties.setSkills(Map.of("disabled", settings));
    SkillRegistry registry = new SkillRegistry(List.of(skill("disabled", SkillRiskLevel.READ_ONLY_LOCAL)), properties);

    assertThat(registry.visibleCapabilities(publicContext())).isEmpty();
  }

  private SkillContext publicContext() {
    return SkillContext.publicUser(ZoneId.of("Asia/Shanghai"), "trace-test");
  }

  private Skill skill(String id, SkillRiskLevel riskLevel) {
    return new Skill() {
      @Override
      public String id() {
        return id;
      }

      @Override
      public CapabilityDescriptor descriptor() {
        return new CapabilityDescriptor(
            id,
            id,
            "通用工具",
            id + " description",
            riskLevel,
            SkillStatus.ENABLED,
            List.of(IntentType.CAPABILITY_QUERY)
        );
      }

      @Override
      public SkillResult execute(SkillRequest request) {
        return SkillResult.answer(id);
      }
    };
  }
}
