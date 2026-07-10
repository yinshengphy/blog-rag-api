package cn.yinsheng.blog.rag.web;

import cn.yinsheng.blog.rag.config.WebSearchProperties;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Component
public class WebPageReader {
  private final WebSearchProperties properties;
  private final HttpClient httpClient;

  public WebPageReader(WebSearchProperties properties) {
    this.properties = properties;
    this.httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NEVER)
        .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
        .build();
  }

  public String read(String rawUrl) {
    try {
      URI uri = URI.create(rawUrl);
      for (int redirects = 0; redirects < 4; redirects++) {
        validate(uri);
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
            .header("User-Agent", "yinsheng-site-assistant/1.0")
            .GET().build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() >= 300 && response.statusCode() < 400) {
          String location = response.headers().firstValue("location").orElseThrow();
          uri = uri.resolve(location);
          continue;
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) return "";
        String contentType = response.headers().firstValue("content-type").orElse("").toLowerCase(Locale.ROOT);
        if (!contentType.contains("text/html") && !contentType.contains("text/plain")) return "";
        byte[] bytes;
        try (InputStream input = response.body()) {
          bytes = input.readNBytes(properties.getMaxPageBytes() + 1);
        }
        if (bytes.length > properties.getMaxPageBytes()) return "";
        if (contentType.contains("text/plain")) return compact(new String(bytes, StandardCharsets.UTF_8));
        Document document = Jsoup.parse(new ByteArrayInputStream(bytes), null, uri.toString());
        document.select("script,style,noscript,nav,footer,header,aside,form").remove();
        return compact(document.body().text());
      }
    } catch (Exception ignored) {
      return "";
    }
    return "";
  }

  private void validate(URI uri) throws Exception {
    if (uri.getScheme() == null || !(uri.getScheme().equals("http") || uri.getScheme().equals("https"))) {
      throw new IllegalArgumentException("Unsupported URL scheme");
    }
    if (uri.getHost() == null) throw new IllegalArgumentException("URL host is missing");
    for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
      if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
          || address.isSiteLocalAddress() || address.isMulticastAddress() || isUniqueLocalV6(address)) {
        throw new IllegalArgumentException("Private network addresses are not allowed");
      }
    }
  }

  private boolean isUniqueLocalV6(InetAddress address) {
    byte[] bytes = address.getAddress();
    return bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
  }

  private String compact(String value) {
    String compact = value.replaceAll("\\s+", " ").trim();
    return compact.length() <= 5000 ? compact : compact.substring(0, 5000);
  }
}
