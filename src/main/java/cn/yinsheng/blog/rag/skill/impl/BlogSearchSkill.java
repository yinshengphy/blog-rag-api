package cn.yinsheng.blog.rag.skill.impl;

import cn.yinsheng.blog.rag.intent.IntentType;
import cn.yinsheng.blog.rag.model.ChatResponse;
import cn.yinsheng.blog.rag.skill.CapabilityDescriptor;
import cn.yinsheng.blog.rag.skill.Skill;
import cn.yinsheng.blog.rag.skill.SkillRequest;
import cn.yinsheng.blog.rag.skill.SkillResult;
import cn.yinsheng.blog.rag.skill.SkillRiskLevel;
import cn.yinsheng.blog.rag.skill.SkillStatus;
import cn.yinsheng.blog.rag.rag.BlogRagService;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BlogSearchSkill implements Skill {
  private final BlogRagService blogRagService;

  public BlogSearchSkill(BlogRagService blogRagService) {
    this.blogRagService = blogRagService;
  }

  @Override
  public String id() {
    return "blog-search";
  }

  @Override
  public CapabilityDescriptor descriptor() {
    return new CapabilityDescriptor(
        id(),
        "博客问答",
        "博客相关",
        "检索博客内容并基于文章片段回答，命中时提供引用和相关文章。",
        SkillRiskLevel.READ_ONLY_LOCAL,
        SkillStatus.ENABLED,
        List.of(IntentType.BLOG_QA, IntentType.BLOG_SEARCH, IntentType.BLOG_NAVIGATION)
    );
  }

  @Override
  public SkillResult execute(SkillRequest request) {
    ChatResponse response = blogRagService.answer(request.question());
    return new SkillResult(response.answer(), response.citations(), response.relatedPosts(), List.of(), response.metadata());
  }
}
