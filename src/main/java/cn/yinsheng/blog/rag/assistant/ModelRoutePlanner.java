package cn.yinsheng.blog.rag.assistant;

import cn.yinsheng.blog.rag.compute.AiComputeClient;
import cn.yinsheng.blog.rag.model.ChatRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class ModelRoutePlanner {
  private final AiComputeClient aiComputeClient;
  private final ObjectMapper objectMapper;
  private final String prompt;

  public ModelRoutePlanner(AiComputeClient aiComputeClient, ObjectMapper objectMapper) {
    this.aiComputeClient = aiComputeClient;
    this.objectMapper = objectMapper;
    this.prompt = loadPrompt();
  }

  public RoutePlan plan(ChatRequest request) {
    return plan(request, List.of());
  }

  public RoutePlan plan(ChatRequest request, List<Map<String, Object>> history) {
    try {
      String input = objectMapper.writeValueAsString(Map.of(
          "question", request.question(),
          "hasImages", request.images() != null && !request.images().isEmpty(),
          "currentPage", request.pageContext() == null ? Map.of() : request.pageContext(),
          "recentConversation", history == null ? List.of() : history
      ));
      String output = aiComputeClient.classify(prompt, input).trim();
      if (output.startsWith("```")) {
        output = output.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
      }
      JsonNode node = objectMapper.readTree(output);
      Route route = Route.valueOf(node.path("route").asText("UNKNOWN").toUpperCase());
      Map<String, Object> arguments = new LinkedHashMap<>();
      copyText(node, arguments, "query");
      copyText(node, arguments, "scope");
      copyText(node, arguments, "target");
      copyText(node, arguments, "focus");
      copyText(node, arguments, "city");
      copyText(node, arguments, "engine");
      copyText(node, arguments, "category");
      if (node.path("page").canConvertToInt()) arguments.put("page", Math.max(1, node.path("page").asInt(1)));
      if (route == Route.BLOG_SUMMARY && !arguments.containsKey("target")
          && (request.pageContext() == null || !request.pageContext().isBlogPost())) {
        arguments.put("target", request.question().trim());
      }
      return new RoutePlan(route, Map.copyOf(arguments));
    } catch (Exception ex) {
      return new RoutePlan(Route.UNKNOWN, Map.of());
    }
  }

  private void copyText(JsonNode node, Map<String, Object> arguments, String name) {
    String value = node.path(name).asText("").trim();
    if (!value.isBlank()) arguments.put(name, value);
  }

  private String loadPrompt() {
    try {
      return new ClassPathResource("prompts/route-planner.md").getContentAsString(StandardCharsets.UTF_8);
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to load route planner prompt", ex);
    }
  }

  public enum Route {
    DIRECT,
    BLOG_QA,
    BLOG_SUMMARY,
    WEATHER,
    WEB_RESEARCH,
    UNKNOWN
  }

  public record RoutePlan(Route route, Map<String, Object> arguments) {
    public String toolName() {
      return switch (route) {
        case BLOG_QA -> "blog_qa";
        case BLOG_SUMMARY -> "blog_summary";
        case WEATHER -> "weather";
        case WEB_RESEARCH -> "web_research";
        default -> "";
      };
    }
  }
}
