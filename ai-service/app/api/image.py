"""
图像生成接口路由模块
-------------------
提供以下接口：
- POST /api/ai/generate/image: 提交文生图异步任务，返回任务 ID
- GET  /api/ai/tasks/{task_id}: 查询图像生成任务状态及结果
"""

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Optional
from app.services import image_gen

router = APIRouter(prefix="/api/ai", tags=["AI-Image"])


# 前端比例 -> Qwen-Image 像素尺寸
SIZE_MAP = {
    "1:1": "1024*1024",
    "16:9": "1280*720",
    "9:16": "720*1280",
    "4:3": "1024*768",
    "3:4": "768*1024",
}

# 风格描述词
STYLE_MAP = {
    "ink": "水墨画风格，黑白灰为主，留白意境",
    "oil": "油画风格，色彩厚重，笔触明显",
    "watercolor": "水彩画风格，色彩通透，晕染自然",
    "chinese-painting": "中国传统国画风格，工笔重彩",
    "realistic": "写实摄影风格，细节丰富，光影自然",
}


class ImageGenerateRequest(BaseModel):
    prompt: str
    style: Optional[str] = None
    size: Optional[str] = "1:1"
    n: Optional[int] = 1


@router.post("/generate/image")
def generate_image(req: ImageGenerateRequest):
    """
    提交文生图异步任务。

    返回：{"task_id": "DashScope 任务 ID"}
    """
    if not req.prompt or not req.prompt.strip():
        raise HTTPException(status_code=400, detail="prompt 不能为空")

    full_prompt = req.prompt
    if req.style and req.style in STYLE_MAP:
        full_prompt = f"{full_prompt}，{STYLE_MAP[req.style]}"

    pixel_size = SIZE_MAP.get(req.size or "1:1", "1024*1024")

    try:
        task_id = image_gen.submit_task(full_prompt, size=pixel_size, n=req.n or 1)
    except RuntimeError as e:
        raise HTTPException(status_code=500, detail=str(e))

    return {"task_id": task_id}


@router.get("/tasks/{task_id}")
def get_image_task(task_id: str):
    """
    查询图像生成任务状态。

    返回：{"status": "RUNNING"|"SUCCEEDED"|"FAILED", "image_url": "...", "error": "..."}
    """
    try:
        result = image_gen.get_task(task_id)
    except RuntimeError as e:
        raise HTTPException(status_code=500, detail=str(e))
    return result