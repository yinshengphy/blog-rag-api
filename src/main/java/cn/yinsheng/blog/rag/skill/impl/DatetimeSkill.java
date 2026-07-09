package cn.yinsheng.blog.rag.skill.impl;

import cn.yinsheng.blog.rag.intent.IntentType;
import cn.yinsheng.blog.rag.skill.CapabilityDescriptor;
import cn.yinsheng.blog.rag.skill.Skill;
import cn.yinsheng.blog.rag.skill.SkillRequest;
import cn.yinsheng.blog.rag.skill.SkillResult;
import cn.yinsheng.blog.rag.skill.SkillRiskLevel;
import cn.yinsheng.blog.rag.skill.SkillStatus;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DatetimeSkill implements Skill {
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z", Locale.CHINA);

  @Override
  public String id() {
    return "datetime";
  }

  @Override
  public CapabilityDescriptor descriptor() {
    return new CapabilityDescriptor(
        id(),
        "日期时间",
        "通用工具",
        "查询当前本地日期和时间。",
        SkillRiskLevel.READ_ONLY_LOCAL,
        SkillStatus.ENABLED,
        List.of(IntentType.DATETIME_QUERY)
    );
  }

  @Override
  public SkillResult execute(SkillRequest request) {
    ZonedDateTime now = ZonedDateTime.now(request.context().zoneId());
    return SkillResult.answer("当前本地时间是：" + FORMATTER.format(now), Map.of("zoneId", request.context().zoneId().toString()));
  }
}
