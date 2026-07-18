package cn.yinsheng.blog.service.skill.impl;

public interface WeatherProvider {
  WeatherReport currentWeather(String city);
}
