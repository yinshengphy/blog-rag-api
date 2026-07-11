package cn.yinsheng.blog.rag.rag;

import cn.yinsheng.blog.rag.model.RetrievedChunk;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class BlogReranker {
  private static final Pattern TERM_PATTERN = Pattern.compile("[a-zA-Z][a-zA-Z0-9+.#_-]*|[\\p{IsHan}]{2,}|\\d+");

  public List<RetrievedChunk> rerank(String question, List<RetrievedChunk> chunks) {
    return chunks.stream()
        .map(chunk -> withScore(chunk, adjustedScore(question, chunk)))
        .sorted(Comparator.comparingDouble(RetrievedChunk::score).reversed())
        .toList();
  }

  public double lexicalScore(String question, RetrievedChunk chunk) {
    String normalizedQuestion = normalize(question);
    String title = normalize(chunk.title());
    String section = normalize(chunk.section());
    String content = normalize(chunk.content());
    double score = 0;
    if (!title.isBlank() && (normalizedQuestion.contains(title) || title.contains(normalizedQuestion))) score += 0.30;
    if (!section.isBlank() && (normalizedQuestion.contains(section) || section.contains(normalizedQuestion))) score += 0.40;
    if (!normalizedQuestion.isBlank() && content.contains(normalizedQuestion)) score += 0.20;
    for (String term : terms(normalizedQuestion)) {
      if (title.contains(term)) score += 0.07;
      if (section.contains(term)) score += 0.08;
      if (content.contains(term)) score += 0.018;
    }
    for (String tag : chunk.tags()) {
      String normalizedTag = normalize(tag);
      if (!normalizedTag.isBlank() && normalizedQuestion.contains(normalizedTag)) score += 0.06;
    }
    return Math.min(0.75, score);
  }

  private double adjustedScore(String question, RetrievedChunk chunk) {
    double score = chunk.score() + lexicalScore(question, chunk);
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

  private Set<String> terms(String value) {
    Set<String> values = new LinkedHashSet<>();
    Matcher matcher = TERM_PATTERN.matcher(value);
    while (matcher.find() && values.size() < 32) {
      String term = matcher.group();
      if (isHan(term)) {
        List<Integer> points = term.codePoints().boxed().toList();
        for (int size = 2; size <= Math.min(3, points.size()); size++) {
          for (int i = 0; i + size <= points.size() && values.size() < 32; i++) {
            StringBuilder gram = new StringBuilder();
            for (int j = i; j < i + size; j++) gram.appendCodePoint(points.get(j));
            values.add(gram.toString());
          }
        }
      } else if (term.length() >= 2) {
        values.add(term);
      }
    }
    return values;
  }

  private boolean isHan(String value) {
    return !value.isBlank() && value.codePoints()
        .allMatch(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
  }

  private String normalize(String value) {
    return value == null ? "" : Normalizer.normalize(value, Normalizer.Form.NFKC)
        .toLowerCase(Locale.ROOT)
        .replaceAll("\\s+", " ")
        .trim();
  }

  private RetrievedChunk withScore(RetrievedChunk chunk, double score) {
    return new RetrievedChunk(
        score,
        chunk.chunkId(),
        chunk.chunkIndex(),
        chunk.slug(),
        chunk.title(),
        chunk.section(),
        chunk.url(),
        chunk.content(),
        chunk.tags(),
        chunk.updatedAt()
    );
  }
}
