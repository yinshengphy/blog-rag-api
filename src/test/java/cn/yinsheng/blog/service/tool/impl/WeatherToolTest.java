package cn.yinsheng.blog.service.tool.impl;

import static org.assertj.core.api.Assertions.assertThat;

import cn.yinsheng.blog.service.skill.impl.WeatherProvider;
import cn.yinsheng.blog.service.skill.impl.WeatherReport;
import cn.yinsheng.blog.service.tool.ToolCall;
import cn.yinsheng.blog.service.tool.ToolExecutionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WeatherToolTest {
  @Test
  void shouldReturnProviderFacts() {
    WeatherProvider provider = city -> new WeatherReport(city, "晴", 25, 0, "mock");
    WeatherTool tool = new WeatherTool(provider, new ObjectMapper());
    var result = tool.execute(new ToolCall("1", "weather", Map.of("city", "上海")), new ToolExecutionContext("", "", null));

    assertThat(result.success()).isTrue();
    assertThat(result.content()).contains("上海", "25.0", "mock");
  }
}
