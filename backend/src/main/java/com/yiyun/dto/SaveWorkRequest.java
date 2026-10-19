package com.yiyun.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 保存作品请求
 */
@Data
public class SaveWorkRequest {

    /** AI 任务 ID */
    @NotNull(message = "任务 ID 不能为空")
    private Long taskId;

    /** 作品名称 */
    private String title;

    /** 作品风格 */
    private String style;

    /** 关联非遗项目 ID */
    private Long heritageProjectId;
}