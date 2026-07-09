package cn.yinsheng.blog.rag.compute;

import cn.yinsheng.blog.rag.config.RagProperties;
import cn.yinsheng.blog.rag.tool.ToolCall;
import cn.yinsheng.blog.rag.tool.ToolDefinition;
import cn.yinsheng.blog.rag.tool.ToolResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AiComputeClient {
  private final RestClient restClient;
  private final RagProperties properties;
  private final ObjectMapper objectMapper;

  public AiComputeClient(RestClient restClient, RagProperties properties, ObjectMapper objectMapper) {
    this.restClient = restClient;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  public List<Double> embed(String text) {
    JsonNode response = restClient.post()
        .uri(properties.aiComputeBaseUrl() + "/v1/embeddings")
        .headers(headers -> setAuth(headers, properties.aiComputeToken()))
        .body(Map.of("model", properties.embeddingModel(), "input", List.of(text)))
        .retrieve()
        .body(JsonNode.class);

    JsonNode vector = response == null ? null : response.path("data").path(0).path("embedding");
    if (vector == null || !vector.isArray()) {
      throw new IllegalStateException("AI Compute embedding response does not contain a vector");
    }
    List<Double> values = new ArrayList<>(vector.size());
    vector.forEach(value -> values.add(value.asDouble()));
    return values;
  }

  public String chat(String systemPrompt, String userPrompt) {
    JsonNode response = restClient.post()
        .uri(properties.aiComputeBaseUrl() + "/v1/chat/completions")
        .headers(headers -> setAuth(headers, properties.aiComputeToken()))
        .body(Map.of(
            "model", properties.chatModel(),
            "max_tokens", properties.maxAnswerTokens(),
            "temperature", 0.2,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            )
        ))
        .retrieve()
        .body(JsonNode.class);

    String content = response == null ? "" : response.path("choices").path(0).path("message").path("content").asText();
    if (content == null || content.isBlank()) {
      throw new IllegalStateException("AI Compute chat response is empty");
    }
    return content.trim();
  }

  public String chatWithTools(
      String systemPrompt,
      String userPrompt,
      List<ToolDefinition> tools,
      Function<ToolCall, ToolResult> toolExecutor,
      int maxToolLoops
  ) {
    List<Map<String, Object>> messages = new ArrayList<>();
    messages.add(Map.of("role", "system", "content", systemPrompt));
    messages.add(Map.of("role", "user", "content", userPrompt));
    List<Map<String, Object>> openAiTools = tools.stream().map(ToolDefinition::toOpenAiTool).toList();
    int maxLoops = Math.max(1, Math.min(maxToolLoops, 3));
    for (int i = 0; i < maxLoops; i++) {
      JsonNode message = chatCompletion(messages, openAiTools);
      JsonNode toolCalls = message.path("tool_calls");
      if (!toolCalls.isArray() || toolCalls.isEmpty()) {
        String content = message.path("content").asText("");
        if (content.isBlank()) {
          throw new IllegalStateException("AI Compute tool chat response is empty");
        }
        return content.trim();
      }
      messages.add(assistantToolCallMessage(message));
      for (JsonNode toolCallNode : toolCalls) {
        ToolCall call = parseToolCall(toolCallNode);
        ToolResult result = toolExecutor.apply(call);
        messages.add(Map.of(
            "role", "tool",
            "tool_call_id", result.toolCallId(),
            "name", result.name(),
            "content", result.content()
        ));
      }
    }
    throw new IllegalStateException("AI Compute exceeded max tool loop count");
  }

  public String chatStream(String systemPrompt, String userPrompt, Consumer<String> deltaConsumer) {
    StringBuilder answer = new StringBuilder();
    restClient.post()
        .uri(properties.aiComputeBaseUrl() + "/v1/chat/completions")
        .headers(headers -> setAuth(headers, properties.aiComputeToken()))
        .body(Map.of(
            "model", properties.chatModel(),
            "stream", true,
            "max_tokens", properties.maxAnswerTokens(),
            "temperature", 0.2,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            )
        ))
        .exchange((httpRequest, httpResponse) -> {
          try (BufferedReader reader = new BufferedReader(
              new InputStreamReader(httpResponse.getBody(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
              if (!line.startsWith("data:")) {
                continue;
              }
              String data = line.substring("data:".length()).trim();
              if (data.isBlank() || "[DONE]".equals(data)) {
                continue;
              }
              JsonNode node = objectMapper.readTree(data);
              String delta = node.path("choices").path(0).path("delta").path("content").asText("");
              if (!delta.isEmpty()) {
                answer.append(delta);
                deltaConsumer.accept(delta);
              }
            }
          }
          return null;
        });
    String value = answer.toString().trim();
    if (value.isBlank()) {
      throw new IllegalStateException("AI Compute streaming chat response is empty");
    }
    return value;
  }

  private void setAuth(org.springframework.http.HttpHeaders headers, String token) {
    if (token != null && !token.isBlank()) {
      headers.setBearerAuth(token);
    }
  }

  private JsonNode chatCompletion(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("model", properties.chatModel());
    body.put("max_tokens", properties.maxAnswerTokens());
    body.put("temperature", 0.2);
    body.put("messages", messages);
    if (!tools.isEmpty()) {
      body.put("tools", tools);
      body.put("tool_choice", "auto");
    }
    JsonNode response = restClient.post()
        .uri(properties.aiComputeBaseUrl() + "/v1/chat/completions")
        .headers(headers -> setAuth(headers, properties.aiComputeToken()))
        .body(body)
        .retrieve()
        .body(JsonNode.class);
    JsonNode message = response == null ? null : response.path("choices").path(0).path("message");
    if (message == null || message.isMissingNode()) {
      throw new IllegalStateException("AI Compute chat response does not contain a message");
    }
    return message;
  }

  private Map<String, Object> assistantToolCallMessage(JsonNode message) {
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("role", "assistant");
    value.put("content", message.path("content").isMissingNode() ? "" : message.path("content").asText(""));
    value.put("tool_calls", objectMapper.convertValue(message.path("tool_calls"), List.class));
    return value;
  }

  private ToolCall parseToolCall(JsonNode node) {
    String id = node.path("id").asText("");
    JsonNode function = node.path("function");
    String name = function.path("name").asText("");
    String argumentsJson = function.path("arguments").asText("{}");
    Map<String, Object> arguments;
    try {
      arguments = objectMapper.readValue(argumentsJson, Map.class);
    } catch (Exception ex) {
      arguments = Map.of("raw", argumentsJson, "parseError", ex.getMessage());
    }
    return new ToolCall(id, name, arguments);
  }
}
