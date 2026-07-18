package cn.yinsheng.blog.service.model;

import java.util.List;

public record RetrievedChunk(
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
}
