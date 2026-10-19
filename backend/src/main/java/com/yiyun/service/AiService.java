package com.yiyun.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yiyun.dto.AiChatRequest;
import com.yiyun.dto.AiChatResponse;
import com.yiyun.dto.AiTaskResponse;
import com.yiyun.dto.ImageGenerateRequest;

/**
 * AI 生成服务接口
 */
public interface AiService {

    /**
     * 文本对话（同步调用 ai-service）
     *
     * @param request 用户请求
     * @param userId  当前用户 ID
     * @return AI 回答及来源
     */
    AiChatResponse chat(AiChatRequest request, Long userId);

    /**
     * 提交图像生成任务（异步）
     *
     * @param request 生成请求
     * @param userId  当前用户 ID
     * @return 任务信息（状态为 RUNNING）
     */
    AiTaskResponse generateImage(ImageGenerateRequest request, Long userId);

    /**
     * 分页查询当前用户的 AI 任务列表
     */
    IPage<AiTaskResponse> pageTasks(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 查询任务详情
     */
    AiTaskResponse getTask(Long taskId, Long userId);
}