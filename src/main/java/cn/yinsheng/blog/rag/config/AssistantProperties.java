package cn.yinsheng.blog.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "assistant")
public class AssistantProperties {
  private int maxToolCallsPerRequest = 3;

  public int getMaxToolCallsPerRequest() {
    return maxToolCallsPerRequest;
  }

  public void setMaxToolCallsPerRequest(int maxToolCallsPerRequest) {
    this.maxToolCallsPerRequest = maxToolCallsPerRequest;
  }
}
