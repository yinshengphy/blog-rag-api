package cn.yinsheng.blog.rag.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.yinsheng.blog.rag.config.RagProperties;
import cn.yinsheng.blog.rag.indexer.BlogIndexService;
import cn.yinsheng.blog.rag.indexer.IndexMode;
import cn.yinsheng.blog.rag.indexer.IndexResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class InternalIndexControllerTest {

  @Test
  void 未授权请求不能触发索引() {
    BlogIndexService service = mock(BlogIndexService.class);
    RagProperties properties = mock(RagProperties.class);
    when(properties.indexApiToken()).thenReturn("secret");
    InternalIndexController controller = new InternalIndexController(service, properties);

    assertThat(controller.synchronize("Bearer wrong").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void 正确令牌可以触发全量重建() {
    BlogIndexService service = mock(BlogIndexService.class);
    RagProperties properties = mock(RagProperties.class);
    when(properties.indexApiToken()).thenReturn("secret");
    when(service.rebuild()).thenReturn(new IndexResult(
        IndexMode.REBUILD, "COMPLETED", 2, 2, 0, 0, List.of("a", "b"), "完成"
    ));
    InternalIndexController controller = new InternalIndexController(service, properties);

    assertThat(controller.rebuild("Bearer secret").getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(service).rebuild();
  }
}

