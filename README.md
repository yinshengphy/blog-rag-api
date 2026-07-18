# blog-rag-api

`yinsheng的小站` 的 Spring Boot 智能助手 API。

## 职责

- `POST /api/chat`：返回 JSON 格式回答。
- `POST /api/chat/stream`：供博客聊天组件使用的 SSE 流式回答。
- 由纯文本模型直接回答普通问题，或按意图调用三个公开工具。
- `blog_qa` 支持全站、当前文章和指定文章问答。
- `blog_summary` 按顺序读取整篇文章并执行分层摘要。
- `weather` 返回真实天气数据。
- 返回回答、引用、相关文章和工具使用元数据。
- 博客发布后通过内部接口增量索引新增、修改和删除的 Markdown 文章。
- 提供受令牌保护的全量重建接口，不运行定时索引任务。

## 主要环境变量

- `AI_COMPUTE_BASE_URL`：内部 AI 计算网关地址。
- `AI_COMPUTE_API_TOKEN`：内部服务调用令牌。
- `RAG_CHAT_MODEL`：默认值 `huihui-qwen3:4b-instruct-2507-abliterated-q4_K_M`。
- `RAG_EMBEDDING_MODEL`：默认值 `bge-m3`。
- `QDRANT_URL`：Qdrant 地址。
- `QDRANT_COLLECTION`：默认值 `blog_chunks`。
- `BLOG_CONTENT_DIR`：索引器读取的 Markdown 源目录。
- `INDEX_DB_PATH`：SQLite 索引状态数据库路径。
- `INDEX_API_TOKEN`：调用内部索引接口的专用令牌。

## 本地构建

```bash
mvn -B -DskipTests package
```

## 本地运行

```bash
export AI_COMPUTE_BASE_URL=http://127.0.0.1:8081
export QDRANT_URL=http://127.0.0.1:6333
mvn spring-boot:run
```

## 索引接口

增量同步：

```bash
curl -X POST \
  -H "Authorization: Bearer ${INDEX_API_TOKEN}" \
  http://127.0.0.1:8080/internal/index/sync
```

全量重建：

```bash
curl -X POST \
  -H "Authorization: Bearer ${INDEX_API_TOKEN}" \
  http://127.0.0.1:8080/internal/index/rebuild
```
