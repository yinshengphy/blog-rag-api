package cn.yinsheng.blog.rag.skill.impl;

import cn.yinsheng.blog.rag.config.WeatherProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "weather", name = "provider", havingValue = "open-meteo", matchIfMissing = true)
public class OpenMeteoWeatherProvider implements WeatherProvider {
  private final RestClient restClient;
  private final WeatherProperties properties;

  public OpenMeteoWeatherProvider(RestClient restClient, WeatherProperties properties) {
    this.restClient = restClient;
    this.properties = properties;
  }

  @Override
  public WeatherReport currentWeather(String city) {
    String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
    JsonNode geocoding = restClient.get()
        .uri(properties.getOpenMeteo().getGeocodingBaseUrl()
            + "/v1/search?name=" + encodedCity + "&count=1&language=zh&format=json")
        .retrieve()
        .body(JsonNode.class);
    JsonNode first = geocoding == null ? null : geocoding.path("results").path(0);
    if (first == null || first.isMissingNode()) {
      throw new IllegalArgumentException("找不到城市：" + city);
    }
    double latitude = first.path("latitude").asDouble();
    double longitude = first.path("longitude").asDouble();
    String resolvedCity = first.path("name").asText(city);
    JsonNode forecast = restClient.get()
        .uri(properties.getOpenMeteo().getBaseUrl()
            + "/v1/forecast?latitude=" + latitude
            + "&longitude=" + longitude
            + "&current=temperature_2m,precipitation,rain,weather_code&timezone=auto")
        .retrieve()
        .body(JsonNode.class);
    JsonNode current = forecast == null ? null : forecast.path("current");
    if (current == null || current.isMissingNode()) {
      throw new IllegalStateException("天气接口没有返回当前天气");
    }
    int weatherCode = current.path("weather_code").asInt(-1);
    double temperature = current.path("temperature_2m").asDouble();
    double precipitation = current.path("precipitation").asDouble(current.path("rain").asDouble(0));
    return new WeatherReport(resolvedCity, describe(weatherCode), temperature, precipitation, "Open-Meteo");
  }

  private String describe(int code) {
    if (code == 0) {
      return "晴";
    }
    if (code <= 3) {
      return "多云";
    }
    if (code == 45 || code == 48) {
      return "雾";
    }
    if (code >= 51 && code <= 67) {
      return "小雨或冻雨";
    }
    if (code >= 71 && code <= 77) {
      return "降雪";
    }
    if (code >= 80 && code <= 82) {
      return "阵雨";
    }
    if (code >= 95) {
      return "雷雨";
    }
    return "未知天气";
  }
}
