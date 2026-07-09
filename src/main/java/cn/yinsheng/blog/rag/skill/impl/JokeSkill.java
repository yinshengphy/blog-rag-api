package cn.yinsheng.blog.rag.skill.impl;

import cn.yinsheng.blog.rag.intent.IntentType;
import cn.yinsheng.blog.rag.skill.CapabilityDescriptor;
import cn.yinsheng.blog.rag.skill.Skill;
import cn.yinsheng.blog.rag.skill.SkillRequest;
import cn.yinsheng.blog.rag.skill.SkillResult;
import cn.yinsheng.blog.rag.skill.SkillRiskLevel;
import cn.yinsheng.blog.rag.skill.SkillStatus;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class JokeSkill implements Skill {
  private static final List<String> JOKES = List.of(
      "程序员最怕什么？不是 Bug，是修好了 Bug 以后不知道为什么好了。",
      "为什么程序员喜欢深色模式？因为亮色模式一开，Bug 都显得更精神了。",
      "产品说这个需求很简单，程序员听完沉默了三秒，开始怀疑“简单”是不是一种量子状态。",
      "有个程序员去买菜，老婆说买一个西瓜，如果有鸡蛋就买十个。结果他买了十个西瓜，因为有鸡蛋。",
      "为什么递归函数很会讲故事？因为它总是先讲自己。",
      "程序员的浪漫：我把你的名字写进常量里，这样谁也别想轻易改掉。",
      "线上出了问题，最可怕的不是报警响了，是它自己好了。",
      "为什么缓存像魔法？因为你永远不知道现在看到的是现实，还是昨天的幻觉。"
  );

  private final AtomicInteger jokeIndex = new AtomicInteger();

  @Override
  public String id() {
    return "joke";
  }

  @Override
  public CapabilityDescriptor descriptor() {
    return new CapabilityDescriptor(
        id(),
        "程序员笑话",
        "互动能力",
        "讲一个短小的技术笑话。",
        SkillRiskLevel.SAFE_LLM,
        SkillStatus.ENABLED,
        List.of(IntentType.JOKE)
    );
  }

  @Override
  public SkillResult execute(SkillRequest request) {
    int index = Math.floorMod(jokeIndex.getAndIncrement(), JOKES.size());
    return SkillResult.answer(JOKES.get(index));
  }
}
