# 遗韵 · 非遗文化智能创作平台

<p align="center">
  <img src="./png/logo.png" alt="遗韵 Logo" width="120" />
</p>

> 基于 RAG 的非遗文化智能问答与 AI 创作平台，让传统文化在数字时代焕发新生。

「遗韵」致力于将非物质文化遗产与人工智能技术相结合，通过 **RAG（检索增强生成）** 技术构建非遗知识库，为用户提供精准的非遗文化问答，并支持 AI 辅助的文化内容创作（文本对话、图像生成）。

---

## 目录

- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [环境变量模板](#环境变量模板)
- [依赖清单](#依赖清单)
- [API 接口](#api-接口)
- [数据库设计](#数据库设计)
- [RAG 检索流程](#rag-检索流程)
- [团队信息](#团队信息)
- [开发进度与计划](#开发进度与计划)
- [许可证](#许可证)

---

## 功能特性

### 已实现 ✅

- **非遗内容展示**：非遗分类浏览、项目列表分页查询（支持分类/地域筛选）、项目详情（含知识文档）
- **RAG 智能问答**：基于非遗知识库的语义检索 + LLM 生成回答，回答附带引用来源
- **限定范围检索**：支持按非遗项目 ID 限定检索范围，提升回答精准度
- **流式对话（SSE）**：AI 回答支持流式输出，逐字展示，提升交互体验
- **对话会话管理**：支持多会话切换、历史消息回显、会话删除
- **多模型支持**：LLM 支持本地 Ollama（免费）、云端 DeepSeek、OpenAI 兼容网关（如 WaLiAPI）；Embedding 支持本地 BGE-M3 与在线 DashScope API
- **AI 图像生成**：基于 DashScope Qwen-Image 2.0 的文生图，支持多种风格（水墨/油画/水彩/国画/写实）与画幅比例（1:1 / 16:9 / 9:16 / 4:3 / 3:4）
- **任务记录**：AI 对话与图像生成任务自动记录，可查询历史任务
- **用户认证**：JWT 无状态认证，BCrypt 密码加密，支持注册/登录/改密
- **作品管理**：用户作品的保存、列表、详情、删除（含 RAG 来源溯源）

### 计划中 🚧

- **AI 视频生成**：非遗文化短视频生成（P0.5 阶段）
- **Prompt 智能增强**：根据非遗知识库自动优化创作 Prompt
- **知识库管理后台**：知识文档的上传、编辑、索引重建
- **操作日志审计**：系统操作日志记录与查询界面

---

## 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| **前端** | Vue 3 + Vite 8 + Element Plus + Pinia + Vue Router 5 | 单页应用 |
| **后端** | Java 17 + Spring Boot 3.5 + Spring Security + JWT + MyBatis-Plus | RESTful API |
| **AI 服务** | Python 3.12 + FastAPI + FAISS + Sentence-Transformers | RAG 检索增强生成 |
| **数据库** | MySQL 8.0 + Redis 7 | 数据存储与缓存 |
| **部署** | Docker Compose + Nginx | 容器化编排 |

### AI 相关依赖

- **Embedding 模型**：[BAAI/bge-m3](https://huggingface.co/BAAI/bge-m3)（本地加载，支持多语言）或 DashScope `text-embedding-v3`（在线 API）
- **向量数据库**：FAISS（IndexFlatIP，内积相似度，向量已归一化等价余弦）
- **LLM**：
  - 本地 Ollama（默认 `qwen2.5:7b`，免费无需 API Key）
  - 云端 DeepSeek（`deepseek-chat`，需配置 API Key）
  - OpenAI 兼容网关（如 WaLiAPI，`kilo-auto/free`，需配置 API Key）
- **图像生成**：阿里云百炼 DashScope Qwen-Image 2.0（需配置 API Key）

---

## 项目结构

```
Yi-yun/
├── frontend/                 # 前端（Vue 3）
│   ├── src/
│   │   ├── api/              # 接口封装（auth / heritage / ai / works）
│   │   ├── components/       # 通用组件
│   │   ├── layouts/          # 布局组件（AuthLayout / MainLayout）
│   │   ├── stores/           # Pinia 状态管理
│   │   ├── views/            # 页面视图（Home / Gallery / AiGuide / Create / Works / Profile）
│   │   ├── router/           # 路由配置
│   │   ├── App.vue
│   │   └── main.js
│   ├── package.json
│   └── vite.config.js
├── backend/                  # 后端（Spring Boot）
│   └── src/main/java/com/yiyun/
│       ├── controller/       # 控制器（Auth / Heritage / AI / Work）
│       ├── service/          # 业务逻辑（含 AiServiceClient 调用 ai-service）
│       ├── mapper/           # MyBatis-Plus Mapper
│       ├── entity/           # 实体类
│       ├── dto/              # 数据传输对象
│       ├── config/           # 配置（Security / CORS / MyBatis-Plus / RestTemplate）
│       ├── security/         # JWT 认证
│       ├── common/           # 统一响应 / 异常处理
│       └── util/             # 工具类
├── ai-service/               # AI 服务（FastAPI + RAG）
│   └── app/
│       ├── api/              # 接口路由（chat / image）
│       ├── core/             # 核心配置与 LLM 调用
│       │   ├── config.py     # 配置管理（从 .env 加载）
│       │   ├── llm.py        # LLM 封装（Ollama / DeepSeek / OpenAI 兼容）
│       │   └── auth.py       # JWT 认证（与后端共享 secret）
│       ├── rag/              # RAG 检索增强
│       │   ├── chunker.py    # 文本切分
│       │   ├── embedder.py   # BGE-M3 / DashScope 向量化
│       │   ├── vector_store.py # FAISS 向量库
│       │   └── retriever.py  # 检索核心（构建索引 + 语义检索）
│       ├── services/         # 多模态生成服务
│       │   └── image_gen.py  # DashScope Qwen-Image 文生图
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
| Ollama | 最新 | 本地 LLM（可选，可用 DeepSeek / OpenAI 兼容网关替代） |

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
- 示例非遗项目「苗族银饰锻制技艺」及 1 篇知识文档

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

前端启动在 `http://localhost:5173`，Vite 已配置代理将 `/api` 和 `/images` 转发至后端 `8080`。

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
| `MYSQL_HOST` | `localhost` | MySQL 地址 |
| `MYSQL_PORT` | `3307` | MySQL 端口 |
| `MYSQL_USER` | `root` | MySQL 用户名 |
| `MYSQL_PASSWORD` | `root` | MySQL 密码 |
| `MYSQL_DATABASE` | `yiyun` | MySQL 数据库名 |
| `JWT_SECRET` | - | JWT 签名密钥（须与后端一致） |
| `JWT_ALGORITHM` | `HS256` | JWT 算法 |
| `LLM_PROVIDER` | `ollama` | LLM 提供商：`ollama` / `deepseek` / `openai` |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama 服务地址 |
| `OLLAMA_MODEL` | `qwen2.5:7b` | Ollama 模型名 |
| `DEEPSEEK_API_KEY` | - | DeepSeek API Key |
| `DEEPSEEK_BASE_URL` | `https://api.deepseek.com/v1` | DeepSeek 接口地址 |
| `DEEPSEEK_MODEL` | `deepseek-chat` | DeepSeek 模型名 |
| `OPENAI_API_KEY` | - | OpenAI 兼容网关 API Key |
| `OPENAI_BASE_URL` | `http://127.0.0.1:8777/v1` | OpenAI 兼容网关地址 |
| `OPENAI_MODEL` | `kilo-auto/free` | OpenAI 兼容模型名 |
| `EMBEDDING_PROVIDER` | `local` | Embedding 提供商：`local` / `api` |
| `EMBEDDING_MODEL` | `BAAI/bge-m3` | Embedding 模型（本地路径或 HuggingFace 名 / DashScope 模型名） |
| `FAISS_INDEX_PATH` | `./data/faiss_index` | FAISS 索引存储路径 |
| `FAISS_TOP_K` | `5` | 检索返回的最相似片段数 |
| `CHUNK_SIZE` | `400` | 文本切分单片段目标字数 |
| `CHUNK_OVERLAP_RATIO` | `0.15` | 相邻片段重叠比例 |
| `IMAGE_PROVIDER` | - | 图像生成提供商（目前支持 `dashscope`） |
| `DASHSCOPE_API_KEY` | - | 阿里云百炼 DashScope API Key |
| `IMAGE_MODEL` | `qwen-image-2.0` | 图像生成模型名 |
| `DASHSCOPE_WORKSPACE_ID` | - | 百炼业务空间 ID（可选，用于构建专属域名） |
| `DASHSCOPE_REGION` | `cn-beijing` | 百炼区域 |

> 💡 切换 LLM 提供商：将 `LLM_PROVIDER` 改为对应值并填写所需 API Key。
> 💡 Embedding 模型本地加载：将 `EMBEDDING_PROVIDER` 设为 `local`，`EMBEDDING_MODEL` 设为本地模型目录路径（如 ModelScope 缓存路径）。
> 💡 启用图像生成：将 `IMAGE_PROVIDER` 设为 `dashscope` 并填写 `DASHSCOPE_API_KEY`。

### 后端配置（`backend/src/main/resources/application.yml`）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | `8080` | 后端端口 |
| `spring.datasource.url` | `jdbc:mysql://localhost:3307/yiyun` | MySQL 连接 |
| `spring.datasource.username` | `yiyun` | MySQL 用户名 |
| `spring.datasource.password` | `yiyun123` | MySQL 密码 |
| `spring.data.redis.host` | `localhost` | Redis 地址 |
| `spring.data.redis.port` | `6379` | Redis 端口 |
| `jwt.secret` | - | JWT 签名密钥（须与 ai-service 一致） |
| `jwt.expiration` | `86400000` | Token 有效期（24 小时） |
| `ai-service.base-url` | `http://localhost:8000` | AI 服务地址 |
| `ai-service.connect-timeout` | `5000` | 连接 AI 服务超时（ms） |
| `ai-service.read-timeout` | `120000` | 读取 AI 服务超时（ms） |
| `app.upload.image-dir` | `./uploads/images` | 上传图片存储目录 |

---

## 环境变量模板

### AI 服务 `.env` 模板

复制 `ai-service/.env` 并按需填写。最小可运行配置如下：

```bash
# ==================== 服务配置 ====================
SERVICE_HOST=0.0.0.0
SERVICE_PORT=8000

# ==================== MySQL 配置 ====================
MYSQL_HOST=localhost
MYSQL_PORT=3307
MYSQL_USER=yiyun
MYSQL_PASSWORD=yiyun123
MYSQL_DATABASE=yiyun

# ==================== JWT 认证配置（与后端 application.yml 中 jwt.secret 保持一致） ====================
JWT_SECRET=your-jwt-secret-must-be-long-enough
JWT_ALGORITHM=HS256

# ==================== LLM 配置（三选一） ====================
# 方式一：本地 Ollama（免费，无需 API Key）
LLM_PROVIDER=ollama
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=qwen2.5:7b

# 方式二：DeepSeek（需 API Key）
# LLM_PROVIDER=deepseek
# DEEPSEEK_API_KEY=your_deepseek_api_key
# DEEPSEEK_BASE_URL=https://api.deepseek.com/v1
# DEEPSEEK_MODEL=deepseek-chat

# 方式三：OpenAI 兼容网关（如 WaLiAPI，需 API Key）
# LLM_PROVIDER=openai
# OPENAI_API_KEY=your_openai_compatible_api_key
# OPENAI_BASE_URL=http://127.0.0.1:8777/v1
# OPENAI_MODEL=kilo-auto/free

# ==================== Embedding 配置 ====================
# 本地 BGE-M3（推荐，需提前下载模型）
EMBEDDING_PROVIDER=local
EMBEDDING_MODEL=BAAI/bge-m3

# 在线 DashScope（无需下载模型）
# EMBEDDING_PROVIDER=api
# EMBEDDING_MODEL=text-embedding-v3

# ==================== FAISS 向量库配置 ====================
FAISS_INDEX_PATH=./data/faiss_index
FAISS_TOP_K=5

# ==================== 文本切分配置 ====================
CHUNK_SIZE=400
CHUNK_OVERLAP_RATIO=0.15

# ==================== 图像生成配置（DashScope Qwen-Image，可选） ====================
IMAGE_PROVIDER=dashscope
DASHSCOPE_API_KEY=your_dashscope_api_key
IMAGE_MODEL=qwen-image-2.0
DASHSCOPE_WORKSPACE_ID=
DASHSCOPE_REGION=cn-beijing
```

> ⚠️ `.env` 文件包含密钥，已被 `.gitignore` 忽略，切勿提交到仓库。

---

## 依赖清单

### 后端（Maven，`backend/pom.xml`）

| 依赖 | 版本 | 说明 |
|------|------|------|
| spring-boot-starter-web | 3.5.5 | Web 框架 |
| spring-boot-starter-security | 3.5.5 | 安全认证 |
| spring-boot-starter-validation | 3.5.5 | 参数校验 |
| spring-boot-starter-data-redis | 3.5.5 | Redis 客户端 |
| mysql-connector-j | (BOM) | MySQL 驱动 |
| mybatis-plus-spring-boot3-starter | 3.5.12 | ORM 框架 |
| mybatis-plus-jsqlparser | 3.5.12 | 分页插件依赖 |
| lombok | (BOM) | 代码简化 |
| jjwt-api / jjwt-impl / jjwt-jackson | 0.12.6 | JWT 处理 |

### 前端（pnpm，`frontend/package.json`）

| 依赖 | 版本 | 说明 |
|------|------|------|
| vue | ^3.5.42 | 前端框架 |
| vue-router | ^5.3.1 | 路由 |
| pinia | ^4.0.3 | 状态管理 |
| element-plus | ^2.14.7 | UI 组件库 |
| @element-plus/icons-vue | ^2.3.2 | Element Plus 图标 |
| axios | ^1.20.0 | HTTP 客户端 |
| vite | ^8.3.0 | 构建工具（dev） |
| @vitejs/plugin-vue | ^6.0.8 | Vue 插件（dev） |
| unplugin-auto-import | ^21.1.0 | 自动导入（dev） |
| unplugin-vue-components | ^32.1.0 | 组件自动注册（dev） |

### AI 服务（pip，`ai-service/requirements.txt`）

| 依赖 | 版本 | 说明 |
|------|------|------|
| fastapi | 0.142.2 | Web 框架 |
| uvicorn | 0.54.0 | ASGI 服务器 |
| pydantic | 2.13.5 | 数据校验 |
| faiss-cpu | 1.8.0.post1 | 向量检索 |
| sentence-transformers | 3.0.1 | 文本向量化（BGE-M3） |
| torch | 2.4.1 | PyTorch（sentence-transformers 依赖） |
| numpy | 1.26.4 | 数值计算 |
| pymysql | 1.1.1 | MySQL 驱动 |
| cryptography | 43.0.1 | 加密库 |
| python-dotenv | 1.0.1 | 环境变量加载 |
| httpx | 0.27.2 | 异步 HTTP 客户端（调用 LLM） |
| dashscope | 1.27.8 | 阿里云百炼 SDK（图像生成 / Embedding） |
| PyJWT | 2.9.0 | JWT 解析（ai-service 侧认证） |

---

## API 接口

### 公开接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 用户登录，返回 JWT Token |
| POST | `/api/auth/register` | 用户注册，返回 JWT Token |
| GET | `/api/heritage/categories` | 获取所有非遗分类 |
| GET | `/api/heritage/projects` | 分页查询非遗项目（支持 categoryId、region 筛选） |
| GET | `/api/heritage/projects/{id}` | 获取非遗项目详情（含知识文档） |

### 需认证接口（Header 携带 `Authorization: Bearer {token}`）

**用户认证**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/auth/me` | 获取当前登录用户信息 |
| PUT | `/api/auth/password` | 修改密码 |

**AI 对话**

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ai/chat` | AI 文本对话（RAG 检索 + LLM 生成） |
| POST | `/api/ai/chat/stream` | AI 文本对话（流式 SSE，逐块返回） |

**AI 对话会话管理**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ai/sessions` | 获取当前用户的会话列表 |
| GET | `/api/ai/sessions/{id}/messages` | 获取指定会话的消息列表 |
| DELETE | `/api/ai/sessions/{id}` | 删除会话（软删除） |

**AI 图像生成**

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ai/generate/image` | 提交图像生成任务（异步，返回任务 ID） |
| GET | `/api/ai/tasks` | 分页查询当前用户的 AI 任务 |
| GET | `/api/ai/tasks/{id}` | 查询 AI 任务详情 |

**作品管理**

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/works` | 保存 AI 生成的作品 |
| GET | `/api/works` | 分页查询当前用户作品列表 |
| GET | `/api/works/{id}` | 查询作品详情（含 RAG 来源） |
| DELETE | `/api/works/{id}` | 删除作品（逻辑删除） |

### AI 服务内部接口（由后端调用，需 JWT）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/health` | 健康检查 |
| POST | `/api/ai/chat` | AI 对话（返回回答 + 来源片段） |
| POST | `/api/ai/chat/stream` | AI 对话流式输出（SSE） |
| POST | `/api/ai/generate/image` | 提交文生图任务 |
| GET | `/api/ai/tasks/{task_id}` | 查询文生图任务状态 |
| GET | `/api/ai/sessions` | 会话列表 |
| GET | `/api/ai/sessions/{id}/messages` | 会话消息 |
| DELETE | `/api/ai/sessions/{id}` | 删除会话 |
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
  "session_id": null
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
        "title": "苗族银饰锻制技艺基础介绍",
        "doc_type": "基础介绍",
        "source": "中国非物质文化遗产网",
        "content": "苗族银饰不仅具有装饰作用...",
        "score": 0.85
      }
    ],
    "sessionId": 1
  }
}
```

**AI 图像生成**

```json
// POST /api/ai/generate/image
{
  "prompt": "苗族银饰凤冠，细节丰富",
  "style": "chinese-painting",
  "size": "1:1"
}
```

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 100,
    "taskType": "IMAGE",
    "status": "PENDING",
    "providerTaskId": "uuid..."
  }
}
```

---

## 数据库设计

### 核心表结构

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `sys_user` | 系统用户 | username, password(BCrypt), nickname, email, status |
| `heritage_category` | 非遗分类 | name, description, sort_num |
| `heritage_project` | 非遗项目 | name, category_id, region, description, history, cultural_meaning, craft |
| `knowledge_document` | 知识文档 | heritage_project_id, title, doc_type, content, source, source_url, author, verified |
| `knowledge_chunk` | RAG 知识片段 | document_id, heritage_project_id, chunk_index, chunk_text, embedding_id |
| `ai_task` | AI 生成任务 | user_id, task_type(CHAT/IMAGE/VIDEO), status, input_text, result_url, provider_task_id |
| `biz_user_work` | 用户 AI 作品 | user_id, work_type, title, prompt, enhanced_prompt, result_url, task_id |
| `biz_work_source` | 作品知识来源 | work_id, heritage_project_id, source_content, retrieval_score |
| `sys_operation_log` | 操作日志 | user_id, module, operation, request_url, execution_time |
| `chat_session` | AI 对话会话 | user_id, title, heritage_project_id, status |
| `chat_message` | AI 对话消息 | session_id, role, content, sources(JSON), seq_no |

### ER 关系概览

```
sys_user ──< ai_task ──< biz_user_work ──< biz_work_source
     │                                            │
     └────────────────────────────────────────────┘

sys_user ──< chat_session ──< chat_message

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

## 团队信息

本项目由「遗韵」团队开发，团队成员及分工如下：

| 成员 | 角色 | 主要工作 |
|------|------|----------|
| Xixke | 全栈开发 | 前端、后端、AI 服务、数据库设计、RAG 检索、图像生成 |
| sujie | 信息收集 | 非遗文化资料收集与整理、知识文档编辑、来源核验 |

- 项目仓库：GitHub / Gitee（须设为 Public 或提供评审访问权限）
- 联系方式：Xiexike7@163.com

---

## 开发进度与计划

### 当前进度

| 模块 | 状态 | 说明 |
|------|------|------|
| 项目骨架（前端/后端/AI 服务） | ✅ 完成 | 三端架构搭建完成 |
| 用户认证（JWT + Spring Security） | ✅ 完成 | 注册、登录、密码加密、Token 校验、改密 |
| 非遗内容展示 | ✅ 完成 | 分类、项目列表、详情 |
| RAG 检索核心 | ✅ 完成 | 切分、向量化、FAISS 索引、语义检索 |
| AI 对话接口 | ✅ 完成 | 端到端打通：前端→后端→AI 服务 |
| AI 对话流式输出（SSE） | ✅ 完成 | 支持逐字流式输出 |
| 对话会话管理 | ✅ 完成 | 会话列表、历史消息、删除会话 |
| AI 图像生成 | ✅ 完成 | DashScope Qwen-Image 文生图，多风格多比例 |
| 任务记录 | ✅ 完成 | AI 对话与图像生成任务自动记录 |
| 作品管理 | ✅ 完成 | 保存、列表、详情、删除（逻辑删除） |
| 前端页面 | ✅ 完成 | 首页、非遗展示、AI 对话、创作、作品、个人中心 |
| AI 视频生成 | ⏳ 未开始 | 依赖视频生成服务 |
| 知识库管理后台 | ⏳ 未开始 | 文档上传与索引重建界面 |

### 后续计划

**P0.5 - 体验提升**
- [ ] AI 视频生成接入
- [ ] 知识库管理后台（文档 CRUD + 索引重建按钮）
- [ ] 操作日志审计接口
- [ ] Prompt 智能增强（RAG + 创作 Prompt 融合）

**P1 - 平台完善**
- [ ] 作品分享与社区
- [ ] 非遗项目贡献机制
- [ ] 管理后台（用户、内容、审核）

---

## 许可证

本项目仅供学习与非遗文化传播使用。