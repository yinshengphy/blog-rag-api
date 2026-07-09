package cn.yinsheng.blog.rag.skill.impl;

import cn.yinsheng.blog.rag.intent.IntentType;
import cn.yinsheng.blog.rag.model.ChatResponse;
import cn.yinsheng.blog.rag.rag.BlogRagService;
import cn.yinsheng.blog.rag.skill.CapabilityDescriptor;
import cn.yinsheng.blog.rag.skill.Skill;
import cn.yinsheng.blog.rag.skill.SkillRequest;
import cn.yinsheng.blog.rag.skill.SkillResult;
import cn.yinsheng.blog.rag.skill.SkillRiskLevel;
import cn.yinsheng.blog.rag.skill.SkillStatus;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BlogSummarySkill implements Skill {
  private final BlogRagService blogRagService;

  public BlogSummarySkill(BlogRagService blogRagService) {
    this.blogRagService = blogRagService;
  }

  @Override
  public String id() {
    return "blog-summary";
  }

  @Override
  public CapabilityDescriptor descriptor() {
    return new CapabilityDescriptor(
        id(),
        "博客摘要",
        "博客相关",
        "按博客检索结果总结文章或主题。",
        SkillRiskLevel.READ_ONLY_LOCAL,
        SkillStatus.ENABLED,
        List.of(IntentType.BLOG_SUMMARY)
    );
  }

  @Override
  public SkillResult execute(SkillRequest request) {
    ChatResponse response = blogRagService.answer("请总结博客中与这个问题相关的内容：" + request.question());
    return new SkillResult(response.answer(), response.citations(), response.relatedPosts(), List.of(), response.metadata());
  }
}
