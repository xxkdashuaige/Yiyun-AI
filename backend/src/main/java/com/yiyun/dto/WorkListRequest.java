package com.yiyun.dto;

import lombok.Data;

/**
 * 作品列表查询参数
 */
@Data
public class WorkListRequest {

    /** 作品类型：IMAGE / VIDEO（可选筛选） */
    private String workType;

    /** 页码，默认 1 */
    private Integer pageNum = 1;

    /** 每页条数，默认 10 */
    private Integer pageSize = 10;
}