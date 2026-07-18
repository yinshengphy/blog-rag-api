package cn.yinsheng.blog.service.indexer;

import java.util.List;

public record IndexResult(
    IndexMode mode,
    String status,
    int totalPosts,
    int indexedPosts,
    int skippedPosts,
    int deletedPosts,
    List<String> indexedSlugs,
    String message
) {
  public boolean completed() {
    return "COMPLETED".equals(status);
  }
}

