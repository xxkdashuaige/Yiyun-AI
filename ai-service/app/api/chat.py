"""
AI 接口路由模块
----------------
提供以下接口：
- POST /api/ai/chat: 文本对话，返回 AI 回答及 RAG 检索来源
- POST /api/ai/chat/stream: 流式文本对话（SSE），逐块返回回答文本，末尾发送来源
- POST /api/ai/index/rebuild: 全量重建 FAISS 向量索引
- GET  /api/ai/sessions: 获取当前用户的会话列表
- GET  /api/ai/sessions/{id}/messages: 获取指定会话的消息列表
- DELETE /api/ai/sessions/{id}: 删除会话
"""

import json
from fastapi import APIRouter, HTTPException, Depends
from fastapi.responses import StreamingResponse
from app.schemas import ChatRequest, ChatResponse, SourceItem, ChatSession, ChatMessage
from app.rag.retriever import retrieve
from app.core.llm import generate_reply, generate_reply_stream
from app.core.auth import get_current_user_id
from app.db import mysql

router = APIRouter(prefix="/api/ai", tags=["AI"])

HISTORY_ROUNDS = 10  # 传给 LLM 的最近对话轮数（每轮 = user + assistant 两条）
TITLE_MAX_LEN = 20   # 会话标题最大字符数


def _build_title(message: str) -> str:
    """从首条用户消息截取前 N 个字符作为会话标题"""
    msg = message.strip()
    if len(msg) > TITLE_MAX_LEN:
        return msg[:TITLE_MAX_LEN] + "..."
    return msg or "新对话"


def _date_to_str(value) -> str | None:
    """将 date/datetime 对象转为 ISO 格式字符串，None 保持不变"""
    if value is None:
        return None
    if hasattr(value, "isoformat"):
        return value.isoformat()
    return str(value)


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
            source_url=c.get("source_url"),
            author=c.get("author"),
            publish_date=_date_to_str(c.get("publish_date")),
            access_date=_date_to_str(c.get("access_date")),
            page_no=c.get("page_no"),
            rights_note=c.get("rights_note"),
            verified=c.get("verified"),
            content=c["content"],
            score=c["score"],
        )
        for c in contexts
    ]


def _resolve_session(req: ChatRequest, user_id: int) -> tuple[int, list[dict]]:
    """
    解析会话：若请求带 session_id 则校验归属并读取历史；否则新建会话。
    返回 (session_id, conversation_history)。
    conversation_history 取最近 HISTORY_ROUNDS 轮，每条 {"role","content"}。
    """
    if req.session_id:
        session = mysql.get_chat_session(req.session_id)
        if not session:
            raise HTTPException(status_code=404, detail="会话不存在")
        if session["user_id"] != user_id:
            raise HTTPException(status_code=403, detail="无权访问该会话")
        if session["status"] != 1:
            raise HTTPException(status_code=404, detail="会话已被删除")
        session_id = req.session_id
        # 读取历史消息并截取最近 N 轮
        all_msgs = mysql.list_chat_messages(session_id)
        history = [{"role": m["role"], "content": m["content"]} for m in all_msgs]
        history = history[-(HISTORY_ROUNDS * 2):]
    else:
        title = _build_title(req.message)
        session_id = mysql.create_chat_session(user_id, title, req.heritage_project_id)
        history = []
    return session_id, history


@router.post("/chat", response_model=ChatResponse)
def chat(req: ChatRequest, user_id: int = Depends(get_current_user_id)):
    """
    AI 文本对话接口。

    流程：
    1. 解析/创建会话，从数据库读取最近对话历史
    2. 对用户问题进行 RAG 语义检索，获取相关知识片段
    3. 将检索结果 + 历史送入 LLM 生成回答
    4. 保存用户消息和 AI 回答到数据库
    5. 返回回答文本、引用来源及会话 ID
    """
    if not req.message or not req.message.strip():
        raise HTTPException(status_code=400, detail="message 不能为空")

    session_id, history = _resolve_session(req, user_id)

    contexts = retrieve(req.message, heritage_project_id=req.heritage_project_id)
    reply = generate_reply(req.message, contexts, conversation_history=history)
    sources = _build_sources(contexts)

    mysql.insert_chat_message(session_id, "user", req.message)
    sources_data = [s.model_dump() for s in sources]
    mysql.insert_chat_message(session_id, "assistant", reply, sources=sources_data)
    mysql.touch_chat_session(session_id)

    return ChatResponse(reply=reply, sources=sources, session_id=session_id)


