package cn.yinsheng.blog.rag.assistant;

import cn.yinsheng.blog.rag.compute.AiComputeClient;
import cn.yinsheng.blog.rag.config.AssistantProperties;
import cn.yinsheng.blog.rag.intent.IntentResult;
import cn.yinsheng.blog.rag.intent.IntentRouter;
import cn.yinsheng.blog.rag.intent.IntentType;
import cn.yinsheng.blog.rag.model.ChatResponse;
import cn.yinsheng.blog.rag.rag.BlogRagService;
import cn.yinsheng.blog.rag.skill.SkillContext;
import cn.yinsheng.blog.rag.skill.SkillExecutor;
import cn.yinsheng.blog.rag.skill.SkillResult;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ChatOrchestrator {
  private static final Logger log = LoggerFactory.getLogger(ChatOrchestrator.class);

  private final IntentRouter intentRouter;
  private final SkillExecutor skillExecutor;
  private final BlogRagService blogRagService;
  private final AiComputeClient aiComputeClient;
  private final AssistantProperties properties;
  private final AnswerComposer answerComposer;
  private final AssistantSessionMemory sessionMemory;

  public ChatOrchestrator(
      IntentRouter intentRouter,
      SkillExecutor skillExecutor,
      BlogRagService blogRagService,
      AiComputeClient aiComputeClient,
      AssistantProperties properties,
      AnswerComposer answerComposer,
      AssistantSessionMemory sessionMemory
  ) {
    this.intentRouter = intentRouter;
    this.skillExecutor = skillExecutor;
    this.blogRagService = blogRagService;
    this.aiComputeClient = aiComputeClient;
    this.properties = properties;
    this.answerComposer = answerComposer;
    this.sessionMemory = sessionMemory;
  }

  public ChatResponse answer(String question) {
    return answer(question, null);
  }

  public ChatResponse answer(String question, String sessionId) {
    long startedAt = System.currentTimeMillis();
    String traceId = UUID.randomUUID().toString();
    ResolvedQuestion resolvedQuestion = resolveQuestion(question, sessionId);
    IntentResult intent = resolvedQuestion.intent();
    SkillContext skillContext = SkillContext.publicUser(zoneId(), traceId);
    ChatResponse response = null;
    String errorCode = "";
    try {
      response = route(resolvedQuestion.question(), intent, skillContext);
      updateSessionMemory(sessionId, response);
      return response;
    } catch (RuntimeException ex) {
      errorCode = ex.getClass().getSimpleName();
      throw ex;
    } finally {
      long latencyMs = System.currentTimeMillis() - startedAt;
      logRequest(traceId, intent, response, latencyMs, errorCode);
    }
  }

  public ChatResponse streamAnswer(String question, Consumer<String> deltaConsumer) {
    return streamAnswer(question, null, response -> {
    }, deltaConsumer);
  }

  public ChatResponse streamAnswer(String question, Consumer<ChatResponse> metaConsumer, Consumer<String> deltaConsumer) {
    return streamAnswer(question, null, metaConsumer, deltaConsumer);
  }

  public ChatResponse streamAnswer(String question, String sessionId, Consumer<ChatResponse> metaConsumer, Consumer<String> deltaConsumer) {
    long startedAt = System.currentTimeMillis();
    String traceId = UUID.randomUUID().toString();
    ResolvedQuestion resolvedQuestion = resolveQuestion(question, sessionId);
    IntentResult intent = resolvedQuestion.intent();
    SkillContext skillContext = SkillContext.publicUser(zoneId(), traceId);
    String errorCode = "";
    ChatResponse response = null;
    try {
      if (isBlogIntent(intent.type())) {
        metaConsumer.accept(simple("", ChatMode.BLOG_RAG, intent));
        ChatResponse blogResponse = blogRagService.streamAnswer(resolvedQuestion.question(), deltaConsumer);
        response = withMetadata(blogResponse, ChatMode.BLOG_RAG, intent, blogResponse.usedSkills(), blogResponse.usedTools(), blogResponse.metadata());
        updateSessionMemory(sessionId, response);
        return response;
      }
      response = route(resolvedQuestion.question(), intent, skillContext);
      updateSessionMemory(sessionId, response);
      metaConsumer.accept(response);
      deltaConsumer.accept(response.answer());
      return response;
    } catch (RuntimeException ex) {
      errorCode = ex.getClass().getSimpleName();
      throw ex;
    } finally {
      logRequest(traceId, intent, response, System.currentTimeMillis() - startedAt, errorCode);
    }
  }

  private ChatResponse route(String question, IntentResult intent, SkillContext skillContext) {
    IntentType type = intent.type();
    if (type == IntentType.UNSAFE_OR_FORBIDDEN) {
      return simple(answerComposer.refusal(), ChatMode.REFUSAL, intent);
    }
    if (type == IntentType.CLARIFICATION) {
      return simple(answerComposer.directAnswer(type), ChatMode.CLARIFICATION, intent);
    }
    if (type == IntentType.GREETING || type == IntentType.SELF_INTRO || type == IntentType.SMALL_TALK) {
      return simple(answerComposer.directAnswer(type), ChatMode.DIRECT, intent);
    }
    if (isSkillIntent(type)) {
      return skillExecutor.execute(question, intent, skillContext)
          .map(executed -> fromSkill(executed.skillId(), executed.result(), intent))
          .orElseGet(() -> simple(answerComposer.fallback(), ChatMode.FALLBACK, intent));
    }
    if (isBlogIntent(type)) {
      ChatResponse response = blogRagService.answer(question);
      return withMetadata(response, ChatMode.BLOG_RAG, intent, response.usedSkills(), response.usedTools(), response.metadata());
    }
    if (type == IntentType.GENERAL_TECH_QA) {
      ChatResponse blogResponse = blogRagService.answer(question);
      if (!blogResponse.citations().isEmpty()) {
        return withMetadata(blogResponse, ChatMode.BLOG_RAG, intent, blogResponse.usedSkills(), blogResponse.usedTools(), blogResponse.metadata());
      }
      String answer = aiComputeClient.chat(answerComposer.generalSystemPrompt(), "用户问题：\n" + question);
      if (!answer.startsWith("补充说明")) {
        answer = "补充说明：" + answer;
      }
      return new ChatResponse(answer, List.of(), List.of(), ChatMode.GENERAL_TECH.name(), type.name(), List.of(), List.of(), Map.of());
    }
    return simple(answerComposer.fallback(), ChatMode.FALLBACK, intent);
  }

  private ResolvedQuestion resolveQuestion(String question, String sessionId) {
    return sessionMemory.pendingSlot(sessionId)
        .filter(AssistantSessionMemory.PendingSlot::isWeatherCitySlot)
        .filter(slot -> looksLikeWeatherCityAnswer(question))
        .map(slot -> new ResolvedQuestion(
            question + "天气",
            IntentResult.of(IntentType.WEATHER_QUERY, 0.93, "会话补充天气城市")
        ))
        .orElseGet(() -> new ResolvedQuestion(question, intentRouter.route(question)));
  }

  private boolean looksLikeWeatherCityAnswer(String question) {
    String value = question == null ? "" : question.trim();
    if (value.isBlank() || value.length() > 20) {
      return false;
    }
    return !value.matches(".*[?？。!！,，].*");
  }

  private void updateSessionMemory(String sessionId, ChatResponse response) {
    if (sessionId == null || sessionId.isBlank() || response == null) {
      return;
    }
    Object pendingSlot = response.metadata().get("pendingSlot");
    if ("weather.city".equals(pendingSlot)) {
      sessionMemory.rememberWeatherCitySlot(sessionId);
      return;
    }
    sessionMemory.clear(sessionId);
  }

  private boolean isSkillIntent(IntentType type) {
    return type == IntentType.CAPABILITY_QUERY
        || type == IntentType.WEATHER_QUERY
        || type == IntentType.JOKE
        || type == IntentType.CALCULATOR
        || type == IntentType.UNIT_CONVERT
        || type == IntentType.DATETIME_QUERY
        || type == IntentType.TIMESTAMP_CONVERT
        || type == IntentType.URL_CODEC
        || type == IntentType.BASE64_CODEC;
  }

  private boolean isBlogIntent(IntentType type) {
    return type == IntentType.BLOG_QA
        || type == IntentType.BLOG_SEARCH
        || type == IntentType.BLOG_SUMMARY
        || type == IntentType.BLOG_RECOMMEND
        || type == IntentType.BLOG_NAVIGATION;
  }

  private ChatResponse fromSkill(String skillId, SkillResult result, IntentResult intent) {
    return new ChatResponse(
        result.answer(),
        result.citations(),
        result.relatedPosts(),
        ChatMode.SKILL.name(),
        intent.type().name(),
        List.of(skillId),
        result.usedTools(),
        mergeMetadata(intent, result.metadata())
    );
  }

  private ChatResponse simple(String answer, ChatMode mode, IntentResult intent) {
    return new ChatResponse(answer, List.of(), List.of(), mode.name(), intent.type().name(), List.of(), List.of(), mergeMetadata(intent, Map.of()));
  }

  private ChatResponse withMetadata(
      ChatResponse response,
      ChatMode mode,
      IntentResult intent,
      List<String> usedSkills,
      List<String> usedTools,
      Map<String, Object> metadata
  ) {
    return new ChatResponse(
        response.answer(),
        response.citations(),
        response.relatedPosts(),
        mode.name(),
        intent.type().name(),
        usedSkills,
        usedTools,
        mergeMetadata(intent, metadata)
    );
  }

  private Map<String, Object> mergeMetadata(IntentResult intent, Map<String, Object> metadata) {
    Map<String, Object> values = new LinkedHashMap<>(metadata);
    values.put("intentConfidence", intent.confidence());
    values.put("intentReason", intent.reason());
    return values;
  }

  private ZoneId zoneId() {
    try {
      return ZoneId.of(properties.getDefaultZoneId());
    } catch (Exception ex) {
      return ZoneId.of("Asia/Shanghai");
    }
  }

  private void logRequest(String traceId, IntentResult intent, ChatResponse response, long latencyMs, String errorCode) {
    String mode = response == null ? "" : String.valueOf(response.mode());
    List<String> usedSkills = response == null ? List.of() : response.usedSkills();
    List<String> usedTools = response == null ? List.of() : response.usedTools();
    Object ragTopScore = response == null ? "" : response.metadata().getOrDefault("ragTopScore", "");
    log.info(
        "assistant_request traceId={} intent={} confidence={} mode={} usedSkills={} usedTools={} latencyMs={} ragTopScore={} errorCode={}",
        traceId,
        intent.type(),
        intent.confidence(),
        mode,
        usedSkills,
        usedTools,
        latencyMs,
        ragTopScore,
        errorCode
    );
  }

  private record ResolvedQuestion(String question, IntentResult intent) {
  }
}
