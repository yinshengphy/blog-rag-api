package cn.yinsheng.blog.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "web-search")
public class WebSearchProperties {
  private String baseUrl = "http://searxng.ai.svc.cluster.local:8080";
  private int resultLimit = 6;
  private int fetchLimit = 3;
  private int maxPageBytes = 1_000_000;
  private int timeoutSeconds = 10;

  public String getBaseUrl() { return baseUrl; }
  public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
  public int getResultLimit() { return resultLimit; }
  public void setResultLimit(int resultLimit) { this.resultLimit = resultLimit; }
  public int getFetchLimit() { return fetchLimit; }
  public void setFetchLimit(int fetchLimit) { this.fetchLimit = fetchLimit; }
  public int getMaxPageBytes() { return maxPageBytes; }
  public void setMaxPageBytes(int maxPageBytes) { this.maxPageBytes = maxPageBytes; }
  public int getTimeoutSeconds() { return timeoutSeconds; }
  public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
}
