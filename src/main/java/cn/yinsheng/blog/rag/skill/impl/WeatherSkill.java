package cn.yinsheng.blog.rag.skill.impl;

import cn.yinsheng.blog.rag.intent.IntentType;
import cn.yinsheng.blog.rag.skill.CapabilityDescriptor;
import cn.yinsheng.blog.rag.skill.Skill;
import cn.yinsheng.blog.rag.skill.SkillRequest;
import cn.yinsheng.blog.rag.skill.SkillResult;
import cn.yinsheng.blog.rag.skill.SkillRiskLevel;
import cn.yinsheng.blog.rag.skill.SkillStatus;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WeatherSkill implements Skill {
  private static final Logger log = LoggerFactory.getLogger(WeatherSkill.class);

  private final WeatherProvider weatherProvider;

  public WeatherSkill(WeatherProvider weatherProvider) {
    this.weatherProvider = weatherProvider;
  }

  @Override
  public String id() {
    return "weather";
  }

  @Override
  public CapabilityDescriptor descriptor() {
    return new CapabilityDescriptor(
        id(),
        "天气查询",
        "通用工具",
        "按城市查询当前天气，答案来自天气服务。",
        SkillRiskLevel.READ_ONLY_EXTERNAL,
        SkillStatus.ENABLED,
        List.of(IntentType.WEATHER_QUERY)
    );
  }

  @Override
  public SkillResult execute(SkillRequest request) {
    String city = extractCity(request.question());
    if (city.isBlank()) {
      return SkillResult.answer("你想查询哪个城市的天气？请告诉我城市名。");
    }
    try {
      WeatherReport report = weatherProvider.currentWeather(city);
      String rainHint = report.precipitationMm() > 0 ? "当前有降水迹象。" : "当前没有明显降水。";
      String answer = "%s 当前天气：%s，气温 %.1f℃，降水 %.1f mm。%s（来源：%s）"
          .formatted(report.city(), report.weather(), report.temperatureCelsius(), report.precipitationMm(), rainHint, report.source());
      return SkillResult.answer(answer, Map.of("city", report.city(), "source", report.source()));
    } catch (Exception ex) {
      log.warn("天气查询失败 city={}", city, ex);
      return SkillResult.answer("我暂时没能查到 " + city + " 的天气数据，请稍后再试或换一个城市名。");
    }
  }

  private String extractCity(String question) {
    String compact = question.replaceAll("\\s+", "")
        .replace("请问", "")
        .replace("帮我查", "")
        .replace("查询", "");
    int keywordIndex = firstKeywordIndex(compact, "下雨", "天气", "气温", "温度", "降雨");
    if (keywordIndex <= 0) {
      return "";
    }
    return compact.substring(0, keywordIndex)
        .replace("今天", "")
        .replace("现在", "")
        .replace("明天", "")
        .replace("会", "")
        .trim();
  }

  private int firstKeywordIndex(String value, String... keywords) {
    int index = -1;
    for (String keyword : keywords) {
      int current = value.indexOf(keyword);
      if (current >= 0 && (index < 0 || current < index)) {
        index = current;
      }
    }
    return index;
  }
}
