package cn.yinsheng.blog.service.model;

import java.util.List;

public record ChunkRecord(
    String chunkId,
    int chunkIndex,
    String slug,
    String title,
    String section,
    String headingPath,
    String url,
    String content,
    String contentHash,
    String chunkHash,
    List<String> tags,
    List<String> categories,
    String description,
    String date,
    String updatedAt
) {
  public String retrievalText() {
    return """
        文章标题：%s
        小节路径：%s
        分类：%s
        标签：%s
        摘要：%s
        正文片段：
        %s
        """.formatted(
            title,
            headingPath,
            String.join(", ", categories),
            String.join(", ", tags),
            description,
            content
        );
  }

  public ChunkRecord(
      String chunkId,
      int chunkIndex,
      String slug,
      String title,
      String section,
      String headingPath,
      String url,
      String content,
      String contentHash,
      String chunkHash,
      List<String> tags,
      String date,
      String updatedAt
  ) {
    this(
        chunkId, chunkIndex, slug, title, section, headingPath, url, content,
        contentHash, chunkHash, tags, List.of(), "", date, updatedAt
    );
  }
}
