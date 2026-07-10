package cn.yinsheng.blog.rag;

import cn.yinsheng.blog.rag.config.AssistantProperties;
import cn.yinsheng.blog.rag.config.RagProperties;
import cn.yinsheng.blog.rag.config.WeatherProperties;
import cn.yinsheng.blog.rag.config.WebSearchProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({RagProperties.class, AssistantProperties.class, WeatherProperties.class, WebSearchProperties.class})
public class BlogRagApplication {

  public static void main(String[] args) {
    SpringApplication.run(BlogRagApplication.class, args);
  }
}
