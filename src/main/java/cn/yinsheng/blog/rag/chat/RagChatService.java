package cn.yinsheng.blog.rag.chat;

import cn.yinsheng.blog.rag.model.ChatResponse;
import cn.yinsheng.blog.rag.rag.BlogRagService;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

@Service
public class RagChatService {
  private final BlogRagService blogRagService;

  public RagChatService(BlogRagService blogRagService) {
    this.blogRagService = blogRagService;
  }

  public ChatResponse answer(String question) {
    return blogRagService.answer(question);
  }

  public ChatResponse streamAnswer(String question, Consumer<String> deltaConsumer) {
    return blogRagService.streamAnswer(question, deltaConsumer);
  }
}
