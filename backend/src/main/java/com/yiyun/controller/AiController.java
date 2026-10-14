package com.yiyun.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yiyun.common.Result;
import com.yiyun.dto.AiChatRequest;
import com.yiyun.dto.AiChatResponse;
import com.yiyun.dto.AiTaskResponse;
import com.yiyun.service.AiService;
import com.yiyun.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * AI 生成接口控制器
 * <p>
 * 提供文本对话、任务查询等接口，需登录后访问。
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * AI 文本对话
     *
     * @param request 对话请求
     * @return AI 回答及 RAG 来源
     */
    @PostMapping("/chat")
    public Result<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(aiService.chat(request, userId));
    }

    /**
     * 分页查询当前用户的 AI 任务
     */
    @GetMapping("/tasks")
    public Result<IPage<AiTaskResponse>> listTasks(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(aiService.pageTasks(userId, pageNum, pageSize));
    }

    /**
     * 查询 AI 任务详情
     */
    @GetMapping("/tasks/{id}")
    public Result<AiTaskResponse> getTask(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(aiService.getTask(id, userId));
    }
}