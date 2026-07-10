package cn.yinsheng.blog.rag.rag;

import cn.yinsheng.blog.rag.qdrant.QdrantClient;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class BlogCatalogService {
  private final QdrantClient qdrantClient;

  public BlogCatalogService(QdrantClient qdrantClient) {
    this.qdrantClient = qdrantClient;
  }

  public Resolution resolve(String target) {
    String query = normalize(target);
    if (query.isBlank()) {
      return Resolution.notFound();
    }
    List<ScoredPost> matches = qdrantClient.listPosts().stream()
        .map(post -> new ScoredPost(post, score(query, post)))
        .filter(match -> match.score() > 0)
        .sorted(Comparator.comparingInt(ScoredPost::score).reversed())
        .toList();
    if (matches.isEmpty()) {
      return Resolution.notFound();
    }
    int topScore = matches.get(0).score();
    List<QdrantClient.PostEntry> top = matches.stream()
        .filter(match -> match.score() == topScore)
        .map(ScoredPost::post)
        .limit(5)
        .toList();
    return top.size() == 1 ? Resolution.found(top.get(0)) : Resolution.ambiguous(top);
  }

  private int score(String query, QdrantClient.PostEntry post) {
    String slug = normalize(post.slug());
    String title = normalize(post.title());
    if (query.equals(slug) || query.equals(title)) return 100;
    if (title.contains(query)) return 80 + Math.min(10, query.length());
    if (query.contains(title)) return 70 + Math.min(10, title.length());
    if (slug.contains(query) || query.contains(slug)) return 60;
    return 0;
  }

  private String normalize(String value) {
    return value == null ? "" : Normalizer.normalize(value, Normalizer.Form.NFKC)
        .toLowerCase(Locale.ROOT)
        .replaceAll("[\\s/\\-_'\"，。！？：；（）()]+", "");
  }

  private record ScoredPost(QdrantClient.PostEntry post, int score) {
  }

  public record Resolution(QdrantClient.PostEntry post, List<QdrantClient.PostEntry> candidates) {
    public static Resolution found(QdrantClient.PostEntry post) { return new Resolution(post, List.of()); }
    public static Resolution ambiguous(List<QdrantClient.PostEntry> candidates) { return new Resolution(null, candidates); }
    public static Resolution notFound() { return new Resolution(null, List.of()); }
    public boolean found() { return post != null; }
    public boolean ambiguous() { return post == null && !candidates.isEmpty(); }
  }
}
