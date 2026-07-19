package cn.yinsheng.blog.service.model;

import java.util.List;

public record RetrievedChunk(
    double score,
    String chunkId,
    int chunkIndex,
    String slug,
    String title,
    String section,
    String headingPath,
    String url,
    String content,
    List<String> tags,
    List<String> categories,
    String description,
    String date,
    String updatedAt
) {
  public RetrievedChunk(
      double score,
      String chunkId,
      int chunkIndex,
      String slug,
      String title,
      String section,
      String url,
      String content,
      List<String> tags,
      String updatedAt
  ) {
    this(
        score, chunkId, chunkIndex, slug, title, section, title + " > " + section,
        url, content, tags, List.of(), "", "", updatedAt
    );
  }
}
