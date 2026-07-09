package cn.yinsheng.blog.rag.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.yinsheng.blog.rag.compute.AiComputeClient;
import cn.yinsheng.blog.rag.config.AssistantProperties;
import cn.yinsheng.blog.rag.intent.IntentResult;
import cn.yinsheng.blog.rag.intent.IntentRouter;
import cn.yinsheng.blog.rag.intent.IntentType;
import cn.yinsheng.blog.rag.model.ChatResponse;
import cn.yinsheng.blog.rag.rag.BlogRagService;
import cn.yinsheng.blog.rag.skill.SkillExecutor;
import cn.yinsheng.blog.rag.skill.SkillRegistry;
import cn.yinsheng.blog.rag.skill.impl.CalculatorSkill;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatOrchestratorTest {
  @Test
  void shouldRouteNonBlogSkillWithoutCallingBlogRag() {
    IntentRouter intentRouter = mock(IntentRouter.class);
    when(intentRouter.route("123 * 456 等于多少？"))
        .thenReturn(IntentResult.of(IntentType.CALCULATOR, 0.96, "test"));
    AssistantProperties properties = new AssistantProperties();
    SkillRegistry registry = new SkillRegistry(List.of(new CalculatorSkill()), properties);
    BlogRagService blogRagService = mock(BlogRagService.class);
    ChatOrchestrator orchestrator = new ChatOrchestrator(
        intentRouter,
        new SkillExecutor(registry),
        blogRagService,
        mock(AiComputeClient.class),
        properties,
        new AnswerComposer(),
        new AssistantSessionMemory()
    );

    ChatResponse response = orchestrator.answer("123 * 456 等于多少？");

    assertThat(response.intent()).isEqualTo("CALCULATOR");
    assertThat(response.usedSkills()).containsExactly("calculator");
    assertThat(response.answer()).contains("56088");
    verify(blogRagService, never()).answer(org.mockito.ArgumentMatchers.anyString());
  }
}
