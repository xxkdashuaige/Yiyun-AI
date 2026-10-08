package com.yiyun.dto;

import lombok.Data;

/**
 * 非遗项目列表项响应
 * <p>
 * 用于列表展示，只包含基本信息，不包含大段文字内容。
 */
@Data
public class HeritageProjectListItemResponse {

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

    /** 封面图片地址 */
    private String coverUrl;
}