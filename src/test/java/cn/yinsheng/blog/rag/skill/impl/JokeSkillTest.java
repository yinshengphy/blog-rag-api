package cn.yinsheng.blog.rag.skill.impl;

import static org.assertj.core.api.Assertions.assertThat;

import cn.yinsheng.blog.rag.intent.IntentResult;
import cn.yinsheng.blog.rag.intent.IntentType;
import cn.yinsheng.blog.rag.skill.SkillContext;
import cn.yinsheng.blog.rag.skill.SkillRequest;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class JokeSkillTest {
  @Test
  void shouldRotateJokes() {
    JokeSkill skill = new JokeSkill();

    String first = skill.execute(request()).answer();
    String second = skill.execute(request()).answer();

    assertThat(first).isNotEqualTo(second);
  }

  private SkillRequest request() {
    return new SkillRequest(
        "讲个程序员笑话",
        IntentResult.of(IntentType.JOKE, 1, "test"),
        SkillContext.publicUser(ZoneId.of("Asia/Shanghai"), "trace-test")
    );
  }
}
