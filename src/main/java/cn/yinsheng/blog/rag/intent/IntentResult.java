package cn.yinsheng.blog.rag.intent;

import java.util.Map;

public record IntentResult(
    IntentType type,
    double confidence,
    String reason,
    Map<String, Object> slots
) {
  public static IntentResult of(IntentType type, double confidence, String reason) {
    return new IntentResult(type, confidence, reason, Map.of());
  }

  public boolean is(IntentType expected) {
    return type == expected;
  }
}
