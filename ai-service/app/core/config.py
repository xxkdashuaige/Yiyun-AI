"""
配置管理模块
------------
从 .env 文件加载环境变量，集中管理 ai-service 的所有配置项，
包括服务端口、LLM、Embedding、FAISS、MySQL、文本切分参数等。
"""

import os
from pathlib import Path
from dotenv import load_dotenv

BASE_DIR = Path(__file__).resolve().parent.parent.parent
load_dotenv(BASE_DIR / ".env")


class Settings:
    # ==================== 服务配置 ====================
    SERVICE_HOST: str = os.getenv("SERVICE_HOST", "0.0.0.0")
    SERVICE_PORT: int = int(os.getenv("SERVICE_PORT", "8000"))

    # ==================== LLM 配置 ====================
    LLM_PROVIDER: str = os.getenv("LLM_PROVIDER", "ollama")
    DEEPSEEK_API_KEY: str = os.getenv("DEEPSEEK_API_KEY", "")
    DEEPSEEK_BASE_URL: str = os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com/v1")
    DEEPSEEK_MODEL: str = os.getenv("DEEPSEEK_MODEL", "deepseek-chat")
    # 本地 Ollama（开发阶段节约成本，无需 API Key）
    OLLAMA_BASE_URL: str = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
    OLLAMA_MODEL: str = os.getenv("OLLAMA_MODEL", "qwen2.5:7b")
    # OpenAI 兼容接口（适用于 WaLiAPI 等 OpenAI 兼容网关）
    OPENAI_API_KEY: str = os.getenv("OPENAI_API_KEY", "")
    OPENAI_BASE_URL: str = os.getenv("OPENAI_BASE_URL", "http://127.0.0.1:8777/v1")
    OPENAI_MODEL: str = os.getenv("OPENAI_MODEL", "kilo-auto/free")

    # ==================== Embedding 配置 ====================
    EMBEDDING_PROVIDER: str = os.getenv("EMBEDDING_PROVIDER", "local")
    EMBEDDING_MODEL: str = os.getenv("EMBEDDING_MODEL", "BAAI/bge-m3")

    # ==================== FAISS 向量库配置 ====================
    FAISS_INDEX_PATH: str = os.getenv("FAISS_INDEX_PATH", "./data/faiss_index")
    FAISS_TOP_K: int = int(os.getenv("FAISS_TOP_K", "5"))

    # ==================== MySQL 配置 ====================
    MYSQL_HOST: str = os.getenv("MYSQL_HOST", "localhost")
    MYSQL_PORT: int = int(os.getenv("MYSQL_PORT", "3306"))
    MYSQL_USER: str = os.getenv("MYSQL_USER", "root")
    MYSQL_PASSWORD: str = os.getenv("MYSQL_PASSWORD", "root")
    MYSQL_DATABASE: str = os.getenv("MYSQL_DATABASE", "yiyun")

    # ==================== 文本切分配置 ====================
    CHUNK_SIZE: int = int(os.getenv("CHUNK_SIZE", "400"))              # 单片段目标字数
    CHUNK_OVERLAP_RATIO: float = float(os.getenv("CHUNK_OVERLAP_RATIO", "0.15"))  # 相邻片段重叠比例

    # ==================== 图像生成配置（DashScope Qwen-Image） ====================
    IMAGE_PROVIDER: str = os.getenv("IMAGE_PROVIDER", "")
    DASHSCOPE_API_KEY: str = os.getenv("DASHSCOPE_API_KEY", os.getenv("IMAGE_API_KEY", ""))
    QWEN_IMAGE_MODEL: str = os.getenv("IMAGE_MODEL", "qwen-image-2.0")
    # 百炼业务空间 ID（Workspace ID），用于构建专属域名
    DASHSCOPE_WORKSPACE_ID: str = os.getenv("DASHSCOPE_WORKSPACE_ID", "")
    DASHSCOPE_REGION: str = os.getenv("DASHSCOPE_REGION", "cn-beijing")

    @property
    def dashscope_base_url(self) -> str:
        """构建百炼平台专属域名 base_url。"""
        if self.DASHSCOPE_WORKSPACE_ID:
            return f"https://{self.DASHSCOPE_WORKSPACE_ID}.{self.DASHSCOPE_REGION}.maas.aliyuncs.com/api/v1"
        return "https://dashscope.aliyuncs.com/api/v1"


settings = Settings()