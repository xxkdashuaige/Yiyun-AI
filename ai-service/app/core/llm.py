"""
LLM 调用模块
------------
封装对大语言模型（目前支持 DeepSeek）的调用。
将 RAG 检索到的知识片段拼接成 prompt 上下文，让 LLM 基于知识库内容回答问题。
若未配置 API Key，则返回检索到的原始片段，便于调试检索效果。
"""

import re
import httpx
from app.core.config import settings


def generate_reply(message: str, contexts: list[dict], conversation_history: list[dict] = None) -> str:
    """
    调用 LLM 生成回答。
    contexts: retrieve 返回的知识片段列表，用于拼接 prompt 上下文。
    conversation_history: 对话历史消息列表，每条 {"role": "user"|"assistant", "content": "..."}。
    """
    context_text = "\n\n".join(
        f"[来源：{c.get('title', '')}]\n{c['content']}"
        for c in contexts
    )

    system_prompt = (
        "你是遗韵非遗文化智能助手，基于检索到的非遗知识回答用户问题。\n"
        "要求：\n"
        "1. 回答必须基于提供的知识库内容，不要编造信息\n"
        "2. 如果知识库中没有相关内容，请如实告知用户\n"
        "3. 回答简洁准确，突出非遗文化特色\n"
        f"\n知识库内容：\n{context_text}"
    )

    if settings.LLM_PROVIDER == "deepseek" and settings.DEEPSEEK_API_KEY:
        return _call_deepseek(system_prompt, message, conversation_history)

    if settings.LLM_PROVIDER == "ollama":
        return _call_ollama(system_prompt, message, conversation_history)

    return _mock_reply(message, contexts)


def _call_ollama(system_prompt: str, user_message: str, conversation_history: list[dict] = None) -> str:
    """调用本地 Ollama 生成回答（使用 OpenAI 兼容接口，无需 API Key）"""
    messages = [{"role": "system", "content": system_prompt}]
    if conversation_history:
        messages.extend(conversation_history[-10:])
    messages.append({"role": "user", "content": user_message})

    url = f"{settings.OLLAMA_BASE_URL}/v1/chat/completions"
    headers = {"Content-Type": "application/json"}
    payload = {
        "model": settings.OLLAMA_MODEL,
        "messages": messages,
        "temperature": 0.7,
        "stream": False,
    }

    with httpx.Client(timeout=120) as client:
        resp = client.post(url, json=payload, headers=headers)
        resp.raise_for_status()
        data = resp.json()
        content = data["choices"][0]["message"]["content"]
        return _strip_think(content)


def _call_deepseek(system_prompt: str, user_message: str, conversation_history: list[dict] = None) -> str:
    """调用 DeepSeek Chat Completion 接口生成回答"""
    messages = [{"role": "system", "content": system_prompt}]
    if conversation_history:
        # 只取最近 10 轮，避免 token 超限
        messages.extend(conversation_history[-10:])
    messages.append({"role": "user", "content": user_message})

    url = f"{settings.DEEPSEEK_BASE_URL}/chat/completions"
    headers = {
        "Authorization": f"Bearer {settings.DEEPSEEK_API_KEY}",
        "Content-Type": "application/json",
    }
    payload = {
        "model": settings.DEEPSEEK_MODEL,
        "messages": messages,
        "temperature": 0.7,
    }

    with httpx.Client(timeout=60) as client:
        resp = client.post(url, json=payload, headers=headers)
        resp.raise_for_status()
        data = resp.json()
        return data["choices"][0]["message"]["content"]


def _strip_think(text: str) -> str:
    """
    清洗 DeepSeek-R1 等推理模型输出的 <think>...</think> 思维链内容，
    只返回给用户最终的回答部分。
    同时处理未闭合的 <think> 标签（模型输出被截断时可能出现）。
    """
    cleaned = re.sub(r"<think>.*?</think>", "", text, flags=re.DOTALL)
    cleaned = re.sub(r"<think>.*$", "", cleaned, flags=re.DOTALL)
    return cleaned.strip()


def _mock_reply(message: str, contexts: list[dict]) -> str:
    """
    当没有配置 LLM API Key 时，返回检索到的知识片段拼接结果，便于调试检索效果。
    """
    if not contexts:
        return f"（未配置 LLM，且未检索到相关知识）你问的是：{message}"

    parts = [f"（未配置 LLM，以下为检索到的相关知识片段）\n"]
    for i, c in enumerate(contexts, 1):
        parts.append(f"[{i}] {c.get('title', '')}：\n{c['content']}\n")
    return "\n".join(parts)