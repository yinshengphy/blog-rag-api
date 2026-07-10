package cn.yinsheng.blog.rag.model;

public record ImageAttachment(
    String mimeType,
    String data,
    String name
) {
}
