package com.yiyun.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yiyun.dto.AiChatRequest;
import com.yiyun.dto.AiChatResponse;
import com.yiyun.dto.AiTaskResponse;
import com.yiyun.dto.ImageGenerateRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

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
     * 流式文本对话（SSE）
     *
     * @param request 用户请求
     * @param userId  当前用户 ID
     * @return SseEmitter 用于向前端推送流式事件
     */
    SseEmitter chatStream(AiChatRequest request, Long userId);

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

    /**
     * 获取当前用户的会话列表
     */
    List<Map<String, Object>> listSessions();

    /**
     * 获取指定会话的消息列表
     */
    List<Map<String, Object>> getSessionMessages(Long sessionId);

    /**
     * 删除会话
     */
    void deleteSession(Long sessionId);
}