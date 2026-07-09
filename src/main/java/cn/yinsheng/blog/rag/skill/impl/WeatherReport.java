package cn.yinsheng.blog.rag.skill.impl;

public record WeatherReport(
    String city,
    String weather,
    double temperatureCelsius,
    double precipitationMm,
    String source
) {
}
