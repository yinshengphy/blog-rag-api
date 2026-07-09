package cn.yinsheng.blog.rag.intent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.yinsheng.blog.rag.config.AssistantProperties;
import org.junit.jupiter.api.Test;

class CompositeIntentRouterTest {
  @Test
  void shouldNotAcceptLlmCapabilityIntentWhenQuestionIsNotAboutAssistantCapabilities() {
    RuleBasedIntentRouter ruleBasedIntentRouter = new RuleBasedIntentRouter();
    LlmIntentRouter llmIntentRouter = mock(LlmIntentRouter.class);
    AssistantProperties properties = new AssistantProperties();
    CompositeIntentRouter router = new CompositeIntentRouter(ruleBasedIntentRouter, llmIntentRouter, properties);

    when(llmIntentRouter.route("这个术语是什么意思？"))
        .thenReturn(IntentResult.of(IntentType.CAPABILITY_QUERY, 0.96, "LLM 误判"));

    IntentResult result = router.route("这个术语是什么意思？");

    assertThat(result.type()).isEqualTo(IntentType.UNKNOWN);
  }
}
