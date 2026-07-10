package cn.yinsheng.blog.rag.web;

import cn.yinsheng.blog.rag.config.WebSearchProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SearxngSearchProvider implements WebSearchProvider {
  private final RestClient restClient;
  private final WebSearchProperties properties;

  public SearxngSearchProvider(RestClient restClient, WebSearchProperties properties) {
    this.restClient = restClient;
    this.properties = properties;
  }

  @Override
  public List<WebSearchResult> search(String query, String engine, int page) {
    StringBuilder url = new StringBuilder(properties.getBaseUrl())
        .append("/search?format=json&q=").append(encode(query))
        .append("&pageno=").append(Math.max(1, page));
    if (engine != null && !engine.isBlank() && !"auto".equalsIgnoreCase(engine)) {
      url.append("&engines=").append(encode(engine));
    }
    JsonNode response = restClient.get().uri(url.toString()).retrieve().body(JsonNode.class);
    List<WebSearchResult> results = new ArrayList<>();
    if (response != null) {
      for (JsonNode item : response.path("results")) {
        if (results.size() >= properties.getResultLimit()) break;
        results.add(new WebSearchResult(
            item.path("title").asText(),
            item.path("url").asText(),
            item.path("content").asText(),
            item.path("engine").asText(),
            item.path("publishedDate").asText()
        ));
      }
    }
    return results;
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
