package cn.yinsheng.blog.rag.skill;

public enum SkillRiskLevel {
  SAFE_LLM,
  READ_ONLY_LOCAL,
  READ_ONLY_EXTERNAL,
  INTERNAL_READ,
  WRITE,
  ADMIN_ONLY
}
