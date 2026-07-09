package cn.yinsheng.blog.rag.skill.impl;

import static org.assertj.core.api.Assertions.assertThat;

import cn.yinsheng.blog.rag.intent.IntentResult;
import cn.yinsheng.blog.rag.intent.IntentType;
import cn.yinsheng.blog.rag.skill.SkillContext;
import cn.yinsheng.blog.rag.skill.SkillRequest;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class CalculatorSkillTest {
  private final CalculatorSkill skill = new CalculatorSkill();

  @Test
  void shouldEvaluateSafeExpression() {
    String answer = skill.execute(request("123 * 456 等于多少？")).answer();

    assertThat(answer).contains("56088");
  }

  @Test
  void shouldRejectUnsafeExpression() {
    String answer = skill.execute(request("1 + Runtime.getRuntime().exec('rm -rf /')")).answer();

    assertThat(answer).contains("没法安全计算");
  }

  private SkillRequest request(String question) {
    return new SkillRequest(
        question,
        IntentResult.of(IntentType.CALCULATOR, 1, "test"),
        SkillContext.publicUser(ZoneId.of("Asia/Shanghai"), "trace-test")
    );
  }
}
