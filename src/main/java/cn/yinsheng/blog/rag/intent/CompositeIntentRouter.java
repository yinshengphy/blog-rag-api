package cn.yinsheng.blog.rag.intent;

import cn.yinsheng.blog.rag.config.AssistantProperties;
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
    return llmResult.confidence() >= ruleResult.confidence() ? llmResult : ruleResult;
  }
}
