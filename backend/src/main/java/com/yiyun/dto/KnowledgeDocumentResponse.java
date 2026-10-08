package com.yiyun.dto;

import lombok.Data;

/**
 * 知识文档响应
 */
@Data
public class KnowledgeDocumentResponse {

    /** 文档 ID */
    private Long id;

    /** 标题 */
    private String title;

    /** 知识类型 */
    private String docType;

    /** 正文内容 */
    private String content;

    /** 来源 */
    private String source;

    /** 作者 */
    private String author;
}