package com.yiyun.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yiyun.dto.WorkListRequest;
import com.yiyun.dto.WorkResponse;

/**
 * 用户作品服务接口
 */
public interface WorkService {

    /**
     * 分页查询当前用户的作品列表
     */
    IPage<WorkResponse> pageWorks(Long userId, WorkListRequest request);

    /**
     * 查询作品详情（含 RAG 来源）
     */
    WorkResponse getWorkDetail(Long workId, Long userId);

    /**
     * 删除作品（逻辑删除，状态置为 DELETED）
     */
    void deleteWork(Long workId, Long userId);
}