"""
Embedding 向量化模块
--------------------
使用 BGE-M3 模型将文本转换为向量表示。
模型采用懒加载（首次调用时加载），避免启动时阻塞。
"""

import numpy as np
from app.core.config import settings

_model = None


def _get_model():
    """懒加载 Embedding 模型（BGE-M3）"""
    global _model
    if _model is None:
        from sentence_transformers import SentenceTransformer
        _model = SentenceTransformer(settings.EMBEDDING_MODEL)
    return _model


def embed_texts(texts: list[str]) -> np.ndarray:
    """
    对文本列表生成 embedding 向量。
    返回 shape 为 (n, dim) 的 numpy 数组。
    """
    if not texts:
        return np.array([])
    model = _get_model()
    embeddings = model.encode(texts, normalize_embeddings=True, show_progress_bar=False)
    return np.array(embeddings, dtype=np.float32)


def embed_query(text: str) -> np.ndarray:
    """
    对单个查询文本生成 embedding 向量。
    返回 shape 为 (dim,) 的 numpy 数组。
    """
    return embed_texts([text])[0]