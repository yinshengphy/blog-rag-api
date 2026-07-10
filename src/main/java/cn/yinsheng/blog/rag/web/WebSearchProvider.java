package cn.yinsheng.blog.rag.web;

import java.util.List;

public interface WebSearchProvider {
  List<WebSearchResult> search(String query, String engine, int page);
}
