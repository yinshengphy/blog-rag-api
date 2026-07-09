package cn.yinsheng.blog.rag.skill.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.yinsheng.blog.rag.config.AssistantProperties;
import cn.yinsheng.blog.rag.intent.IntentResult;
import cn.yinsheng.blog.rag.intent.IntentType;
import cn.yinsheng.blog.rag.skill.CapabilityDescriptor;
import cn.yinsheng.blog.rag.skill.Skill;
import cn.yinsheng.blog.rag.skill.SkillContext;
import cn.yinsheng.blog.rag.skill.SkillRegistry;
import cn.yinsheng.blog.rag.skill.SkillRequest;
import cn.yinsheng.blog.rag.skill.SkillResult;
import cn.yinsheng.blog.rag.skill.SkillRiskLevel;
import cn.yinsheng.blog.rag.skill.SkillStatus;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class CapabilitySkillTest {
  @Test
  void shouldGenerateOutputFromRegistryAndReflectDisabledSkills() {
    AssistantProperties properties = new AssistantProperties();
    AssistantProperties.SkillSettings disabled = new AssistantProperties.SkillSettings();
    disabled.setEnabled(false);
    properties.setSkills(Map.of("weather", disabled));
    Skill publicSkill = fixedSkill("calculator", "安全计算器", "通用工具");
    Skill disabledSkill = fixedSkill("weather", "天气查询", "通用工具");
    SkillRegistry registry = new SkillRegistry(List.of(publicSkill, disabledSkill), properties);
    ObjectProvider<SkillRegistry> provider = mock(ObjectProvider.class);
    when(provider.getObject()).thenReturn(registry);
    CapabilitySkill skill = new CapabilitySkill(provider);

    String answer = skill.execute(new SkillRequest(
        "你有哪些能力？",
        IntentResult.of(IntentType.CAPABILITY_QUERY, 1, "test"),
        SkillContext.publicUser(ZoneId.of("Asia/Shanghai"), "trace-test")
    )).answer();

    assertThat(answer).contains("安全计算器");
    assertThat(answer).doesNotContain("天气查询");
  }

  private Skill fixedSkill(String id, String name, String category) {
    return new Skill() {
      @Override
      public String id() {
        return id;
      }

      @Override
      public CapabilityDescriptor descriptor() {
        return new CapabilityDescriptor(
            id,
            name,
            category,
            "测试能力",
            SkillRiskLevel.READ_ONLY_LOCAL,
            SkillStatus.ENABLED,
            List.of(IntentType.CAPABILITY_QUERY)
        );
      }

      @Override
      public SkillResult execute(SkillRequest request) {
        return SkillResult.answer(name);
      }
    };
  }
}
