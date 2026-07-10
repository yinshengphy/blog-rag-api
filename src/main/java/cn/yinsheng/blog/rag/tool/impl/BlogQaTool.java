package cn.yinsheng.blog.rag.tool.impl;

import cn.yinsheng.blog.rag.model.Citation;
import cn.yinsheng.blog.rag.model.PageContext;
import cn.yinsheng.blog.rag.model.RelatedPost;
import cn.yinsheng.blog.rag.model.RetrievedChunk;
import cn.yinsheng.blog.rag.rag.BlogCatalogService;
import cn.yinsheng.blog.rag.rag.BlogRetriever;
import cn.yinsheng.blog.rag.rag.CitationBuilder;
import cn.yinsheng.blog.rag.rag.RelatedPostBuilder;
import cn.yinsheng.blog.rag.tool.ToolCall;
import cn.yinsheng.blog.rag.tool.ToolDefinition;
import cn.yinsheng.blog.rag.tool.ToolExecutionContext;
import cn.yinsheng.blog.rag.tool.ToolRegistry;
import cn.yinsheng.blog.rag.tool.ToolResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class BlogQaTool implements ToolRegistry.ToolHandler {
  private final BlogRetriever retriever;
  private final BlogCatalogService catalog;
  private final CitationBuilder citationBuilder;
  private final RelatedPostBuilder relatedPostBuilder;

  public BlogQaTool(BlogRetriever retriever, BlogCatalogService catalog, CitationBuilder citationBuilder, RelatedPostBuilder relatedPostBuilder) {
    this.retriever = retriever;
    this.catalog = catalog;
    this.citationBuilder = citationBuilder;
    this.relatedPostBuilder = relatedPostBuilder;
  }

  @Override
  public ToolDefinition definition() {
    return new ToolDefinition("blog_qa", "Search and answer from this site's blog. Use CURRENT_POST for references to this/current article, SPECIFIED_POST for a named article, and ALL_POSTS for site-wide questions.", Map.of(
        "type", "object",
        "properties", Map.of(
            "query", Map.of("type", "string"),
            "scope", Map.of("type", "string", "enum", List.of("CURRENT_POST", "SPECIFIED_POST", "ALL_POSTS")),
            "target", Map.of("type", "string")
        ),
        "required", List.of("query", "scope")
    ));
  }

  @Override
  public ToolResult execute(ToolCall call, ToolExecutionContext context) {
    String query = stringArg(call, "query");
    String scope = stringArg(call, "scope");
    String slug = resolveSlug(scope, stringArg(call, "target"), context.pageContext());
    if ("__missing_current__".equals(slug)) {
      return ToolResult.failure(call, "No current blog post is available. Ask the user to name an article.");
    }
    if (slug != null && slug.startsWith("__candidates__")) {
      return ToolResult.failure(call, "The article name is ambiguous. Candidates: " + slug.substring("__candidates__".length()));
    }
    if ("__not_found__".equals(slug)) {
      return ToolResult.failure(call, "The requested blog post was not found.");
    }
    List<RetrievedChunk> chunks = retriever.retrieve(query, slug);
    if (chunks.isEmpty() || chunks.get(0).score() < 0.30) {
      return ToolResult.failure(call, "No sufficiently relevant blog content was found.");
    }
    List<Citation> citations = citationBuilder.build(chunks, "");
    List<RelatedPost> related = relatedPostBuilder.build(chunks);
    StringBuilder content = new StringBuilder();
    for (int i = 0; i < chunks.size(); i++) {
      RetrievedChunk chunk = chunks.get(i);
      content.append("[Source ").append(i + 1).append("]\n")
          .append("Title: ").append(chunk.title()).append('\n')
          .append("Section: ").append(chunk.section()).append('\n')
          .append("URL: ").append(chunk.url()).append('\n')
          .append("Content: ").append(chunk.content()).append("\n\n");
    }
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("ragTopScore", chunks.get(0).score());
    metadata.put("scope", scope);
    if (slug != null) metadata.put("slug", slug);
    return ToolResult.success(call, content.toString(), citations, related, metadata);
  }

  private String resolveSlug(String scope, String target, PageContext pageContext) {
    if ("ALL_POSTS".equalsIgnoreCase(scope)) return null;
    if ("CURRENT_POST".equalsIgnoreCase(scope)) {
      return pageContext != null && pageContext.isBlogPost() ? pageContext.slug() : "__missing_current__";
    }
    BlogCatalogService.Resolution resolution = catalog.resolve(target);
    if (resolution.found()) return resolution.post().slug();
    if (resolution.ambiguous()) {
      return "__candidates__" + resolution.candidates().stream().map(item -> item.title() + " (" + item.slug() + ")").toList();
    }
    return "__not_found__";
  }

  private String stringArg(ToolCall call, String name) {
    Object value = call.arguments().get(name);
    return value == null ? "" : String.valueOf(value).trim();
  }
}
