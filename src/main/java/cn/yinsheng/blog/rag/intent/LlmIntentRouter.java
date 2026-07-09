package cn.yinsheng.blog.rag.intent;

import cn.yinsheng.blog.rag.compute.AiComputeClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LlmIntentRouter implements IntentRouter {
  private final AiComputeClient aiComputeClient;
  private final ObjectMapper objectMapper;

  public LlmIntentRouter(AiComputeClient aiComputeClient, ObjectMapper objectMapper) {
    this.aiComputeClient = aiComputeClient;
    this.objectMapper = objectMapper;
  }

  @Override
  public IntentResult route(String question) {
    String systemPrompt = """
        你只负责做意图分类，不回答用户问题。
        必须只输出严格 JSON，不要 Markdown，不要解释。
        JSON 字段：intent、confidence、reason。
        intent 只能是以下枚举之一：
        GREETING, SELF_INTRO, CAPABILITY_QUERY, BLOG_QA, BLOG_SEARCH, BLOG_SUMMARY, BLOG_RECOMMEND,
        BLOG_NAVIGATION, WEATHER_QUERY, JOKE, CALCULATOR, UNIT_CONVERT, DATETIME_QUERY,
        TIMESTAMP_CONVERT, URL_CODEC, BASE64_CODEC, TEXT_TRANSLATE, TEXT_POLISH, TEXT_SUMMARY,
        GENERAL_TECH_QA, SMALL_TALK, CLARIFICATION, UNSAFE_OR_FORBIDDEN, UNKNOWN。
        """;
    String userPrompt = "用户问题：\n" + question;
    try {
      String content = aiComputeClient.chat(systemPrompt, userPrompt);
      JsonNode node = objectMapper.readTree(content);
      IntentType type = IntentType.valueOf(node.path("intent").asText("UNKNOWN").toUpperCase(Locale.ROOT));
      double confidence = Math.max(0, Math.min(1, node.path("confidence").asDouble(0.5)));
      String reason = node.path("reason").asText("LLM 分类");
      return new IntentResult(type, confidence, reason, Map.of());
    } catch (Exception ex) {
      return IntentResult.of(IntentType.UNKNOWN, 0.3, "LLM 意图分类失败");
    }
  }
}
