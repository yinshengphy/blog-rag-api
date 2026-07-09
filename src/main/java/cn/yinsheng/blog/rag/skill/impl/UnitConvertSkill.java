package cn.yinsheng.blog.rag.skill.impl;

import cn.yinsheng.blog.rag.intent.IntentType;
import cn.yinsheng.blog.rag.skill.CapabilityDescriptor;
import cn.yinsheng.blog.rag.skill.Skill;
import cn.yinsheng.blog.rag.skill.SkillRequest;
import cn.yinsheng.blog.rag.skill.SkillResult;
import cn.yinsheng.blog.rag.skill.SkillRiskLevel;
import cn.yinsheng.blog.rag.skill.SkillStatus;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class UnitConvertSkill implements Skill {
  private static final Pattern NUMBER = Pattern.compile("(-?\\d+(?:\\.\\d+)?)");

  @Override
  public String id() {
    return "unit-convert";
  }

  @Override
  public CapabilityDescriptor descriptor() {
    return new CapabilityDescriptor(
        id(),
        "单位换算",
        "通用工具",
        "支持常见长度、重量和温度换算。",
        SkillRiskLevel.READ_ONLY_LOCAL,
        SkillStatus.ENABLED,
        List.of(IntentType.UNIT_CONVERT)
    );
  }

  @Override
  public SkillResult execute(SkillRequest request) {
    String question = request.question();
    Matcher matcher = NUMBER.matcher(question);
    if (!matcher.find()) {
      return SkillResult.answer("请告诉我要换算的数字，例如：10 公里换算成米。");
    }
    double value = Double.parseDouble(matcher.group(1));
    if (question.contains("公里") || question.contains("千米")) {
      return SkillResult.answer(format(value) + " 公里 = " + format(value * 1000) + " 米");
    }
    if (question.contains("公斤") || question.contains("千克")) {
      return SkillResult.answer(format(value) + " 千克 = " + format(value * 1000) + " 克");
    }
    if (question.contains("摄氏") || question.contains("℃")) {
      return SkillResult.answer(format(value) + " 摄氏度 = " + format(value * 9 / 5 + 32) + " 华氏度");
    }
    if (question.contains("华氏") || question.contains("℉")) {
      return SkillResult.answer(format(value) + " 华氏度 = " + format((value - 32) * 5 / 9) + " 摄氏度");
    }
    return SkillResult.answer("我暂时只支持公里/米、千克/克、摄氏/华氏这些常见换算。");
  }

  private String format(double value) {
    if (Math.rint(value) == value) {
      return String.valueOf((long) value);
    }
    return String.format(java.util.Locale.ROOT, "%.4f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
  }
}
