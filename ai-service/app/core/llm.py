"""
LLM 调用模块
------------
封装对大语言模型（目前支持 DeepSeek）的调用。
将 RAG 检索到的知识片段拼接成 prompt 上下文，让 LLM 基于知识库内容回答问题。
若未配置 API Key，则返回检索到的原始片段，便于调试检索效果。
"""

import re
import json
import httpx
from typing import Generator
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


def _build_system_prompt(message: str, contexts: list[dict]) -> str:
    """构建系统提示词（供普通接口和流式接口共用）"""
    context_text = "\n\n".join(
        f"[来源：{c.get('title', '')}]\n{c['content']}"
        for c in contexts
    )
    return (
        "你是遗韵非遗文化智能助手，基于检索到的非遗知识回答用户问题。\n"
        "要求：\n"
        "1. 回答必须基于提供的知识库内容，不要编造信息\n"
        "2. 如果知识库中没有相关内容，请如实告知用户\n"
        "3. 回答简洁准确，突出非遗文化特色\n"
        f"\n知识库内容：\n{context_text}"
    )


def _build_messages(system_prompt: str, user_message: str, conversation_history: list[dict] = None) -> list[dict]:
    """构建发送给 LLM 的消息列表"""
    messages = [{"role": "system", "content": system_prompt}]
    if conversation_history:
        messages.extend(conversation_history[-10:])
    messages.append({"role": "user", "content": user_message})
    return messages


def generate_reply_stream(
    message: str, contexts: list[dict], conversation_history: list[dict] = None
) -> Generator[str, None, None]:
    """
    流式生成回答，逐块 yield 文本片段。
    当未配置 LLM 时，使用 mock 逐字输出。
    """
    system_prompt = _build_system_prompt(message, contexts)

    if settings.LLM_PROVIDER == "deepseek" and settings.DEEPSEEK_API_KEY:
        yield from _stream_deepseek(system_prompt, message, conversation_history)
    elif settings.LLM_PROVIDER == "ollama":
        yield from _stream_ollama(system_prompt, message, conversation_history)
    else:
        yield from _stream_mock(message, contexts)


def _stream_ollama(system_prompt: str, user_message: str, conversation_history: list[dict] = None) -> Generator[str, None, None]:
    """流式调用 Ollama"""
    messages = _build_messages(system_prompt, user_message, conversation_history)
    url = f"{settings.OLLAMA_BASE_URL}/v1/chat/completions"
    headers = {"Content-Type": "application/json"}
    payload = {
        "model": settings.OLLAMA_MODEL,
        "messages": messages,
        "temperature": 0.7,
        "stream": True,
    }

    with httpx.Client(timeout=120) as client:
        with client.stream("POST", url, json=payload, headers=headers) as resp:
            resp.raise_for_status()
            buffer = ""
            for line in resp.iter_lines():
                if not line or not line.startswith("data: "):
                    continue
                data_str = line[6:]
                if data_str.strip() == "[DONE]":
                    break
                try:
                    data = json.loads(data_str)
                    delta = data["choices"][0]["delta"].get("content", "")
                    if delta:
                        buffer += delta
                        yield delta
                except (json.JSONDecodeError, KeyError, IndexError):
                    continue


def _stream_deepseek(system_prompt: str, user_message: str, conversation_history: list[dict] = None) -> Generator[str, None, None]:
    """流式调用 DeepSeek"""
    messages = _build_messages(system_prompt, user_message, conversation_history)
    url = f"{settings.DEEPSEEK_BASE_URL}/chat/completions"
    headers = {
        "Authorization": f"Bearer {settings.DEEPSEEK_API_KEY}",
        "Content-Type": "application/json",
    }
    payload = {
        "model": settings.DEEPSEEK_MODEL,
        "messages": messages,
        "temperature": 0.7,
        "stream": True,
    }

    with httpx.Client(timeout=120) as client:
        with client.stream("POST", url, json=payload, headers=headers) as resp:
            resp.raise_for_status()
            for line in resp.iter_lines():
                if not line or not line.startswith("data: "):
                    continue
                data_str = line[6:]
                if data_str.strip() == "[DONE]":
                    break
                try:
                    data = json.loads(data_str)
                    delta = data["choices"][0]["delta"].get("content", "")
                    if delta:
                        yield delta
                except (json.JSONDecodeError, KeyError, IndexError):
                    continue


def _stream_mock(message: str, contexts: list[dict]) -> Generator[str, None, None]:
    """mock 流式输出，逐字返回"""
    import time
    full_reply = _mock_reply(message, contexts)
    for char in full_reply:
        yield char
        time.sleep(0.02)