package com.yiyun.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 生成任务实体
 * <p>
 * 对应数据库表 ai_task，记录用户的 AI 生成请求及执行状态。
 */
@Data
@TableName("ai_task")
public class AiTask {

    /** 任务 ID（主键，自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID（关联 sys_user.id） */
    private Long userId;

    /** 任务类型：CHAT / IMAGE / VIDEO */
    private String taskType;

    /** AI 服务提供商 */
    private String provider;

    /** 模型名称 */
    private String model;

    /** 输入内容 */
    private String inputText;

    /** 生成 Prompt */
    private String prompt;

    /** 任务状态：PENDING / RUNNING / SUCCESS / FAILED */
    private String status;

    /** 结果地址 */
    private String resultUrl;

    /** 错误信息 */
    private String errorMessage;

    /** RAG 来源 JSON（保存作品时关联写入 biz_work_source） */
    private String sourcesJson;

    /** 开始时间 */
    private LocalDateTime startedAt;

    /** 完成时间 */
    private LocalDateTime finishedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}