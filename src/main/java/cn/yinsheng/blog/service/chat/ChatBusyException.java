package cn.yinsheng.blog.service.chat;

public class ChatBusyException extends RuntimeException {
  public ChatBusyException(String message) {
    super(message);
  }
}
