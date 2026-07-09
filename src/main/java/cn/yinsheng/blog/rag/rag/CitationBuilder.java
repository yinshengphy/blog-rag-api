package cn.yinsheng.blog.rag.rag;

import cn.yinsheng.blog.rag.model.Citation;
import cn.yinsheng.blog.rag.model.RetrievedChunk;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CitationBuilder {
  public List<Citation> build(List<RetrievedChunk> chunks) {
    Map<String, Citation> citations = new LinkedHashMap<>();
    for (RetrievedChunk chunk : chunks) {
      String key = chunk.title() + ">" + chunk.section();
      citations.putIfAbsent(key, new Citation(
          chunk.title(),
          chunk.section(),
          chunk.url(),
          snippet(chunk.content())
      ));
    }
    return new ArrayList<>(citations.values());
  }

  private String snippet(String content) {
    String compact = content.replaceAll("\\s+", " ").trim();
    return compact.length() <= 120 ? compact : compact.substring(0, 120) + "...";
  }
}
