"""
图像生成服务模块
----------------
封装阿里云百炼 DashScope Qwen-Image 文生图 API 的调用。

Qwen-Image 系列模型调用说明：
- qwen-image-2.0（加速版）：仅支持同步接口，走 multimodal-generation/generation 端点
- qwen-image-2.0-pro / qwen-image-3.0-pro：支持同步 + 异步接口

本模块使用 HTTP 直接调用多模态对话接口，一次请求即可获得图片 URL。
"""

import uuid
import httpx
from app.core.config import settings

# 内存缓存：task_id -> {"image_url": str, "error": str | None}
_task_cache: dict[str, dict] = {}


def _base_url() -> str:
    return settings.dashscope_base_url


def generate_image_sync(prompt: str, size: str = "1024*1024", n: int = 1) -> str:
    """
    同步调用 DashScope 文生图（多模态对话接口），返回图片 URL。

    :param prompt: 文本提示词
    :param size: 图片尺寸，如 "1024*1024"、"1280*720"、"720*1280"
    :param n: 生成图片数量
    :return: 生成的图片 URL
    :raises RuntimeError: 生成失败时抛出
    """
    if not settings.DASHSCOPE_API_KEY:
        raise RuntimeError("DASHSCOPE_API_KEY 未配置")

    url = f"{_base_url()}/services/aigc/multimodal-generation/generation"
    headers = {
        "Authorization": f"Bearer {settings.DASHSCOPE_API_KEY}",
        "Content-Type": "application/json",
    }
    body = {
        "model": settings.QWEN_IMAGE_MODEL,
        "input": {
            "messages": [
                {
                    "role": "user",
                    "content": [{"text": prompt}],
                }
            ]
        },
        "parameters": {
            "size": size,
            "n": n,
            "prompt_extend": True,
            "watermark": False,
        },
    }

    try:
        with httpx.Client(timeout=120.0) as client:
            resp = client.post(url, headers=headers, json=body)
            resp.raise_for_status()
            data = resp.json()
    except httpx.HTTPStatusError as e:
        raise RuntimeError(
            f"调用文生图失败 (HTTP {e.response.status_code}): {e.response.text}"
        ) from e
    except Exception as e:
        raise RuntimeError(f"调用文生图网络异常: {e}") from e

    output = data.get("output", {})
    choices = output.get("choices", [])
    if not choices:
        raise RuntimeError(f"响应中未找到 choices: {data}")

    message = choices[0].get("message", {})
    content = message.get("content", [])
    image_url = None
    for item in content:
        if isinstance(item, dict) and item.get("image"):
            image_url = item["image"]
            break

    if not image_url:
        raise RuntimeError(f"响应中未找到图片 URL: {data}")

    return image_url


def submit_task(prompt: str, size: str = "1024*1024", n: int = 1) -> str:
    """
    提交文生图任务（内部同步调用 DashScope，用内存缓存模拟异步任务）。

    :param prompt: 文本提示词
    :param size: 图片尺寸
    :param n: 生成图片数量
    :return: 本地任务 ID（UUID）
    """
    task_id = str(uuid.uuid4())
    try:
        image_url = generate_image_sync(prompt, size=size, n=n)
        _task_cache[task_id] = {"image_url": image_url, "error": None}
    except RuntimeError as e:
        _task_cache[task_id] = {"image_url": None, "error": str(e)}
    return task_id


def get_task(task_id: str) -> dict:
    """
    查询图像生成任务状态。

    :param task_id: 本地任务 ID
    :return: {"status": "SUCCEEDED"|"FAILED"|"RUNNING", "image_url": str|None, "error": str|None}
    """
    cached = _task_cache.get(task_id)
    if cached is None:
        return {"status": "RUNNING", "image_url": None, "error": None}

    if cached.get("image_url"):
        return {"status": "SUCCEEDED", "image_url": cached["image_url"], "error": None}
    return {"status": "FAILED", "image_url": None, "error": cached.get("error", "生成失败")}