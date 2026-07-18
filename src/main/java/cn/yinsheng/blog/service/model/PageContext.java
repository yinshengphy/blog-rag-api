package cn.yinsheng.blog.service.model;

public record PageContext(
    String pageType,
    String slug,
    String title,
    String url,
    String heading
) {
  public boolean isBlogPost() {
    return "BLOG_POST".equalsIgnoreCase(pageType) && slug != null && !slug.isBlank();
  }
}
