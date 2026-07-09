package cn.yinsheng.blog.rag.rag;

import cn.yinsheng.blog.rag.model.RetrievedChunk;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class BlogReranker {
  public List<RetrievedChunk> rerank(String question, List<RetrievedChunk> chunks) {
    String normalizedQuestion = question.toLowerCase(Locale.ROOT);
    return chunks.stream()
        .sorted(Comparator.comparingDouble((RetrievedChunk chunk) -> adjustedScore(normalizedQuestion, chunk)).reversed())
        .toList();
  }

  private double adjustedScore(String normalizedQuestion, RetrievedChunk chunk) {
    double score = chunk.score();
    String title = chunk.title().toLowerCase(Locale.ROOT);
    String section = chunk.section().toLowerCase(Locale.ROOT);
    if (!title.isBlank() && normalizedQuestion.contains(title)) {
      score += 0.08;
    }
    if (!section.isBlank() && normalizedQuestion.contains(section)) {
      score += 0.06;
    }
    for (String tag : chunk.tags()) {
      if (!tag.isBlank() && normalizedQuestion.contains(tag.toLowerCase(Locale.ROOT))) {
        score += 0.04;
      }
    }
    try {
      if (!chunk.updatedAt().isBlank()) {
        long ageDays = Math.max(0, (Instant.now().toEpochMilli() - Instant.parse(chunk.updatedAt()).toEpochMilli()) / 86_400_000L);
        score += Math.max(0, 0.02 - Math.min(0.02, ageDays / 3650.0));
      }
    } catch (Exception ignored) {
      // 更新时间只作为弱排序信号，异常时忽略。
    }
    return score;
  }
}
