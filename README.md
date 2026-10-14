# 遗韵 · 非遗文化智能创作平台

> 基于 RAG 的非遗文化智能问答与 AI 创作平台，让传统文化在数字时代焕发新生。

「遗韵」致力于将非物质文化遗产与人工智能技术相结合，通过 **RAG（检索增强生成）** 技术构建非遗知识库，为用户提供精准的非遗文化问答，并支持 AI 辅助的文化内容创作。

---

## 目录

- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [API 接口](#api-接口)
- [数据库设计](#数据库设计)
- [RAG 检索流程](#rag-检索流程)
- [开发进度与计划](#开发进度与计划)

---

## 功能特性

### 已实现 ✅

- **非遗内容展示**：非遗分类浏览、项目列表分页查询、项目详情（含知识文档）
- **RAG 智能问答**：基于非遗知识库的语义检索 + LLM 生成回答，回答附带引用来源
- **限定范围检索**：支持按非遗项目 ID 限定检索范围，提升回答精准度
- **多模型支持**：LLM 支持本地 Ollama（免费）与 DeepSeek（云端），Embedding 使用 BGE-M3
- **任务记录**：AI 对话任务自动记录，可查询历史任务
- **用户认证**：JWT 无状态认证，BCrypt 密码加密
- **作品管理**：用户作品的列表、详情、删除（为后续 AI 创作预留）

### 计划中 🚧

- **AI 图像生成**：基于非遗元素的 AI 绘图（P0 阶段）
- **AI 视频生成**：非遗文化短视频生成（P0.5 阶段）
- **Prompt 智能增强**：根据非遗知识库自动优化创作 Prompt
- **知识库管理后台**：知识文档的上传、编辑、索引重建
- **作品来源溯源**：AI 作品关联 RAG 检索到的知识来源
- **操作日志审计**：系统操作日志记录与查询

---

## 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| **前端** | Vue 3 + Vite + Element Plus + Pinia + Vue Router | 单页应用 |
| **后端** | Java 17 + Spring Boot 3.5 + Spring Security + JWT + MyBatis-Plus | RESTful API |
| **AI 服务** | Python 3.12 + FastAPI + FAISS + Sentence-Transformers | RAG 检索增强生成 |
| **数据库** | MySQL 8.0 + Redis 7 | 数据存储与缓存 |
| **部署** | Docker Compose + Nginx | 容器化编排 |

### AI 相关依赖

- **Embedding 模型**：[BAAI/bge-m3](https://huggingface.co/BAAI/bge-m3)（本地加载，支持多语言）
- **向量数据库**：FAISS（IndexFlatIP，内积相似度，向量已归一化等价余弦）
- **LLM**：
  - 本地 Ollama（默认 `deepseek-r1:8b`，免费无需 API Key）
  - 云端 DeepSeek（`deepseek-chat`，需配置 API Key）

---

## 项目结构

```
Yi-yun/
├── frontend/                 # 前端（Vue 3）
│   ├── src/
│   │   ├── components/       # 组件
│   │   ├── assets/           # 静态资源
│   │   ├── App.vue
│   │   └── main.js
│   ├── package.json
│   └── vite.config.js
├── backend/                  # 后端（Spring Boot）
│   └── src/main/java/com/yiyun/
│       ├── controller/       # 控制器（Auth / Heritage / AI / Work）
│       ├── service/          # 业务逻辑
│       ├── mapper/           # MyBatis-Plus Mapper
│       ├── entity/           # 实体类
│       ├── dto/              # 数据传输对象
│       ├── config/           # 配置（Security / CORS / MyBatis-Plus）
│       ├── security/         # JWT 认证
│       ├── common/           # 统一响应 / 异常处理
│       └── util/             # 工具类
├── ai-service/               # AI 服务（FastAPI + RAG）
│   └── app/
│       ├── api/              # 接口路由（chat）
│       ├── core/             # 核心配置与 LLM 调用
│       │   ├── config.py     # 配置管理（从 .env 加载）
│       │   └── llm.py        # LLM 封装（Ollama / DeepSeek）
│       ├── rag/              # RAG 检索增强
│       │   ├── chunker.py    # 文本切分
│       │   ├── embedder.py   # BGE-M3 向量化
│       │   ├── vector_store.py # FAISS 向量库
│       │   └── retriever.py  # 检索核心（构建索引 + 语义检索）
│       ├── db/               # MySQL 数据访问
│       ├── schemas.py        # Pydantic 请求/响应模型
│       └── main.py           # FastAPI 入口
├── deploy/                   # 部署配置
│   ├── docker-compose.yml    # MySQL + Redis
│   ├── mysql/init/yiyun.sql  # 数据库初始化脚本
│   └── niginx/nginx.conf     # Nginx 配置
└── README.md
```

---

## 环境要求

| 软件 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 17+ | 后端运行环境 |
| Maven | 3.8+ | 后端构建工具 |
| Node.js | 18+ | 前端运行环境 |
| pnpm | 8+ | 前端包管理器 |
| Python | 3.10+ | AI 服务运行环境 |
| Docker | 20+ | 容器化部署（可选） |
| MySQL | 8.0 | 数据库 |
| Redis | 7+ | 缓存 |
| Ollama | 最新 | 本地 LLM（可选，可用 DeepSeek 替代） |

---

## 快速开始

### 1. 启动基础设施（MySQL + Redis）

```bash
cd deploy
docker-compose up -d
```

此命令会启动：
- **MySQL 8.0**：端口 `3307`，自动执行 `yiyun.sql` 初始化数据库
- **Redis 7**：端口 `6379`

默认数据库账号：`yiyun / yiyun123`，数据库名 `yiyun`。

初始化数据包含：
- 默认用户：`admin / 123456`、`test / 123456`
- 7 个非遗分类（传统美术、传统技艺、传统音乐等）
- 示例非遗项目「苗族银饰」及 3 篇知识文档

### 2. 启动后端服务

```bash
cd backend
mvn spring-boot:run
```

后端启动在 `http://localhost:8080`。

### 3. 启动 AI 服务

```bash
cd ai-service
python -m venv .venv
.venv\Scripts\activate        # Windows
# source .venv/bin/activate   # Linux/Mac

pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

AI 服务启动在 `http://localhost:8000`。

> ⚠️ 首次启动时，若 FAISS 索引不存在，会自动从 MySQL 读取知识文档并构建索引（需下载 BGE-M3 模型，建议提前配置本地模型路径，见 [配置说明](#配置说明)）。

### 4. 启动前端

```bash
cd frontend
pnpm install
pnpm dev
```

前端启动在 `http://localhost:5173`。

### 5. 验证服务

```bash
# 后端健康检查
curl http://localhost:8080/api/heritage/categories

# AI 服务健康检查
curl http://localhost:8000/health
```

---

## 配置说明

### AI 服务配置（`ai-service/.env`）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `SERVICE_HOST` | `0.0.0.0` | 服务监听地址 |
| `SERVICE_PORT` | `8000` | 服务端口 |
| `LLM_PROVIDER` | `ollama` | LLM 提供商：`ollama` / `deepseek` |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama 服务地址 |
| `OLLAMA_MODEL` | `qwen2.5:7b` | Ollama 模型名 |
| `DEEPSEEK_API_KEY` | - | DeepSeek API Key |
| `DEEPSEEK_BASE_URL` | `https://api.deepseek.com/v1` | DeepSeek 接口地址 |
| `DEEPSEEK_MODEL` | `deepseek-chat` | DeepSeek 模型名 |
| `EMBEDDING_MODEL` | `BAAI/bge-m3` | Embedding 模型（本地路径或 HuggingFace 名） |
| `FAISS_INDEX_PATH` | `./data/faiss_index` | FAISS 索引存储路径 |
| `FAISS_TOP_K` | `5` | 检索返回的最相似片段数 |
| `CHUNK_SIZE` | `400` | 文本切分单片段目标字数 |
| `CHUNK_OVERLAP_RATIO` | `0.15` | 相邻片段重叠比例 |

> 💡 切换到 DeepSeek：将 `LLM_PROVIDER` 改为 `deepseek` 并填写 `DEEPSEEK_API_KEY`。
> 💡 Embedding 模型本地加载：将 `EMBEDDING_MODEL` 设为本地模型目录路径（如 ModelScope 缓存路径）。

### 后端配置（`backend/src/main/resources/application.yml`）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | `8080` | 后端端口 |
| `spring.datasource.url` | `jdbc:mysql://localhost:3307/yiyun` | MySQL 连接 |
| `spring.data.redis.host` | `localhost` | Redis 地址 |
| `jwt.secret` | - | JWT 签名密钥 |
| `jwt.expiration` | `86400000` | Token 有效期（24 小时） |
| `ai-service.base-url` | `http://localhost:8000` | AI 服务地址 |

---

## API 接口

### 公开接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 用户登录，返回 JWT Token |
| GET | `/api/heritage/categories` | 获取所有非遗分类 |
| GET | `/api/heritage/projects` | 分页查询非遗项目（支持 categoryId、region 筛选） |
| GET | `/api/heritage/projects/{id}` | 获取非遗项目详情（含知识文档） |

### 需认证接口（Header 携带 `Authorization: Bearer {token}`）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ai/chat` | AI 文本对话（RAG 检索 + LLM 生成） |
| GET | `/api/ai/tasks` | 分页查询当前用户的 AI 任务 |
| GET | `/api/ai/tasks/{id}` | 查询 AI 任务详情 |
| GET | `/api/works` | 分页查询当前用户作品列表 |
| GET | `/api/works/{id}` | 查询作品详情（含 RAG 来源） |
| DELETE | `/api/works/{id}` | 删除作品（逻辑删除） |

### AI 服务内部接口（由后端调用）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/health` | 健康检查 |
| POST | `/api/ai/chat` | AI 对话（返回回答 + 来源片段） |
| POST | `/api/ai/index/rebuild` | 全量重建 FAISS 向量索引 |

### 请求/响应示例

**登录**

```json
// POST /api/auth/login
{
  "username": "admin",
  "password": "123456"
}
```

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": { "id": 1, "username": "admin", "nickname": "管理员" }
  }
}
```

**AI 对话**

```json
// POST /api/ai/chat
{
  "message": "苗族银饰有什么文化内涵？",
  "heritage_project_id": 1,
  "conversation_history": [
    { "role": "user", "content": "你好" },
    { "role": "assistant", "content": "你好！我是遗韵非遗文化助手。" }
  ]
}
```

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "reply": "苗族银饰不仅具有装饰作用，还与民族文化、社会身份和传统习俗存在联系...",
    "sources": [
      {
        "chunk_id": 1,
        "document_id": 1,
        "heritage_project_id": 1,
        "title": "苗族银饰文化内涵",
        "doc_type": "文化内涵",
        "source": "苗族传统文化资料",
        "content": "苗族银饰不仅具有装饰作用...",
        "score": 0.85
      }
    ],
    "taskId": 100
  }
}
```

---

## 数据库设计

### 核心表结构

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `sys_user` | 系统用户 | username, password(BCrypt), nickname, status |
| `heritage_category` | 非遗分类 | name, description, sort_num |
| `heritage_project` | 非遗项目 | name, category_id, region, description, history, cultural_meaning, craft |
| `knowledge_document` | 知识文档 | heritage_project_id, title, doc_type, content, source |
| `knowledge_chunk` | RAG 知识片段 | document_id, heritage_project_id, chunk_index, chunk_text, embedding_id |
| `ai_task` | AI 生成任务 | user_id, task_type(CHAT/IMAGE/VIDEO), status, input_text, result_url |
| `biz_user_work` | 用户 AI 作品 | user_id, work_type, title, prompt, result_url, task_id |
| `biz_work_source` | 作品知识来源 | work_id, heritage_project_id, source_content, retrieval_score |
| `sys_operation_log` | 操作日志 | user_id, module, operation, request_url, execution_time |

### ER 关系概览

```
sys_user ──< ai_task ──< biz_user_work ──< biz_work_source
     │                                            │
     └────────────────────────────────────────────┘

heritage_category ──< heritage_project ──< knowledge_document ──< knowledge_chunk
```

---

## RAG 检索流程

```
用户提问
   │
   ▼
embed_query()  ── BGE-M3 向量化查询文本
   │
   ▼
vector_store.search()  ── FAISS 内积相似度检索 Top-K
   │
   ▼
fetch_chunks_by_ids()  ── 回查 MySQL 获取片段内容及来源文档
   │
   ▼
generate_reply()  ── 拼接上下文 + 对话历史 → LLM 生成回答
   │
   ▼
返回回答 + 引用来源
```

### 索引构建流程

1. `build_index()` 读取所有 `knowledge_document`
2. `split_text_into_chunks()` 按段落 + 滑动窗口切分为知识片段
3. 批量写入 `knowledge_chunk` 表
4. `embed_texts()` 使用 BGE-M3 生成向量
5. `vector_store.build()` 构建 FAISS 索引并持久化到磁盘
6. 更新 `knowledge_chunk.embedding_id`

---

## 开发进度与计划

### 当前进度

| 模块 | 状态 | 说明 |
|------|------|------|
| 项目骨架（前端/后端/AI 服务） | ✅ 完成 | 三端架构搭建完成 |
| 用户认证（JWT + Spring Security） | ✅ 完成 | 登录、密码加密、Token 校验 |
| 非遗内容展示 | ✅ 完成 | 分类、项目列表、详情 |
| RAG 检索核心 | ✅ 完成 | 切分、向量化、FAISS 索引、语义检索 |
| AI 对话接口 | ✅ 完成 | 端到端打通：前端→后端→AI 服务 |
| 任务记录 | ✅ 完成 | AI 对话任务自动记录 |
| 作品管理 | ✅ 完成 | 列表、详情、删除（逻辑删除） |
| 前端页面 | 🚧 进行中 | 基础脚手架已搭建，业务页面待开发 |
| AI 图像/视频生成 | ⏳ 未开始 | 依赖图像/视频生成服务 |
| 知识库管理后台 | ⏳ 未开始 | 文档上传与索引重建界面 |

### 后续计划

**P0 - 核心闭环**
- [ ] 前端非遗展示页面（首页、分类、项目详情）
- [ ] 前端 AI 对话页面（含来源展示）
- [ ] AI 图像生成接入（非遗元素绘图）
- [ ] Prompt 智能增强（RAG + 创作 Prompt 融合）
- [ ] 作品生成流程打通（输入 → AI 生成 → 保存 → 来源溯源）

**P0.5 - 体验提升**
- [ ] AI 视频生成接入
- [ ] 知识库管理后台（文档 CRUD + 索引重建按钮）
- [ ] 操作日志审计接口
- [ ] 对话流式输出（SSE）

**P1 - 平台完善**
- [ ] 用户注册 / 个人中心
- [ ] 作品分享与社区
- [ ] 非遗项目贡献机制
- [ ] 管理后台（用户、内容、审核）

---

## 许可证

本项目仅供学习与非遗文化传播使用。