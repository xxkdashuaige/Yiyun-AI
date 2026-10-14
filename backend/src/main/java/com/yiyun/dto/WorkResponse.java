package com.yiyun.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 作品详情响应
 */
@Data
public class WorkResponse {

    /** 作品 ID */
    private Long id;

    /** 作品类型：IMAGE / VIDEO */
    private String workType;

    /** 作品名称 */
    private String title;

    /** 用户原始需求 */
    private String prompt;

    /** AI 增强后的 Prompt */
    private String enhancedPrompt;

    /** 作品风格 */
    private String style;

    /** 作品地址 */
    private String resultUrl;

    /** 缩略图地址 */
    private String thumbnailUrl;

    /** 状态 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** RAG 检索来源列表 */
    private List<SourceItem> sources;
}