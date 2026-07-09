package cn.yinsheng.blog.rag.intent;

public interface IntentRouter {
  IntentResult route(String question);
}
