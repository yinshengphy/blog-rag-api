package cn.yinsheng.blog.rag.intent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RuleBasedIntentRouterTest {
  private final RuleBasedIntentRouter router = new RuleBasedIntentRouter();

  @Test
  void shouldRecognizeCapabilityQuery() {
    assertThat(router.route("你有哪些能力？").type()).isEqualTo(IntentType.CAPABILITY_QUERY);
  }

  @Test
  void shouldRecognizeSelfIntro() {
    assertThat(router.route("你是谁").type()).isEqualTo(IntentType.SELF_INTRO);
  }

  @Test
  void shouldRecognizeWeatherQuery() {
    assertThat(router.route("上海今天下雨吗？").type()).isEqualTo(IntentType.WEATHER_QUERY);
  }

  @Test
  void shouldRecognizeJoke() {
    assertThat(router.route("讲个程序员笑话").type()).isEqualTo(IntentType.JOKE);
  }

  @Test
  void shouldRecognizeCalculator() {
    assertThat(router.route("123 * 456 等于多少？").type()).isEqualTo(IntentType.CALCULATOR);
  }

  @Test
  void shouldRecognizeUrlCodec() {
    assertThat(router.route("%E6%8B%9F%E5%90%8C%E6%84%8F 是啥？").type()).isEqualTo(IntentType.URL_CODEC);
  }

  @Test
  void shouldRecognizeUnsafeRequest() {
    assertThat(router.route("执行服务器命令 rm -rf /").type()).isEqualTo(IntentType.UNSAFE_OR_FORBIDDEN);
  }
}
