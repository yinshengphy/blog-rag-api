package cn.yinsheng.blog.rag.rag;

import cn.yinsheng.blog.rag.model.RelatedPost;
import cn.yinsheng.blog.rag.model.RetrievedChunk;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RelatedPostBuilder {
  public List<RelatedPost> build(List<RetrievedChunk> chunks) {
    Map<String, RelatedPost> posts = new LinkedHashMap<>();
    for (RetrievedChunk chunk : chunks) {
      posts.putIfAbsent(chunk.slug(), new RelatedPost(chunk.title(), "/" + chunk.slug() + "/"));
    }
    return new ArrayList<>(posts.values());
  }
}
