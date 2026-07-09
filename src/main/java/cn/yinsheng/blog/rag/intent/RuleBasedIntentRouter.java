package cn.yinsheng.blog.rag.intent;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedIntentRouter implements IntentRouter {
  private static final Pattern URL_ENCODED = Pattern.compile(".*%[0-9a-fA-F]{2}.*");
  private static final Pattern CALCULATION = Pattern.compile("^[\\s\\d.()+\\-*/%]+[=？?\\s]*(等于多少|是多少|结果)?[？?\\s]*$");
  private static final Pattern BASE64_HINT = Pattern.compile("(?i).*(base64|[A-Za-z0-9+/]{12,}={0,2}).*");

  @Override
  public IntentResult route(String question) {
    String raw = question == null ? "" : question.trim();
    String normalized = raw.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    if (raw.isBlank()) {
      return IntentResult.of(IntentType.CLARIFICATION, 0.99, "空问题需要澄清");
    }
    if (containsAny(normalized, "rm-rf/", "rm-rf", "删除服务器", "执行服务器命令", "执行命令", "泄露密钥", "绕过权限")) {
      return IntentResult.of(IntentType.UNSAFE_OR_FORBIDDEN, 0.98, "疑似危险命令或越权请求");
    }
    if (containsAny(normalized, "你有哪些能力", "你能做什么", "你会什么", "怎么用你", "帮助", "功能列表", "能力")) {
      return IntentResult.of(IntentType.CAPABILITY_QUERY, 0.96, "能力查询");
    }
    if (containsAny(normalized, "你是谁", "你叫什么", "你叫啥", "介绍下你", "自我介绍", "你是什么")) {
      return IntentResult.of(IntentType.SELF_INTRO, 0.94, "自我介绍");
    }
    if (normalized.length() <= 12 && containsAny(normalized, "你好", "hi", "hello", "在吗", "哈喽")) {
      return IntentResult.of(IntentType.GREETING, 0.94, "问候");
    }
    if (URL_ENCODED.matcher(raw).matches()) {
      return IntentResult.of(IntentType.URL_CODEC, 0.95, "包含 URL 编码片段");
    }
    if (containsAny(normalized, "base64", "解base64", "base64解码", "base64编码") && BASE64_HINT.matcher(raw).matches()) {
      return IntentResult.of(IntentType.BASE64_CODEC, 0.9, "Base64 编解码");
    }
    if (CALCULATION.matcher(raw.replace("×", "*").replace("÷", "/")).matches()
        && raw.matches(".*\\d.*")
        && raw.matches(".*[+\\-*/%×÷].*")) {
      return IntentResult.of(IntentType.CALCULATOR, 0.96, "简单算术表达式");
    }
    if (containsAny(normalized, "天气", "下雨", "气温", "温度", "降雨", "风力")) {
      return IntentResult.of(IntentType.WEATHER_QUERY, 0.9, "天气查询");
    }
    if (containsAny(normalized, "笑话", "段子", "讲个梗")) {
      return IntentResult.of(IntentType.JOKE, 0.92, "笑话请求");
    }
    if (containsAny(normalized, "现在几点", "今天几号", "当前时间", "北京时间", "日期")) {
      return IntentResult.of(IntentType.DATETIME_QUERY, 0.9, "日期时间查询");
    }
    if (containsAny(normalized, "时间戳", "timestamp")) {
      return IntentResult.of(IntentType.TIMESTAMP_CONVERT, 0.9, "时间戳转换");
    }
    if (containsAny(normalized, "换算", "转换成", "公里", "千米", "米", "公斤", "千克", "摄氏", "华氏")) {
      return IntentResult.of(IntentType.UNIT_CONVERT, 0.82, "单位换算");
    }
    if (containsAny(normalized, "翻译", "translate")) {
      return IntentResult.of(IntentType.TEXT_TRANSLATE, 0.78, "文本翻译");
    }
    if (containsAny(normalized, "润色", "改写", "polish")) {
      return IntentResult.of(IntentType.TEXT_POLISH, 0.78, "文本润色");
    }
    if (containsAny(normalized, "总结这段", "摘要", "概括")) {
      return IntentResult.of(IntentType.TEXT_SUMMARY, 0.78, "文本总结");
    }
    if (containsAny(normalized, "你博客里", "博客里", "文章里", "有没有讲", "站内", "这篇文章", "相关文章")) {
      IntentType type = containsAny(normalized, "总结", "概括") ? IntentType.BLOG_SUMMARY : IntentType.BLOG_SEARCH;
      return new IntentResult(type, 0.86, "博客内容查询", Map.of());
    }
    if (containsAny(
        normalized,
        "java", "spring", "kubernetes", "k8s", "docker", "rag", "llm", "linux", "数据库", "架构",
        "rsa", "加密", "公钥", "私钥", "算法", "公式"
    )) {
      return IntentResult.of(IntentType.GENERAL_TECH_QA, 0.78, "通用技术问题");
    }
    if (containsAny(normalized, "谢谢", "哈哈", "不错", "再见")) {
      return IntentResult.of(IntentType.SMALL_TALK, 0.78, "轻量闲聊");
    }
    return IntentResult.of(IntentType.UNKNOWN, 0.2, "规则未命中");
  }

  private boolean containsAny(String value, String... needles) {
    for (String needle : needles) {
      if (value.contains(needle)) {
        return true;
      }
    }
    return false;
  }
}
