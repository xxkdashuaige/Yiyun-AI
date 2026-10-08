package com.yiyun.dto;

import lombok.Data;

import java.util.List;

/**
 * 非遗项目详情响应
 * <p>
 * 包含项目完整信息以及关联的知识文档列表。
 */
@Data
public class HeritageProjectDetailResponse {

    /** 项目 ID */
    private Long id;

    /** 项目名称 */
    private String name;

    /** 分类 ID */
    private Long categoryId;

    /** 分类名称 */
    private String categoryName;

    /** 地域 */
    private String region;

    /** 项目简介 */
    private String description;

    /** 历史背景 */
    private String history;

    /** 文化内涵 */
    private String culturalMeaning;

    /** 工艺与表现形式 */
    private String craft;

    /** 封面图片地址 */
    private String coverUrl;

    /** 关联的知识文档列表 */
    private List<KnowledgeDocumentResponse> documents;
}