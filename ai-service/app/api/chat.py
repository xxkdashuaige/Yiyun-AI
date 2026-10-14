"""
AI 接口路由模块
----------------
提供以下接口：
- POST /api/ai/chat: 文本对话，返回 AI 回答及 RAG 检索来源
- POST /api/ai/index/rebuild: 全量重建 FAISS 向量索引
"""

from fastapi import APIRouter, HTTPException
from app.schemas import ChatRequest, ChatResponse, SourceItem
from app.rag.retriever import retrieve
from app.core.llm import generate_reply

router = APIRouter(prefix="/api/ai", tags=["AI"])


@router.post("/chat", response_model=ChatResponse)
def chat(req: ChatRequest):
    """
    AI 文本对话接口。

    流程：
    1. 对用户问题进行 RAG 语义检索，获取相关知识片段
    2. 将检索结果送入 LLM 生成回答
    3. 返回回答文本及引用来源列表
    """
    if not req.message or not req.message.strip():
        raise HTTPException(status_code=400, detail="message 不能为空")

    # RAG 检索相关知识片段（可选限定非遗项目范围）
    contexts = retrieve(req.message, heritage_project_id=req.heritage_project_id)

    # 调用 LLM 生成回答（携带对话历史）
    reply = generate_reply(req.message, contexts, conversation_history=req.conversation_history)

    # 组装来源信息
    sources = [
        SourceItem(
            chunk_id=c["chunk_id"],
            document_id=c["document_id"],
            heritage_project_id=c.get("heritage_project_id"),
            title=c.get("title", ""),
            doc_type=c.get("doc_type"),
            source=c.get("source"),
            content=c["content"],
            score=c["score"],
        )
        for c in contexts
    ]

    return ChatResponse(reply=reply, sources=sources)


@router.post("/index/rebuild")
def rebuild_index():
    """全量重建 FAISS 向量索引（知识文档变更后调用）"""
    from app.rag.retriever import build_index
    build_index()
    return {"code": 200, "message": "FAISS 索引重建完成"}