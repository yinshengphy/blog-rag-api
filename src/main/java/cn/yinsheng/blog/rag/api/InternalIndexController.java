package cn.yinsheng.blog.rag.api;

import cn.yinsheng.blog.rag.config.RagProperties;
import cn.yinsheng.blog.rag.indexer.BlogIndexService;
import cn.yinsheng.blog.rag.indexer.IndexResult;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalIndexController {
  private final BlogIndexService indexService;
  private final RagProperties properties;

  public InternalIndexController(BlogIndexService indexService, RagProperties properties) {
    this.indexService = indexService;
    this.properties = properties;
  }

  @PostMapping("/internal/index/sync")
  public ResponseEntity<IndexResult> synchronize(
      @RequestHeader(value = "Authorization", required = false) String authorization
  ) {
    if (!authorized(authorization)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    return response(indexService.synchronize());
  }

  @PostMapping("/internal/index/rebuild")
  public ResponseEntity<IndexResult> rebuild(
      @RequestHeader(value = "Authorization", required = false) String authorization
  ) {
    if (!authorized(authorization)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    return response(indexService.rebuild());
  }

  private ResponseEntity<IndexResult> response(IndexResult result) {
    if (result.completed()) {
      return ResponseEntity.ok(result);
    }
    if ("BUSY".equals(result.status())) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
    }
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
  }

  private boolean authorized(String authorization) {
    String expected = properties.indexApiToken();
    if (expected == null || expected.isBlank() || authorization == null || !authorization.startsWith("Bearer ")) {
      return false;
    }
    byte[] actualBytes = authorization.substring(7).getBytes(StandardCharsets.UTF_8);
    byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(expectedBytes, actualBytes);
  }
}

