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
from dataclasses import dataclass
from typing import Generator
from app.core.config import settings

NO_CONTEXT_TEXT = "（本次未检索到与用户问题相关的知识库内容）"

SYSTEM_PROMPT_TEMPLATE = (
    "你是遗韵非遗文化智能助手，专注于中国非物质文化遗产的介绍与解答，"
    "语气亲切自然，像一位热爱非遗文化的朋友在与用户交流。\n"
    "回答要求：\n"
    "1. 有知识库内容时，必须基于知识库内容回答，不要编造信息，可适当润色使表达更生动。\n"
    "2. 如果知识库中没有相关内容，不要冷冰冰地拒绝，而是用温暖的语气告诉用户目前知识库暂未收录该内容，"
    "并主动引导用户，例如可以说「目前我的知识库里还没有这方面的内容呢，不过我对传统技艺、传统美术、民俗等非遗门类都有些了解，要不要聊聊这些？」"
    "鼓励用户换个非遗相关的话题继续提问。\n"
    "3. 回答简洁准确，突出非遗文化特色，适当使用富有文化韵味的表达。\n"
    "\n知识库内容：\n{context_text}"
)


@dataclass
class ProviderConfig:
    """LLM 提供商配置，描述各提供商之间的差异点"""
    name: str
    base_url: str
    model: str
    api_key: str
    timeout: int
    strip_think: bool
    requires_api_key: bool = False
    url_path: str = "/chat/completions"


PROVIDERS: dict[str, ProviderConfig] = {
    "ollama": ProviderConfig(
        name="ollama",
        base_url=settings.OLLAMA_BASE_URL,
        model=settings.OLLAMA_MODEL,
        api_key="",
        timeout=120,
        strip_think=True,
        requires_api_key=False,
        url_path="/v1/chat/completions",
    ),
    "deepseek": ProviderConfig(
        name="deepseek",
        base_url=settings.DEEPSEEK_BASE_URL,
        model=settings.DEEPSEEK_MODEL,
        api_key=settings.DEEPSEEK_API_KEY,
        timeout=60,
        strip_think=False,
        requires_api_key=True,
    ),
    "openai": ProviderConfig(
        name="openai",
        base_url=settings.OPENAI_BASE_URL,
        model=settings.OPENAI_MODEL,
        api_key=settings.OPENAI_API_KEY,
        timeout=120,
        strip_think=True,
        requires_api_key=True,
    ),
}


def _resolve_provider() -> ProviderConfig | None:
    """
    根据配置解析当前应使用的 LLM 提供商。
    若提供商需要 API Key 但未配置，则返回 None（调用方 fallback 到 mock）。
    """
    cfg = PROVIDERS.get(settings.LLM_PROVIDER)
    if cfg is None:
        return None
    if cfg.requires_api_key and not cfg.api_key.strip():
        return None
    return cfg


def _build_context_text(contexts: list[dict]) -> str:
    """将检索到的知识片段拼接为上下文文本，无内容时返回占位提示"""
    if not contexts:
        return NO_CONTEXT_TEXT
    return "\n\n".join(
        f"[来源：{c.get('title', '')}]\n{c['content']}"
        for c in contexts
    )


def _build_system_prompt(contexts: list[dict]) -> str:
    """构建系统提示词（供普通接口和流式接口共用）"""
    return SYSTEM_PROMPT_TEMPLATE.format(context_text=_build_context_text(contexts))


def _build_messages(system_prompt: str, user_message: str, conversation_history: list[dict] = None) -> list[dict]:
    """构建发送给 LLM 的消息列表，只保留最近 10 轮对话避免 token 超限"""
    messages = [{"role": "system", "content": system_prompt}]
    if conversation_history:
        messages.extend(conversation_history[-10:])
    messages.append({"role": "user", "content": user_message})
    return messages


def _build_headers(cfg: ProviderConfig) -> dict[str, str]:
    """根据提供商配置构建请求头（需要 API Key 的自动带上 Authorization）"""
    headers = {"Content-Type": "application/json"}
    if cfg.api_key:
        headers["Authorization"] = f"Bearer {cfg.api_key}"
    return headers


def generate_reply(message: str, contexts: list[dict], conversation_history: list[dict] = None) -> str:
    """
    调用 LLM 生成回答。
    contexts: retrieve 返回的知识片段列表，用于拼接 prompt 上下文。
    conversation_history: 对话历史消息列表，每条 {"role": "user"|"assistant", "content": "..."}。
    """
    system_prompt = _build_system_prompt(contexts)
    cfg = _resolve_provider()
    if cfg is None:
        return _mock_reply(message, contexts)
    return _call_llm(cfg, system_prompt, message, conversation_history)


def _call_llm(cfg: ProviderConfig, system_prompt: str, user_message: str, conversation_history: list[dict] = None) -> str:
    """统一的同步 LLM 调用，差异点由 ProviderConfig 描述"""
    messages = _build_messages(system_prompt, user_message, conversation_history)
    url = f"{cfg.base_url}{cfg.url_path}"
    payload = {
        "model": cfg.model,
        "messages": messages,
        "temperature": 0.7,
        "stream": False,
    }

    with httpx.Client(timeout=cfg.timeout) as client:
        resp = client.post(url, json=payload, headers=_build_headers(cfg))
        resp.raise_for_status()
        data = resp.json()

    content = data["choices"][0]["message"]["content"]
    return _strip_think(content) if cfg.strip_think else content


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


def generate_reply_stream(
    message: str, contexts: list[dict], conversation_history: list[dict] = None
) -> Generator[str, None, None]:
    """
    流式生成回答，逐块 yield 文本片段。
    当未配置 LLM 时，使用 mock 逐字输出。
    """
    system_prompt = _build_system_prompt(contexts)
    cfg = _resolve_provider()
    if cfg is None:
        yield from _stream_mock(message, contexts)
    else:
        yield from _stream_llm(cfg, system_prompt, message, conversation_history)


def _stream_llm(cfg: ProviderConfig, system_prompt: str, user_message: str, conversation_history: list[dict] = None) -> Generator[str, None, None]:
    """统一的流式 LLM 调用（SSE），差异点由 ProviderConfig 描述"""
    messages = _build_messages(system_prompt, user_message, conversation_history)
    url = f"{cfg.base_url}{cfg.url_path}"
    payload = {
        "model": cfg.model,
        "messages": messages,
        "temperature": 0.7,
        "stream": True,
    }

    with httpx.Client(timeout=cfg.timeout) as client:
        with client.stream("POST", url, json=payload, headers=_build_headers(cfg)) as resp:
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