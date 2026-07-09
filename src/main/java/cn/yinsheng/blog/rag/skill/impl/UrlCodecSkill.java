package cn.yinsheng.blog.rag.skill.impl;

import cn.yinsheng.blog.rag.intent.IntentType;
import cn.yinsheng.blog.rag.skill.CapabilityDescriptor;
import cn.yinsheng.blog.rag.skill.Skill;
import cn.yinsheng.blog.rag.skill.SkillRequest;
import cn.yinsheng.blog.rag.skill.SkillResult;
import cn.yinsheng.blog.rag.skill.SkillRiskLevel;
import cn.yinsheng.blog.rag.skill.SkillStatus;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class UrlCodecSkill implements Skill {
  private static final Pattern ENCODED = Pattern.compile("((?:%[0-9a-fA-F]{2})+)");

  @Override
  public String id() {
    return "url-codec";
  }

  @Override
  public CapabilityDescriptor descriptor() {
    return new CapabilityDescriptor(
        id(),
        "URL 编解码",
        "开发工具",
        "支持 UTF-8 URL encode 和 decode。",
        SkillRiskLevel.READ_ONLY_LOCAL,
        SkillStatus.ENABLED,
        List.of(IntentType.URL_CODEC)
    );
  }

  @Override
  public SkillResult execute(SkillRequest request) {
    String question = request.question();
    Matcher matcher = ENCODED.matcher(question);
    if (matcher.find()) {
      try {
        return SkillResult.answer("URL 解码结果：" + URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8));
      } catch (Exception ex) {
        return SkillResult.answer("这段 URL 编码无法按 UTF-8 正常解码。");
      }
    }
    String text = question.replaceFirst("(?i)url", "")
        .replace("编码", "")
        .replace("encode", "")
        .trim();
    if (text.isBlank()) {
      return SkillResult.answer("请给我要编码或解码的文本。");
    }
    return SkillResult.answer("URL 编码结果：" + URLEncoder.encode(text, StandardCharsets.UTF_8));
  }
}
