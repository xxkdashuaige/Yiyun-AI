package com.yiyun.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yiyun.dto.HeritageProjectDetailResponse;
import com.yiyun.dto.HeritageProjectListItemResponse;
import com.yiyun.dto.HeritageProjectListRequest;

/**
 * 非遗项目服务接口
 */
public interface HeritageProjectService {

    /**
     * 分页查询非遗项目列表
     *
     * @param request 查询参数
     * @return 分页结果
     */
    IPage<HeritageProjectListItemResponse> pageList(HeritageProjectListRequest request);

    /**
     * 查询非遗项目详情（含知识文档）
     *
     * @param id 项目 ID
     * @return 项目详情
     */
    HeritageProjectDetailResponse detail(Long id);
}