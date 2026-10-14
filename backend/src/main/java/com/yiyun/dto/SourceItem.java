package com.yiyun.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * RAG 检索来源信息
 */
@Data
public class SourceItem {

    /** 知识片段 ID */
    private Long chunkId;

    /** 知识文档 ID */
    private Long documentId;

    /** 非遗项目 ID */
    private Long heritageProjectId;

    /** 文档标题 */
    private String title;

    /** 文档类型 */
    private String docType;

    /** 来源 */
    private String source;

    /** 片段内容 */
    private String content;

    /** 检索得分 */
    private BigDecimal score;
}