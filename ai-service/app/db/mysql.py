"""
MySQL 数据库访问模块
--------------------
提供 ai-service 与 MySQL 数据库的交互方法，主要用于：
1. 读取 knowledge_document 表中的知识文档（用于切分和向量化）
2. 操作 knowledge_chunk 表（增删改查切分后的知识片段）
3. 根据 chunk ID 回查片段及来源文档信息
"""

import pymysql
from pymysql.cursors import DictCursor
from app.core.config import settings


def get_connection():
    """获取 MySQL 数据库连接（返回 DictCursor，结果为字典格式）"""
    return pymysql.connect(
        host=settings.MYSQL_HOST,
        port=settings.MYSQL_PORT,
        user=settings.MYSQL_USER,
        password=settings.MYSQL_PASSWORD,
        database=settings.MYSQL_DATABASE,
        charset="utf8mb4",
        cursorclass=DictCursor,
    )


def fetch_all_knowledge_documents():
    """读取所有状态为正常的知识文档（status=1）"""
    conn = get_connection()
    try:
        with conn.cursor() as cursor:
            sql = """
                SELECT id, heritage_project_id, title, doc_type, content, source, author
                FROM knowledge_document
                WHERE status = 1
            """
            cursor.execute(sql)
            return cursor.fetchall()
    finally:
        conn.close()


def fetch_chunks_by_ids(chunk_ids, heritage_project_id=None):
    """
    根据 chunk ID 列表查询知识片段及其来源文档信息。
    heritage_project_id: 可选，限定只返回指定非遗项目下的片段。
    返回字段包含：片段内容、文档标题、文档类型、来源、作者等。
    """
    if not chunk_ids:
        return []
    conn = get_connection()
    try:
        with conn.cursor() as cursor:
            placeholders = ",".join(["%s"] * len(chunk_ids))
            sql = f"""
                SELECT c.id, c.document_id, c.heritage_project_id, c.chunk_index,
                       c.chunk_text, c.embedding_id,
                       d.title AS document_title, d.doc_type, d.source, d.author
                FROM knowledge_chunk c
                JOIN knowledge_document d ON c.document_id = d.id
                WHERE c.id IN ({placeholders})
            """
            params = list(chunk_ids)
            if heritage_project_id is not None:
                sql += " AND c.heritage_project_id = %s"
                params.append(heritage_project_id)
            cursor.execute(sql, params)
            return cursor.fetchall()
    finally:
        conn.close()


def delete_chunks_by_document_id(document_id):
    """删除指定文档下的所有知识片段（重建索引时先删后插）"""
    conn = get_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute("DELETE FROM knowledge_chunk WHERE document_id = %s", (document_id,))
        conn.commit()
    finally:
        conn.close()


def insert_chunks(chunks):
    """
    批量插入知识片段。
    chunks 为元组列表，每个元组字段顺序：
    (document_id, heritage_project_id, chunk_index, chunk_text, embedding_id, token_count)
    """
    if not chunks:
        return
    conn = get_connection()
    try:
        with conn.cursor() as cursor:
            sql = """
                INSERT INTO knowledge_chunk
                (document_id, heritage_project_id, chunk_index, chunk_text, embedding_id, token_count)
                VALUES (%s, %s, %s, %s, %s, %s)
            """
            cursor.executemany(sql, chunks)
        conn.commit()
    finally:
        conn.close()


def fetch_all_chunks():
    """读取所有知识片段（按 ID 升序），用于构建 FAISS 索引"""
    conn = get_connection()
    try:
        with conn.cursor() as cursor:
            sql = """
                SELECT id, document_id, heritage_project_id, chunk_index, chunk_text, embedding_id
                FROM knowledge_chunk
                ORDER BY id
            """
            cursor.execute(sql)
            return cursor.fetchall()
    finally:
        conn.close()


def update_chunk_embedding_id(chunk_id, embedding_id):
    """更新知识片段的 embedding_id（FAISS 向量索引 ID）"""
    conn = get_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute(
                "UPDATE knowledge_chunk SET embedding_id = %s WHERE id = %s",
                (embedding_id, chunk_id),
            )
        conn.commit()
    finally:
        conn.close()