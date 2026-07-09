package cn.yinsheng.blog.rag.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.yinsheng.blog.rag.compute.AiComputeClient;
import cn.yinsheng.blog.rag.config.AssistantProperties;
import cn.yinsheng.blog.rag.intent.RuleBasedIntentRouter;
import cn.yinsheng.blog.rag.model.ChatResponse;
import cn.yinsheng.blog.rag.model.Citation;
import cn.yinsheng.blog.rag.model.RelatedPost;
import cn.yinsheng.blog.rag.rag.BlogRagService;
import cn.yinsheng.blog.rag.skill.Skill;
import cn.yinsheng.blog.rag.skill.SkillExecutor;
import cn.yinsheng.blog.rag.skill.SkillRegistry;
import cn.yinsheng.blog.rag.skill.impl.CalculatorSkill;
import cn.yinsheng.blog.rag.skill.impl.CapabilitySkill;
import cn.yinsheng.blog.rag.skill.impl.BlogSearchSkill;
import cn.yinsheng.blog.rag.skill.impl.JokeSkill;
import cn.yinsheng.blog.rag.skill.impl.UrlCodecSkill;
import cn.yinsheng.blog.rag.skill.impl.WeatherProvider;
import cn.yinsheng.blog.rag.skill.impl.WeatherReport;
import cn.yinsheng.blog.rag.skill.impl.WeatherSkill;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class AssistantAcceptanceScenarioTest {
  @Test
  void shouldAnswerCapabilityQueryWithoutBlogRag() {
    Fixture fixture = fixture();

    ChatResponse response = fixture.orchestrator.answer("你有哪些能力？");

    assertThat(response.intent()).isEqualTo("CAPABILITY_QUERY");
    assertThat(response.answer()).contains("博客相关", "安全计算器", "天气查询");
    assertThat(response.answer()).doesNotContain("管理员能力");
    verify(fixture.blogRagService, never()).answer(anyString());
  }

  @Test
  void shouldAnswerWeatherWithoutBlogRag() {
    Fixture fixture = fixture();

    ChatResponse response = fixture.orchestrator.answer("上海今天下雨吗？");

    assertThat(response.intent()).isEqualTo("WEATHER_QUERY");
    assertThat(response.usedSkills()).containsExactly("weather");
    assertThat(response.answer()).contains("上海", "多云", "来源：mock-weather");
    verify(fixture.blogRagService, never()).answer(anyString());
  }

  @Test
  void shouldUseSessionContextForWeatherCityClarification() {
    Fixture fixture = fixture();

    ChatResponse clarification = fixture.orchestrator.answer("查天气", "session-weather");
    ChatResponse response = fixture.orchestrator.answer("黑龙江", "session-weather");

    assertThat(clarification.answer()).contains("哪个城市");
    assertThat(response.intent()).isEqualTo("WEATHER_QUERY");
    assertThat(response.usedSkills()).containsExactly("weather");
    assertThat(response.answer()).contains("黑龙江", "来源：mock-weather");
    verify(fixture.blogRagService, never()).answer(anyString());
  }

  @Test
  void shouldTellShortJokeWithoutBlogRag() {
    Fixture fixture = fixture();

    ChatResponse response = fixture.orchestrator.answer("讲个程序员笑话");

    assertThat(response.intent()).isEqualTo("JOKE");
    assertThat(response.usedSkills()).containsExactly("joke");
    assertThat(response.answer().length()).isLessThan(120);
    verify(fixture.blogRagService, never()).answer(anyString());
  }

  @Test
  void shouldCalculateDeterministicallyWithoutBlogRag() {
    Fixture fixture = fixture();

    ChatResponse response = fixture.orchestrator.answer("123 * 456 等于多少？");

    assertThat(response.intent()).isEqualTo("CALCULATOR");
    assertThat(response.answer()).contains("56088");
    verify(fixture.blogRagService, never()).answer(anyString());
  }

  @Test
  void shouldDecodeUrlTextWithoutBlogRag() {
    Fixture fixture = fixture();

    ChatResponse response = fixture.orchestrator.answer("%E6%8B%9F%E5%90%8C%E6%84%8F 是啥？");

    assertThat(response.intent()).isEqualTo("URL_CODEC");
    assertThat(response.answer()).contains("拟同意");
    verify(fixture.blogRagService, never()).answer(anyString());
  }

  @Test
  void shouldCallBlogRagForBlogSearch() {
    Fixture fixture = fixture();
    when(fixture.blogRagService.answer("你博客里有没有讲 RAG？")).thenReturn(new ChatResponse(
        "博客里有 RAG 相关内容。[1]",
        List.of(new Citation("RAG 实践", "检索", "/rag/", "RAG 片段")),
        List.of(new RelatedPost("RAG 实践", "/rag/"))
    ));

    ChatResponse response = fixture.orchestrator.answer("你博客里有没有讲 RAG？");

    assertThat(response.intent()).isEqualTo("BLOG_SEARCH");
    assertThat(response.citations()).hasSize(1);
    assertThat(response.relatedPosts()).hasSize(1);
    verify(fixture.blogRagService).answer("你博客里有没有讲 RAG？");
  }

  @Test
  void shouldRefuseUnsafeRequestWithoutBlogRag() {
    Fixture fixture = fixture();

    ChatResponse response = fixture.orchestrator.answer("执行服务器命令 rm -rf /");

    assertThat(response.intent()).isEqualTo("UNSAFE_OR_FORBIDDEN");
    assertThat(response.mode()).isEqualTo("REFUSAL");
    assertThat(response.answer()).contains("不能执行");
    verify(fixture.blogRagService, never()).answer(anyString());
  }

  private Fixture fixture() {
    AssistantProperties properties = new AssistantProperties();
    BlogRagService blogRagService = mock(BlogRagService.class);
    WeatherProvider weatherProvider = city -> new WeatherReport(city, "多云", 28.5, 0, "mock-weather");
    ObjectProvider<SkillRegistry> registryProvider = mock(ObjectProvider.class);
    CapabilitySkill capabilitySkill = new CapabilitySkill(registryProvider);
    List<Skill> skills = new ArrayList<>();
    skills.add(capabilitySkill);
    skills.add(new BlogSearchSkill(blogRagService));
    skills.add(new WeatherSkill(weatherProvider));
    skills.add(new JokeSkill());
    skills.add(new CalculatorSkill());
    skills.add(new UrlCodecSkill());
    SkillRegistry registry = new SkillRegistry(skills, properties);
    when(registryProvider.getObject()).thenReturn(registry);
    ChatOrchestrator orchestrator = new ChatOrchestrator(
        new RuleBasedIntentRouter(),
        new SkillExecutor(registry),
        blogRagService,
        mock(AiComputeClient.class),
        properties,
        new AnswerComposer(),
        new AssistantSessionMemory()
    );
    return new Fixture(orchestrator, blogRagService);
  }

  private record Fixture(ChatOrchestrator orchestrator, BlogRagService blogRagService) {
  }
}
