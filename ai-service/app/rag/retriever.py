"""
RAG 检索核心模块
-----------------
负责知识片段的切分入库、向量化、FAISS 索引构建，以及用户查询时的语义检索。

流程：
1. build_index(): 读取 knowledge_document → 切分 → 写入 knowledge_chunk → 向量化 → 构建 FAISS 索引
2. retrieve(query): 查询向量化 → FAISS 相似度检索 → 回查 MySQL 获取片段及来源
"""

import numpy as np
from app.rag.chunker import split_text_into_chunks
from app.rag.embedder import embed_texts, embed_query
from app.rag.vector_store import vector_store
from app.db import mysql


def build_index():
    """
    全量构建 FAISS 索引：
    1. 读取所有 knowledge_document
    2. 切分并写入 knowledge_chunk
    3. 生成 embedding
    4. 构建 FAISS 索引
    """
    documents = mysql.fetch_all_knowledge_documents()

    all_chunks_data = []
    for doc in documents:
        doc_id = doc["id"]
        heritage_project_id = doc["heritage_project_id"]
        content = doc["content"]

        mysql.delete_chunks_by_document_id(doc_id)

        chunks = split_text_into_chunks(content)
        for idx, chunk_text in enumerate(chunks):
            all_chunks_data.append((
                doc_id,
                heritage_project_id,
                idx,
                chunk_text,
                None,
                None,
            ))

    mysql.insert_chunks(all_chunks_data)

    chunks = mysql.fetch_all_chunks()
    if not chunks:
        vector_store.build([], np.array([]))
        return

    chunk_ids = [c["id"] for c in chunks]
    texts = [c["chunk_text"] for c in chunks]

    embeddings = embed_texts(texts)
    vector_store.build(chunk_ids, embeddings)

    for c in chunks:
        mysql.update_chunk_embedding_id(c["id"], str(c["id"]))


def ensure_index_loaded():
    """
    确保 FAISS 索引已加载。如果索引文件存在则加载，否则构建。
    """
    if vector_store.is_loaded:
        return
    if not vector_store.load():
        build_index()


def retrieve(query: str, heritage_project_id: int = None, top_k: int = None) -> list[dict]:
    """
    根据用户查询检索相关知识片段。
    heritage_project_id: 限定只检索指定非遗项目下的知识（可选）。
    返回包含片段信息及来源文档的列表。
    """
    ensure_index_loaded()

    query_vector = embed_query(query)
    chunk_ids, scores = vector_store.search(query_vector, top_k)

    if not chunk_ids:
        return []

    rows = mysql.fetch_chunks_by_ids(chunk_ids, heritage_project_id=heritage_project_id)

    score_map = {cid: s for cid, s in zip(chunk_ids, scores)}

    results = []
    for row in rows:
        results.append({
            "chunk_id": row["id"],
            "document_id": row["document_id"],
            "heritage_project_id": row["heritage_project_id"],
            "chunk_index": row["chunk_index"],
            "content": row["chunk_text"],
            "title": row.get("document_title", ""),
            "doc_type": row.get("doc_type"),
            "source": row.get("source"),
            "source_url": row.get("source_url"),
            "author": row.get("author"),
            "publish_date": row.get("publish_date"),
            "access_date": row.get("access_date"),
            "page_no": row.get("page_no"),
            "rights_note": row.get("rights_note"),
            "verified": row.get("verified"),
            "score": score_map.get(row["id"], 0.0),
        })

    results.sort(key=lambda x: x["score"], reverse=True)
    return results