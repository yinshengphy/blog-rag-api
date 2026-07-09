package cn.yinsheng.blog.rag.skill.impl;

import cn.yinsheng.blog.rag.config.WeatherProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "weather", name = "provider", havingValue = "amap")
public class AmapWeatherProvider implements WeatherProvider {
  private final RestClient restClient;
  private final WeatherProperties properties;

  public AmapWeatherProvider(RestClient restClient, WeatherProperties properties) {
    this.restClient = restClient;
    this.properties = properties;
  }

  @Override
  public WeatherReport currentWeather(String city) {
    String key = properties.getAmap().getKey();
    if (key.isBlank()) {
      throw new IllegalStateException("weather.amap.key 未配置");
    }
    JsonNode response = restClient.get()
        .uri("https://restapi.amap.com/v3/weather/weatherInfo?city="
            + URLEncoder.encode(city, StandardCharsets.UTF_8)
            + "&key=" + URLEncoder.encode(key, StandardCharsets.UTF_8)
            + "&extensions=base")
        .retrieve()
        .body(JsonNode.class);
    JsonNode live = response == null ? null : response.path("lives").path(0);
    if (live == null || live.isMissingNode()) {
      throw new IllegalArgumentException("高德天气没有返回城市数据：" + city);
    }
    double temperature = parseDouble(live.path("temperature").asText("0"));
    String weather = live.path("weather").asText("未知天气");
    double precipitation = weather.contains("雨") ? 0.1 : 0;
    return new WeatherReport(live.path("city").asText(city), weather, temperature, precipitation, "高德天气");
  }

  private double parseDouble(String value) {
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException ex) {
      return 0;
    }
  }
}
