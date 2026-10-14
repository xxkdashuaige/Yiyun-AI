package com.yiyun.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 非遗知识片段实体
 * <p>
 * 对应数据库表 knowledge_chunk，存储切分后的知识片段及其向量索引，用于 RAG 语义检索。
 */
@Data
@TableName("knowledge_chunk")
public class KnowledgeChunk {

    /** 知识片段 ID（主键，自增，同时作为 FAISS 的 embedding_id） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属知识文档 ID（关联 knowledge_document.id） */
    private Long documentId;

    /** 所属非遗项目 ID（冗余字段，便于按项目检索） */
    private Long heritageProjectId;

    /** 片段在原文中的序号 */
    private Integer chunkIndex;

    /** 片段文本内容 */
    private String chunkText;

    /** FAISS 向量索引 ID（对应 chunk.id） */
    private String embeddingId;

    /** 片段 Token 数量 */
    private Integer tokenCount;

    /** 创建时间 */
    private LocalDateTime createdAt;
}