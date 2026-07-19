package cn.yinsheng.blog.service.rag;

import cn.yinsheng.blog.service.model.Citation;
import cn.yinsheng.blog.service.model.RetrievedChunk;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class CitationBuilder {
  private static final Pattern CITATION_MARKER_PATTERN = Pattern.compile("\\[(\\d+)]");
  private static final Pattern LATIN_TERM_PATTERN = Pattern.compile("[a-z0-9]{2,}");
  private static final Pattern HAN_TEXT_PATTERN = Pattern.compile("[\\p{IsHan}]{2,}");

  public List<Citation> build(List<RetrievedChunk> chunks) {
    return build(chunks, "");
  }

  public List<Citation> build(List<RetrievedChunk> chunks, String answer) {
    Map<String, Citation> citations = new LinkedHashMap<>();
    for (int index = 0; index < chunks.size(); index++) {
      RetrievedChunk chunk = chunks.get(index);
      String key = chunk.url() + ">" + chunk.section();
      citations.putIfAbsent(key, new Citation(
          chunk.title(),
          chunk.section(),
          chunk.url(),
          snippet(chunk.content(), referenceText(answer, index + 1))
      ));
    }
    return new ArrayList<>(citations.values());
  }

  private String referenceText(String answer, int index) {
    if (answer == null || answer.isBlank()) {
      return "";
    }
    Matcher matcher = CITATION_MARKER_PATTERN.matcher(answer);
    while (matcher.find()) {
      if (Integer.parseInt(matcher.group(1)) != index) {
        continue;
      }
      int markerStart = matcher.start();
      int start = Math.max(
          answer.lastIndexOf('\n', markerStart - 1),
          Math.max(
              Math.max(answer.lastIndexOf('。', markerStart - 1), answer.lastIndexOf('！', markerStart - 1)),
              Math.max(answer.lastIndexOf('？', markerStart - 1), answer.lastIndexOf('.', markerStart - 1))
          )
      );
      return answer.substring(start + 1, markerStart).trim();
    }
    return "";
  }

  private String snippet(String content, String referenceText) {
    String compact = content.replaceAll("\\s+", " ").trim();
    String focused = focusedSnippet(compact, referenceText);
    String value = focused.isBlank() ? compact : focused;
    return value.length() <= 140 ? value : value.substring(0, 140) + "...";
  }

  private String focusedSnippet(String content, String referenceText) {
    Set<String> terms = referenceTerms(referenceText);
    if (content.isBlank() || terms.isEmpty()) {
      return "";
    }
    return sentenceWindows(content).stream()
        .max(Comparator.comparingInt(window -> score(window, terms)))
        .filter(window -> score(window, terms) > 0)
        .orElse("");
  }

  private List<String> sentenceWindows(String content) {
    String[] sentences = content.split("(?<=[。！？.!?])\\s*");
    List<String> windows = new ArrayList<>();
    for (int i = 0; i < sentences.length; i++) {
      String current = sentences[i].trim();
      if (current.isBlank()) {
        continue;
      }
      String next = i + 1 < sentences.length ? sentences[i + 1].trim() : "";
      windows.add(next.isBlank() ? current : current + " " + next);
    }
    return windows.isEmpty() ? List.of(content) : windows;
  }

  private Set<String> referenceTerms(String value) {
    String normalized = normalize(value);
    Set<String> terms = new HashSet<>();
    Matcher latinMatcher = LATIN_TERM_PATTERN.matcher(normalized);
    while (latinMatcher.find()) {
      terms.add(latinMatcher.group());
    }
    Matcher hanMatcher = HAN_TEXT_PATTERN.matcher(normalized);
    while (hanMatcher.find()) {
      String text = hanMatcher.group();
      for (int i = 0; i < text.length() - 1; i++) {
        terms.add(text.substring(i, i + 2));
      }
    }
    terms.removeAll(Set.of("这个", "一种", "可以", "通过", "实现", "问题", "内容", "文章"));
    return terms;
  }

  private int score(String content, Set<String> terms) {
    String normalized = normalize(content);
    int score = 0;
    for (String term : terms) {
      if (normalized.contains(term)) {
        score += Math.max(1, term.length() - 1);
      }
    }
    return score;
  }

  private String normalize(String value) {
    return value == null ? "" : value
        .toLowerCase(Locale.ROOT)
        .replaceAll("[`*_~\\[\\](){}<>\"'，。！？；：、,.!?\\s-]", "");
  }
}
