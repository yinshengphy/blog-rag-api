package cn.yinsheng.blog.rag.rag;

import cn.yinsheng.blog.rag.chat.ChatBusyException;
import cn.yinsheng.blog.rag.chat.ChatLimiter;
import cn.yinsheng.blog.rag.compute.AiComputeClient;
import cn.yinsheng.blog.rag.model.ChatResponse;
import cn.yinsheng.blog.rag.model.RetrievedChunk;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BlogRagService {
  private static final Logger log = LoggerFactory.getLogger(BlogRagService.class);
  public static final String NO_CONFIDENT_MATCH_ANSWER = "我没有在博客里找到足够确定的对应内容。你可以换个更具体的关键词，或者问我有哪些能力。";

  private final AiComputeClient aiComputeClient;
  private final ChatLimiter limiter;
  private final BlogRetriever blogRetriever;
  private final BlogPromptBuilder promptBuilder;
  private final CitationBuilder citationBuilder;
  private final RelatedPostBuilder relatedPostBuilder;

  public BlogRagService(
      AiComputeClient aiComputeClient,
      ChatLimiter limiter,
      BlogRetriever blogRetriever,
      BlogPromptBuilder promptBuilder,
      CitationBuilder citationBuilder,
      RelatedPostBuilder relatedPostBuilder
  ) {
    this.aiComputeClient = aiComputeClient;
    this.limiter = limiter;
    this.blogRetriever = blogRetriever;
    this.promptBuilder = promptBuilder;
    this.citationBuilder = citationBuilder;
    this.relatedPostBuilder = relatedPostBuilder;
  }

  public ChatResponse answer(String question) {
    if (!limiter.tryEnter()) {
      throw new ChatBusyException("当前请求较多，请稍后再试。");
    }
    try {
      long startedAt = System.currentTimeMillis();
      List<RetrievedChunk> ranked = blogRetriever.retrieve(question);
      if (!hasConfidentMatch(ranked)) {
        return response(NO_CONFIDENT_MATCH_ANSWER, ranked, "");
      }
      String answer = aiComputeClient.chat(promptBuilder.systemPrompt(), promptBuilder.userPrompt(question, ranked));
      log.info("Answered blog RAG in {} ms with {} chunks", System.currentTimeMillis() - startedAt, ranked.size());
      return response(answer, ranked, "blog-search");
    } finally {
      limiter.leave();
    }
  }

  public ChatResponse streamAnswer(String question, Consumer<String> deltaConsumer) {
    if (!limiter.tryEnter()) {
      throw new ChatBusyException("当前请求较多，请稍后再试。");
    }
    try {
      long startedAt = System.currentTimeMillis();
      List<RetrievedChunk> ranked = blogRetriever.retrieve(question);
      if (!hasConfidentMatch(ranked)) {
        deltaConsumer.accept(NO_CONFIDENT_MATCH_ANSWER);
        return response(NO_CONFIDENT_MATCH_ANSWER, ranked, "");
      }
      String answer = aiComputeClient.chatStream(
          promptBuilder.systemPrompt(),
          promptBuilder.userPrompt(question, ranked),
          deltaConsumer
      );
      log.info("Streamed blog RAG in {} ms with {} chunks", System.currentTimeMillis() - startedAt, ranked.size());
      return response(answer, ranked, "blog-search");
    } finally {
      limiter.leave();
    }
  }

  public boolean hasConfidentMatch(List<RetrievedChunk> ranked) {
    return !ranked.isEmpty() && ranked.get(0).score() >= 0.35;
  }

  public double topScore(List<RetrievedChunk> ranked) {
    return ranked.isEmpty() ? 0 : ranked.get(0).score();
  }

  private ChatResponse response(String answer, List<RetrievedChunk> ranked, String usedSkill) {
    List<String> usedSkills = usedSkill.isBlank() ? List.of() : List.of(usedSkill);
    return new ChatResponse(
        answer,
        hasConfidentMatch(ranked) ? citationBuilder.build(ranked) : List.of(),
        hasConfidentMatch(ranked) ? relatedPostBuilder.build(ranked) : List.of(),
        "blog_rag",
        null,
        usedSkills,
        List.of(),
        Map.of("ragTopScore", topScore(ranked))
    );
  }
}
