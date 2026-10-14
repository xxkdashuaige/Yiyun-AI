"""
FAISS 向量库模块
----------------
封装 FAISS 向量索引的构建、加载和检索。
使用 IndexFlatIP（内积）作为相似度度量，向量已做归一化，等价于余弦相似度。
id_map 维护 FAISS 内部索引与 MySQL chunk ID 的映射关系。
"""

import os
import numpy as np
import faiss
from app.core.config import settings


class VectorStore:
    def __init__(self):
        self.index: faiss.Index | None = None  # FAISS 索引对象
        self.id_map: list[int] = []            # FAISS 内部索引 → chunk ID 映射
        self._loaded = False                   # 索引是否已加载

    def build(self, ids: list[int], embeddings: np.ndarray):
        """
        构建 FAISS 索引。
        ids: chunk id 列表
        embeddings: shape (n, dim) 的向量矩阵
        """
        if embeddings.size == 0:
            self.index = None
            self.id_map = []
            self._loaded = True
            return

        dim = embeddings.shape[1]
        index = faiss.IndexFlatIP(dim)
        index.add(embeddings.astype(np.float32))
        self.index = index
        self.id_map = list(ids)
        self._loaded = True
        os.makedirs(settings.FAISS_INDEX_PATH, exist_ok=True)
        faiss.write_index(index, os.path.join(settings.FAISS_INDEX_PATH, "index.faiss"))
        np.save(os.path.join(settings.FAISS_INDEX_PATH, "id_map.npy"), np.array(self.id_map))

    def load(self) -> bool:
        """从磁盘加载已保存的 FAISS 索引文件，加载成功返回 True"""
        index_path = os.path.join(settings.FAISS_INDEX_PATH, "index.faiss")
        id_map_path = os.path.join(settings.FAISS_INDEX_PATH, "id_map.npy")
        if not os.path.exists(index_path) or not os.path.exists(id_map_path):
            return False
        self.index = faiss.read_index(index_path)
        self.id_map = np.load(id_map_path).tolist()
        self._loaded = True
        return True

    def search(self, query_vector: np.ndarray, top_k: int = None) -> tuple[list[int], list[float]]:
        """
        检索最相似的 top_k 个向量。
        返回 (chunk_id 列表, 相似度得分列表)
        """
        if self.index is None or not self.id_map:
            return [], []

        if top_k is None:
            top_k = settings.FAISS_TOP_K

        top_k = min(top_k, len(self.id_map))
        query = query_vector.reshape(1, -1).astype(np.float32)
        scores, indices = self.index.search(query, top_k)

        valid_pairs = [
            (self.id_map[i], float(s))
            for i, s in zip(indices[0], scores[0])
            if i < len(self.id_map)
        ]
        chunk_ids = [p[0] for p in valid_pairs]
        score_list = [p[1] for p in valid_pairs]
        return chunk_ids, score_list

    @property
    def is_loaded(self) -> bool:
        """索引是否已加载到内存"""
        return self._loaded


vector_store = VectorStore()