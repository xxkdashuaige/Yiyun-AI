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
                SELECT id, heritage_project_id, title, doc_type, content,
                       source, source_url, author, publish_date, access_date,
                       page_no, rights_note, verified
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
                       d.title AS document_title, d.doc_type, d.source,
                       d.source_url, d.author, d.publish_date, d.access_date,
                       d.page_no, d.rights_note, d.verified
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


# ==================== 对话会话与消息 ====================

def create_chat_session(user_id, title, heritage_project_id=None):
    """创建新的对话会话，返回会话 ID"""
    conn = get_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute(
                "INSERT INTO chat_session (user_id, title, heritage_project_id) VALUES (%s, %s, %s)",
                (user_id, title, heritage_project_id),
            )
            conn.commit()
            return cursor.lastrowid
    finally:
        conn.close()


def get_chat_session(session_id):
    """根据 ID 查询会话（不限用户，调用方需自行校验归属）"""
    conn = get_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute(
                "SELECT id, user_id, title, heritage_project_id, status, created_at, updated_at "
                "FROM chat_session WHERE id = %s",
                (session_id,),
            )
            return cursor.fetchone()
    finally:
        conn.close()


def list_chat_sessions(user_id):
    """查询用户的所有正常会话（按更新时间倒序）"""
    conn = get_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute(
                "SELECT id, user_id, title, heritage_project_id, status, created_at, updated_at "
                "FROM chat_session WHERE user_id = %s AND status = 1 ORDER BY updated_at DESC",
                (user_id,),
            )
            return cursor.fetchall()
    finally:
        conn.close()


def delete_chat_session(session_id, user_id):
    """软删除会话（仅允许删除本人的会话），返回受影响行数"""
    conn = get_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute(
                "UPDATE chat_session SET status = 0 WHERE id = %s AND user_id = %s",
                (session_id, user_id),
            )
            conn.commit()
            return cursor.rowcount
    finally:
        conn.close()


def get_next_seq_no(session_id):
    """获取会话下一条消息的顺序号"""
    conn = get_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute(
                "SELECT COALESCE(MAX(seq_no), 0) + 1 AS next_seq FROM chat_message WHERE session_id = %s",
                (session_id,),
            )
            row = cursor.fetchone()
            return row["next_seq"] if row else 1
    finally:
        conn.close()


def insert_chat_message(session_id, role, content, sources=None, seq_no=None):
    """插入一条对话消息，返回消息 ID"""
    conn = get_connection()
    try:
        with conn.cursor() as cursor:
            if seq_no is None:
                seq_no = get_next_seq_no(session_id)
            import json as _json
            sources_json = _json.dumps(sources, ensure_ascii=False) if sources else None
            cursor.execute(
                "INSERT INTO chat_message (session_id, role, content, sources, seq_no) "
                "VALUES (%s, %s, %s, %s, %s)",
                (session_id, role, content, sources_json, seq_no),
            )
            conn.commit()
            return cursor.lastrowid
    finally:
        conn.close()


def list_chat_messages(session_id):
    """查询会话的全部消息（按顺序号升序）"""
    conn = get_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute(
                "SELECT id, session_id, role, content, sources, seq_no, created_at "
                "FROM chat_message WHERE session_id = %s ORDER BY seq_no ASC",
                (session_id,),
            )
            rows = cursor.fetchall()
            import json as _json
            for row in rows:
                if row.get("sources"):
                    try:
                        row["sources"] = _json.loads(row["sources"])
                    except (TypeError, ValueError):
                        row["sources"] = None
            return rows
    finally:
        conn.close()


def touch_chat_session(session_id):
    """更新会话的 updated_at 时间戳"""
    conn = get_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute(
                "UPDATE chat_session SET updated_at = CURRENT_TIMESTAMP WHERE id = %s",
                (session_id,),
            )
            conn.commit()
    finally:
        conn.close()