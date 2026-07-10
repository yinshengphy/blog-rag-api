package cn.yinsheng.blog.rag.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.yinsheng.blog.rag.compute.AiComputeClient;
import cn.yinsheng.blog.rag.model.ChatRequest;
import cn.yinsheng.blog.rag.model.PageContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModelRoutePlannerTest {
  @Test
  void shouldUseModelJsonForCurrentBlogQuestion() {
    AiComputeClient ai = mock(AiComputeClient.class);
    when(ai.classify(anyString(), anyString())).thenReturn("""
        {"route":"BLOG_QA","query":"混合加密在哪里","scope":"CURRENT_POST"}
        """);
    ModelRoutePlanner planner = new ModelRoutePlanner(ai, new ObjectMapper());

    var plan = planner.plan(new ChatRequest(
        "这篇文章的混合加密在哪里",
        "s1",
        new PageContext("BLOG_POST", "rsa", "RSA", "/rsa/", ""),
        List.of()
    ));

    assertThat(plan.route()).isEqualTo(ModelRoutePlanner.Route.BLOG_QA);
    assertThat(plan.arguments()).containsEntry("scope", "CURRENT_POST");
  }

  @Test
  void shouldRecoverSummaryTargetFromQuestionWhenModelOmitsIt() {
    AiComputeClient ai = mock(AiComputeClient.class);
    when(ai.classify(anyString(), anyString())).thenReturn("""
        {"route":"BLOG_SUMMARY"}
        """);
    ModelRoutePlanner planner = new ModelRoutePlanner(ai, new ObjectMapper());

    var plan = planner.plan(
        new ChatRequest("完整总结 RSA 那篇博客", "s1", new PageContext("HOME", "", "", "/", ""), List.of()),
        List.of(Map.of("role", "user", "content", "上一轮问题"))
    );

    assertThat(plan.route()).isEqualTo(ModelRoutePlanner.Route.BLOG_SUMMARY);
    assertThat(plan.arguments()).containsEntry("target", "完整总结 RSA 那篇博客");
  }
}
