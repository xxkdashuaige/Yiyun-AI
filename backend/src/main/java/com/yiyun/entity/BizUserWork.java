package com.yiyun.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户 AI 作品实体
 * <p>
 * 对应数据库表 biz_user_work，存储用户通过 AI 生成的作品记录。
 */
@Data
@TableName("biz_user_work")
public class BizUserWork {

    /** 作品 ID（主键，自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID（关联 sys_user.id） */
    private Long userId;

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

    /** 生成服务商 */
    private String provider;

    /** 生成模型 */
    private String model;

    /** AI 任务 ID（关联 ai_task.id） */
    private Long taskId;

    /** 状态：GENERATING / SUCCESS / FAILED / DELETED */
    private String status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}