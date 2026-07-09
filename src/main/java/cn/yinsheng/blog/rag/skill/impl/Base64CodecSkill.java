package cn.yinsheng.blog.rag.skill.impl;

import cn.yinsheng.blog.rag.intent.IntentType;
import cn.yinsheng.blog.rag.skill.CapabilityDescriptor;
import cn.yinsheng.blog.rag.skill.Skill;
import cn.yinsheng.blog.rag.skill.SkillRequest;
import cn.yinsheng.blog.rag.skill.SkillResult;
import cn.yinsheng.blog.rag.skill.SkillRiskLevel;
import cn.yinsheng.blog.rag.skill.SkillStatus;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class Base64CodecSkill implements Skill {
  private static final Pattern BASE64_TEXT = Pattern.compile("([A-Za-z0-9+/]{8,}={0,2})");

  @Override
  public String id() {
    return "base64-codec";
  }

  @Override
  public CapabilityDescriptor descriptor() {
    return new CapabilityDescriptor(
        id(),
        "Base64 编解码",
        "开发工具",
        "支持 UTF-8 Base64 encode 和 decode。",
        SkillRiskLevel.READ_ONLY_LOCAL,
        SkillStatus.ENABLED,
        List.of(IntentType.BASE64_CODEC)
    );
  }

  @Override
  public SkillResult execute(SkillRequest request) {
    String question = request.question();
    if (question.toLowerCase(java.util.Locale.ROOT).contains("编码")) {
      String text = question.replace("Base64", "").replace("base64", "").replace("编码", "").trim();
      if (text.isBlank()) {
        return SkillResult.answer("请给我要 Base64 编码的文本。");
      }
      return SkillResult.answer("Base64 编码结果：" + Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)));
    }
    Matcher matcher = BASE64_TEXT.matcher(question);
    if (!matcher.find()) {
      return SkillResult.answer("请给我要 Base64 解码的文本。");
    }
    try {
      String decoded = new String(Base64.getDecoder().decode(matcher.group(1)), StandardCharsets.UTF_8);
      return SkillResult.answer("Base64 解码结果：" + decoded);
    } catch (IllegalArgumentException ex) {
      return SkillResult.answer("这段 Base64 文本格式不正确，无法解码。");
    }
  }
}
