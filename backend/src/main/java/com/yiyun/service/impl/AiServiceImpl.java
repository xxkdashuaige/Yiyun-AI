package com.yiyun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yiyun.dto.AiChatRequest;
import com.yiyun.dto.AiChatResponse;
import com.yiyun.dto.AiTaskResponse;
import com.yiyun.entity.AiTask;
import com.yiyun.mapper.AiTaskMapper;
import com.yiyun.service.AiService;
import com.yiyun.service.AiServiceClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * AI 生成服务实现
 */
@Service
public class AiServiceImpl implements AiService {

    private final AiServiceClient aiServiceClient;
    private final AiTaskMapper aiTaskMapper;

    public AiServiceImpl(AiServiceClient aiServiceClient,
                         AiTaskMapper aiTaskMapper) {
        this.aiServiceClient = aiServiceClient;
        this.aiTaskMapper = aiTaskMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatResponse chat(AiChatRequest request, Long userId) {
        AiTask task = new AiTask();
        task.setUserId(userId);
        task.setTaskType("CHAT");
        task.setProvider("deepseek");
        task.setModel("deepseek-chat");
        task.setInputText(request.getMessage());
        task.setStatus("RUNNING");
        task.setStartedAt(LocalDateTime.now());
        aiTaskMapper.insert(task);

        try {
            AiChatResponse response = aiServiceClient.chat(request);

            task.setStatus("SUCCESS");
            task.setFinishedAt(LocalDateTime.now());
            aiTaskMapper.updateById(task);

            response.setTaskId(task.getId());
            return response;
        } catch (Exception e) {
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            task.setFinishedAt(LocalDateTime.now());
            aiTaskMapper.updateById(task);
            throw new RuntimeException("AI 生成失败：" + e.getMessage(), e);
        }
    }

    @Override
    public IPage<AiTaskResponse> pageTasks(Long userId, Integer pageNum, Integer pageSize) {
        Page<AiTask> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AiTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiTask::getUserId, userId)
                .orderByDesc(AiTask::getCreatedAt);
        IPage<AiTask> taskPage = aiTaskMapper.selectPage(page, wrapper);

        return taskPage.convert(task -> {
            AiTaskResponse resp = new AiTaskResponse();
            BeanUtils.copyProperties(task, resp);
            return resp;
        });
    }

    @Override
    public AiTaskResponse getTask(Long taskId, Long userId) {
        AiTask task = aiTaskMapper.selectById(taskId);
        if (task == null || !task.getUserId().equals(userId)) {
            throw new RuntimeException("任务不存在");
        }
        AiTaskResponse resp = new AiTaskResponse();
        BeanUtils.copyProperties(task, resp);
        return resp;
    }
}