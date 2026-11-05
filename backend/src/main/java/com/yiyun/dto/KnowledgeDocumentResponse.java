package com.yiyun.dto;

import lombok.Data;

import java.time.LocalDate;

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

    /** 来源名称 */
    private String source;

    /** 来源链接 */
    private String sourceUrl;

    /** 作者 */
    private String author;

    /** 发布日期 */
    private LocalDate publishDate;

    /** 访问日期 */
    private LocalDate accessDate;

    /** 页码 */
    private String pageNo;

    /** 版权声明 */
    private String rightsNote;

    /** 是否已核验：0 否，1 是 */
    private Integer verified;
}