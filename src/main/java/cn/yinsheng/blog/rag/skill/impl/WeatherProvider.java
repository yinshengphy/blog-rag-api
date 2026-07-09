package cn.yinsheng.blog.rag.skill.impl;

public interface WeatherProvider {
  WeatherReport currentWeather(String city);
}
