"""
文本切分模块
------------
将长文本按段落切分为适合向量化的知识片段。
策略：按段落切分 + 滑动窗口重叠（overlap），避免语义边界被切断。
"""

import re
from app.core.config import settings


def split_text_into_chunks(text: str) -> list[str]:
    """
    按段落切分文本，相邻片段保留 overlap_ratio 比例的重叠。

    策略：
    1. 按换行符切成段落
    2. 段落太短则与下一段合并，直到达到 chunk_size
    3. 超长段落按句号切分
    4. 相邻片段保留 overlap_ratio 比例的重叠
    """
    if not text or not text.strip():
        return []

    paragraphs = [p.strip() for p in text.split("\n") if p.strip()]
    if not paragraphs:
        return []

    chunk_size = settings.CHUNK_SIZE
    overlap_size = int(chunk_size * settings.CHUNK_OVERLAP_RATIO)

    chunks: list[str] = []
    current = ""

    for para in paragraphs:
        if len(current) + len(para) + 1 <= chunk_size:
            current = (current + "\n" + para).strip() if current else para
        else:
            if current:
                chunks.append(current)
                current = current[-overlap_size:] + "\n" + para if overlap_size > 0 else para
            else:
                sub_chunks = _split_long_paragraph(para, chunk_size, overlap_size)
                chunks.extend(sub_chunks[:-1])
                current = sub_chunks[-1] if sub_chunks else ""

    if current.strip():
        chunks.append(current.strip())

    return [c.strip() for c in chunks if c.strip()]


def _split_long_paragraph(para: str, chunk_size: int, overlap_size: int) -> list[str]:
    """
    对超长段落按句号、问号、感叹号切分，再合并到目标长度。
    """
    sentences = re.split(r"(?<=[。！？!?])", para)
    sentences = [s.strip() for s in sentences if s.strip()]

    if not sentences:
        return [para[:chunk_size]]

    result: list[str] = []
    current = ""

    for s in sentences:
        if len(current) + len(s) <= chunk_size:
            current += s
        else:
            if current:
                result.append(current)
                current = current[-overlap_size:] + s if overlap_size > 0 else s
            else:
                for i in range(0, len(s), chunk_size - overlap_size):
                    piece = s[i:i + chunk_size]
                    if piece:
                        result.append(piece)
                current = ""

    if current.strip():
        result.append(current.strip())

    return result