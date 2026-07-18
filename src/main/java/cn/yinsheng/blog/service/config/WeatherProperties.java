package cn.yinsheng.blog.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "weather")
public class WeatherProperties {
  private String provider = "open-meteo";
  private OpenMeteo openMeteo = new OpenMeteo();
  private Amap amap = new Amap();

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider == null || provider.isBlank() ? "open-meteo" : provider;
  }

  public OpenMeteo getOpenMeteo() {
    return openMeteo;
  }

  public void setOpenMeteo(OpenMeteo openMeteo) {
    this.openMeteo = openMeteo == null ? new OpenMeteo() : openMeteo;
  }

  public Amap getAmap() {
    return amap;
  }

  public void setAmap(Amap amap) {
    this.amap = amap == null ? new Amap() : amap;
  }

  public static class OpenMeteo {
    private String baseUrl = "https://api.open-meteo.com";
    private String geocodingBaseUrl = "https://geocoding-api.open-meteo.com";

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl == null || baseUrl.isBlank() ? "https://api.open-meteo.com" : baseUrl;
    }

    public String getGeocodingBaseUrl() {
      return geocodingBaseUrl;
    }

    public void setGeocodingBaseUrl(String geocodingBaseUrl) {
      this.geocodingBaseUrl = geocodingBaseUrl == null || geocodingBaseUrl.isBlank()
          ? "https://geocoding-api.open-meteo.com"
          : geocodingBaseUrl;
    }
  }

  public static class Amap {
    private String key = "";

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key == null ? "" : key;
    }
  }
}
