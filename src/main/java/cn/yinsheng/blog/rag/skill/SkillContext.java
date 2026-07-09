package cn.yinsheng.blog.rag.skill;

import java.time.ZoneId;

public record SkillContext(
    boolean admin,
    ZoneId zoneId,
    String traceId
) {
  public static SkillContext publicUser(ZoneId zoneId, String traceId) {
    return new SkillContext(false, zoneId, traceId);
  }
}
