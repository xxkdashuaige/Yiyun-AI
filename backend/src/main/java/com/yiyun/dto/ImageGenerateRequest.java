package com.yiyun.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 图像生成请求
 */
@Data
public class ImageGenerateRequest {

    /** 提示词 */
    @NotBlank(message = "提示词不能为空")
    private String prompt;

    /** 风格：ink / oil / watercolor / chinese-painting / realistic */
    private String style;

    /** 尺寸：1:1 / 16:9 / 9:16 / 4:3 */
    private String size;

    /** 生成数量 */
    private Integer count = 1;

    /** 关联非遗项目 ID */
    private Long heritageProjectId;

    /** 参考图地址（图生图时使用） */
    private String referenceImageUrl;
}