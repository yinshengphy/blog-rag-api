package cn.yinsheng.blog.service.skill.impl;

public record WeatherReport(
    String city,
    String weather,
    double temperatureCelsius,
    double precipitationMm,
    String source
) {
}
