package com.yiyun.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 任务响应
 */
@Data
public class AiTaskResponse {

    /** 任务 ID */
    private Long id;

    /** 任务类型：CHAT / IMAGE / VIDEO */
    private String taskType;

    /** 模型名称 */
    private String model;

    /** 输入内容 */
    private String inputText;

    /** 任务状态：PENDING / RUNNING / SUCCESS / FAILED */
    private String status;

    /** 结果地址 */
    private String resultUrl;

    /** 错误信息 */
    private String errorMessage;

    /** 开始时间 */
    private LocalDateTime startedAt;

    /** 完成时间 */
    private LocalDateTime finishedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;
}