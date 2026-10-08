package com.yiyun.dto;

import lombok.Data;

/**
 * 非遗项目列表查询参数
 */
@Data
public class HeritageProjectListRequest {

    /** 分类 ID（可选筛选） */
    private Long categoryId;

    /** 地域关键字（可选筛选，模糊匹配） */
    private String region;

    /** 页码，默认 1 */
    private Integer pageNum = 1;

    /** 每页条数，默认 10 */
    private Integer pageSize = 10;
}