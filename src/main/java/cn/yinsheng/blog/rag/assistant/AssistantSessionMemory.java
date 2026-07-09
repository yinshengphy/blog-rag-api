package cn.yinsheng.blog.rag.assistant;

import cn.yinsheng.blog.rag.intent.IntentType;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class AssistantSessionMemory {
  private static final Duration TTL = Duration.ofMinutes(15);
  private static final String WEATHER_CITY_SLOT = "weather.city";

  private final ConcurrentMap<String, PendingSlot> pendingSlots = new ConcurrentHashMap<>();

  public Optional<PendingSlot> pendingSlot(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      return Optional.empty();
    }
    cleanup();
    PendingSlot slot = pendingSlots.get(sessionId);
    if (slot == null || slot.isExpired()) {
      pendingSlots.remove(sessionId);
      return Optional.empty();
    }
    return Optional.of(slot);
  }

  public void rememberWeatherCitySlot(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      return;
    }
    pendingSlots.put(sessionId, new PendingSlot(IntentType.WEATHER_QUERY, WEATHER_CITY_SLOT, Instant.now().plus(TTL)));
  }

  public void clear(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      return;
    }
    pendingSlots.remove(sessionId);
  }

  private void cleanup() {
    pendingSlots.entrySet().removeIf(entry -> entry.getValue().isExpired());
  }

  public record PendingSlot(IntentType intentType, String slot, Instant expiresAt) {
    public boolean isWeatherCitySlot() {
      return intentType == IntentType.WEATHER_QUERY && WEATHER_CITY_SLOT.equals(slot);
    }

    private boolean isExpired() {
      return Instant.now().isAfter(expiresAt);
    }
  }
}
