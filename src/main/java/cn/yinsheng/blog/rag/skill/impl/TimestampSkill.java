package cn.yinsheng.blog.rag.skill.impl;

import cn.yinsheng.blog.rag.intent.IntentType;
import cn.yinsheng.blog.rag.skill.CapabilityDescriptor;
import cn.yinsheng.blog.rag.skill.Skill;
import cn.yinsheng.blog.rag.skill.SkillRequest;
import cn.yinsheng.blog.rag.skill.SkillResult;
import cn.yinsheng.blog.rag.skill.SkillRiskLevel;
import cn.yinsheng.blog.rag.skill.SkillStatus;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class TimestampSkill implements Skill {
  private static final Pattern NUMBER = Pattern.compile("\\b(\\d{10}|\\d{13})\\b");

  @Override
  public String id() {
    return "timestamp";
  }

  @Override
  public CapabilityDescriptor descriptor() {
    return new CapabilityDescriptor(
        id(),
        "时间戳转换",
        "开发工具",
        "支持秒级和毫秒级时间戳转本地时间。",
        SkillRiskLevel.READ_ONLY_LOCAL,
        SkillStatus.ENABLED,
        List.of(IntentType.TIMESTAMP_CONVERT)
    );
  }

  @Override
  public SkillResult execute(SkillRequest request) {
    Matcher matcher = NUMBER.matcher(request.question());
    if (!matcher.find()) {
      long seconds = Instant.now().getEpochSecond();
      long millis = Instant.now().toEpochMilli();
      return SkillResult.answer("当前时间戳：秒级 " + seconds + "，毫秒级 " + millis + "。");
    }
    String value = matcher.group(1);
    long epochMillis = value.length() == 13 ? Long.parseLong(value) : Long.parseLong(value) * 1000;
    ZonedDateTime dateTime = Instant.ofEpochMilli(epochMillis).atZone(request.context().zoneId());
    return SkillResult.answer(value + " 对应本地时间：" + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").format(dateTime));
  }
}