@router.post("/chat/stream")
async def chat_stream(req: ChatRequest, user_id: int = Depends(get_current_user_id)):
    """
    AI 文本对话流式接口（SSE）。

    事件格式：
    - data: {"delta": "文本片段"}   逐个文本片段
    - data: {"sources": [...], "session_id": N}  流末尾发送 RAG 来源列表及会话 ID
    - data: [DONE]                  流结束标记
    """
    if not req.message or not req.message.strip():
        raise HTTPException(status_code=400, detail="message 不能为空")

    session_id, history = _resolve_session(req, user_id)

    contexts = retrieve(req.message, heritage_project_id=req.heritage_project_id)
    sources = _build_sources(contexts)
    sources_data = [s.model_dump() for s in sources]

    # 先保存用户消息
    mysql.insert_chat_message(session_id, "user", req.message)

    full_reply_parts = []

    def event_generator():
        for delta in generate_reply_stream(
            req.message, contexts, conversation_history=history
        ):
            full_reply_parts.append(delta)
            yield f"data: {json.dumps({'delta': delta}, ensure_ascii=False)}\n\n"

        full_reply = "".join(full_reply_parts)
        mysql.insert_chat_message(session_id, "assistant", full_reply, sources=sources_data)
        mysql.touch_chat_session(session_id)

        yield f"data: {json.dumps({'sources': sources_data, 'session_id': session_id}, ensure_ascii=False)}\n\n"
        yield "data: [DONE]\n\n"

    return StreamingResponse(event_generator(), media_type="text/event-stream")


# ==================== 会话管理接口 ====================

@router.get("/sessions", response_model=list[ChatSession])
def list_sessions(user_id: int = Depends(get_current_user_id)):
    """获取当前用户的会话列表（按更新时间倒序）"""
    sessions = mysql.list_chat_sessions(user_id)
    return [
        ChatSession(
            id=s["id"],
            title=s["title"],
            heritage_project_id=s.get("heritage_project_id"),
            created_at=_date_to_str(s["created_at"]),
            updated_at=_date_to_str(s["updated_at"]),
        )
        for s in sessions
    ]


@router.get("/sessions/{session_id}/messages", response_model=list[ChatMessage])
def list_session_messages(session_id: int, user_id: int = Depends(get_current_user_id)):
    """获取指定会话的全部消息（用于打开历史对话时回显）"""
    session = mysql.get_chat_session(session_id)
    if not session:
        raise HTTPException(status_code=404, detail="会话不存在")
    if session["user_id"] != user_id:
        raise HTTPException(status_code=403, detail="无权访问该会话")

    messages = mysql.list_chat_messages(session_id)
    return [
        ChatMessage(
            id=m["id"],
            role=m["role"],
            content=m["content"],
            sources=m.get("sources"),
            seq_no=m["seq_no"],
            created_at=_date_to_str(m["created_at"]),
        )
        for m in messages
    ]


@router.delete("/sessions/{session_id}")
def delete_session(session_id: int, user_id: int = Depends(get_current_user_id)):
    """删除会话（软删除）"""
    affected = mysql.delete_chat_session(session_id, user_id)
    if affected == 0:
        raise HTTPException(status_code=404, detail="会话不存在或无权删除")
    return {"code": 200, "message": "会话已删除"}


@router.post("/index/rebuild")
def rebuild_index():
    """全量重建 FAISS 向量索引（知识文档变更后调用）"""
    from app.rag.retriever import build_index
    build_index()
    return {"code": 200, "message": "FAISS 索引重建完成"}