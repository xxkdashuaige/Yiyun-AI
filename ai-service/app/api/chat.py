"""
AI 接口路由模块
----------------
提供以下接口：
- POST /api/ai/chat: 文本对话，返回 AI 回答及 RAG 检索来源
- POST /api/ai/chat/stream: 流式文本对话（SSE），逐块返回回答文本，末尾发送来源
- POST /api/ai/index/rebuild: 全量重建 FAISS 向量索引
"""

import json
from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse
from app.schemas import ChatRequest, ChatResponse, SourceItem
from app.rag.retriever import retrieve
from app.core.llm import generate_reply, generate_reply_stream

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
    sources = _build_sources(contexts)

    return ChatResponse(reply=reply, sources=sources)


@router.post("/chat/stream")
async def chat_stream(req: ChatRequest):
    """
    AI 文本对话流式接口（SSE）。

    事件格式：
    - data: {"delta": "文本片段"}   逐个文本片段
    - data: {"sources": [...]}      流末尾发送 RAG 来源列表
    - data: [DONE]                  流结束标记
    """
    if not req.message or not req.message.strip():
        raise HTTPException(status_code=400, detail="message 不能为空")

    contexts = retrieve(req.message, heritage_project_id=req.heritage_project_id)
    sources = _build_sources(contexts)

    def event_generator():
        for delta in generate_reply_stream(
            req.message, contexts, conversation_history=req.conversation_history
        ):
            yield f"data: {json.dumps({'delta': delta}, ensure_ascii=False)}\n\n"
        sources_data = [s.model_dump() for s in sources]
        yield f"data: {json.dumps({'sources': sources_data}, ensure_ascii=False)}\n\n"
        yield "data: [DONE]\n\n"

    return StreamingResponse(event_generator(), media_type="text/event-stream")


def _build_sources(contexts: list[dict]) -> list[SourceItem]:
    """从检索结果组装来源列表"""
    return [
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


@router.post("/index/rebuild")
def rebuild_index():
    """全量重建 FAISS 向量索引（知识文档变更后调用）"""
    from app.rag.retriever import build_index
    build_index()
    return {"code": 200, "message": "FAISS 索引重建完成"}