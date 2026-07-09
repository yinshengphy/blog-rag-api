package cn.yinsheng.blog.rag.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "assistant")
public class AssistantProperties {
  private Intent intent = new Intent();
  private int maxSkillCallsPerRequest = 3;
  private int maxToolCallsPerRequest = 3;
  private String defaultZoneId = "Asia/Shanghai";
  private Map<String, SkillSettings> skills = new HashMap<>();

  public Intent getIntent() {
    return intent;
  }

  public void setIntent(Intent intent) {
    this.intent = intent == null ? new Intent() : intent;
  }

  public int getMaxSkillCallsPerRequest() {
    return maxSkillCallsPerRequest;
  }

  public void setMaxSkillCallsPerRequest(int maxSkillCallsPerRequest) {
    this.maxSkillCallsPerRequest = maxSkillCallsPerRequest;
  }

  public int getMaxToolCallsPerRequest() {
    return maxToolCallsPerRequest;
  }

  public void setMaxToolCallsPerRequest(int maxToolCallsPerRequest) {
    this.maxToolCallsPerRequest = maxToolCallsPerRequest;
  }

  public String getDefaultZoneId() {
    return defaultZoneId;
  }

  public void setDefaultZoneId(String defaultZoneId) {
    this.defaultZoneId = defaultZoneId == null || defaultZoneId.isBlank() ? "Asia/Shanghai" : defaultZoneId;
  }

  public Map<String, SkillSettings> getSkills() {
    return skills;
  }

  public void setSkills(Map<String, SkillSettings> skills) {
    this.skills = skills == null ? new HashMap<>() : skills;
  }

  public boolean isSkillEnabled(String skillId, boolean defaultEnabled) {
    SkillSettings settings = skills.get(skillId);
    return settings == null ? defaultEnabled : settings.isEnabled();
  }

  public static class Intent {
    private double lowConfidenceThreshold = 0.75;

    public double getLowConfidenceThreshold() {
      return lowConfidenceThreshold;
    }

    public void setLowConfidenceThreshold(double lowConfidenceThreshold) {
      this.lowConfidenceThreshold = lowConfidenceThreshold;
    }
  }

  public static class SkillSettings {
    private boolean enabled = true;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }
}
