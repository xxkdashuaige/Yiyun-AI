"""
Pydantic 请求/响应模型定义
--------------------------
定义 ai-service 对外接口的请求体和响应体数据结构，
用于 FastAPI 自动校验和序列化。
"""

from typing import List, Optional, Dict, Any
from pydantic import BaseModel


class ChatRequest(BaseModel):
    """AI 对话请求体"""
    message: str                          # 用户输入的问题
    heritage_project_id: Optional[int] = None  # 指定非遗项目 ID（可选，限定检索范围）
    session_id: Optional[int] = None      # 指定会话 ID；不传则新建会话
    conversation_history: Optional[List[Dict[str, str]]] = None  # 【已废弃】对话历史，改由后端按 session_id 从数据库读取


class SourceItem(BaseModel):
    """RAG 检索到的知识来源条目"""
    chunk_id: int                         # 知识片段 ID
    document_id: int                      # 所属知识文档 ID
    heritage_project_id: Optional[int] = None  # 所属非遗项目 ID
    title: str                            # 文档标题
    doc_type: Optional[str] = None        # 文档类型
    source: Optional[str] = None          # 来源名称
    source_url: Optional[str] = None      # 来源链接
    author: Optional[str] = None          # 作者
    publish_date: Optional[str] = None    # 发布日期
    access_date: Optional[str] = None     # 访问日期
    page_no: Optional[str] = None         # 页码
    rights_note: Optional[str] = None     # 版权声明
    verified: Optional[int] = None        # 是否已核验：0否 1是
    content: str                          # 片段文本内容
    score: float                          # 检索相似度得分


class ChatResponse(BaseModel):
    """AI 对话响应体"""
    reply: str                            # AI 生成的回答文本
    sources: List[SourceItem] = []        # RAG 检索到的知识来源列表
    session_id: int                       # 所属会话 ID（新建或已有）


class ChatSession(BaseModel):
    """对话会话"""
    id: int
    title: str
    heritage_project_id: Optional[int] = None
    created_at: str
    updated_at: str


class ChatMessage(BaseModel):
    """对话消息"""
    id: int
    role: str
    content: str
    sources: Optional[List[Dict[str, Any]]] = None
    seq_no: int
    created_at: str