package cn.yinsheng.blog.rag.intent;

import cn.yinsheng.blog.rag.config.AssistantProperties;
import java.util.Locale;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class CompositeIntentRouter implements IntentRouter {
  private final RuleBasedIntentRouter ruleBasedIntentRouter;
  private final LlmIntentRouter llmIntentRouter;
  private final AssistantProperties properties;

  public CompositeIntentRouter(
      RuleBasedIntentRouter ruleBasedIntentRouter,
      LlmIntentRouter llmIntentRouter,
      AssistantProperties properties
  ) {
    this.ruleBasedIntentRouter = ruleBasedIntentRouter;
    this.llmIntentRouter = llmIntentRouter;
    this.properties = properties;
  }

  @Override
  public IntentResult route(String question) {
    IntentResult ruleResult = ruleBasedIntentRouter.route(question);
    if (ruleResult.confidence() >= properties.getIntent().getLowConfidenceThreshold()
        || ruleResult.type() == IntentType.UNSAFE_OR_FORBIDDEN
        || ruleResult.type() == IntentType.CLARIFICATION) {
      return ruleResult;
    }
    IntentResult llmResult = llmIntentRouter.route(question);
    if (llmResult.type() == IntentType.CAPABILITY_QUERY && !looksLikeCapabilityQuery(question)) {
      return ruleResult;
    }
    return llmResult.confidence() >= ruleResult.confidence() ? llmResult : ruleResult;
  }

  private boolean looksLikeCapabilityQuery(String question) {
    String normalized = question == null ? "" : question.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    return containsAny(normalized, "你有哪些能力", "你能做什么", "你会什么", "你有什么功能", "你有什么能力", "功能列表", "怎么用你");
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
