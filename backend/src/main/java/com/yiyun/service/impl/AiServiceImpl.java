package com.yiyun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yiyun.dto.AiChatRequest;
import com.yiyun.dto.AiChatResponse;
import com.yiyun.dto.AiTaskResponse;
import com.yiyun.dto.ImageGenerateRequest;
import com.yiyun.entity.AiTask;
import com.yiyun.mapper.AiTaskMapper;
import com.yiyun.service.AiService;
import com.yiyun.service.AiServiceClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

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
    @Transactional(rollbackFor = Exception.class)
    public AiTaskResponse generateImage(ImageGenerateRequest request, Long userId) {
        AiTask task = new AiTask();
        task.setUserId(userId);
        task.setTaskType("IMAGE");
        task.setProvider("mock");
        task.setModel("mock-image-v1");
        task.setInputText(request.getPrompt());
        task.setPrompt(buildImagePrompt(request));
        task.setStatus("RUNNING");
        task.setStartedAt(LocalDateTime.now());
        aiTaskMapper.insert(task);

        Long taskId = task.getId();

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(3000L);

                AiTask update = new AiTask();
                update.setId(taskId);
                update.setStatus("SUCCESS");
                update.setResultUrl(buildPlaceholderImageUrl(request.getStyle()));
                update.setFinishedAt(LocalDateTime.now());
                aiTaskMapper.updateById(update);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                AiTask update = new AiTask();
                update.setId(taskId);
                update.setStatus("FAILED");
                update.setErrorMessage("生成被中断");
                update.setFinishedAt(LocalDateTime.now());
                aiTaskMapper.updateById(update);
            } catch (Exception e) {
                AiTask update = new AiTask();
                update.setId(taskId);
                update.setStatus("FAILED");
                update.setErrorMessage(e.getMessage());
                update.setFinishedAt(LocalDateTime.now());
                aiTaskMapper.updateById(update);
            }
        });

        AiTaskResponse resp = new AiTaskResponse();
        BeanUtils.copyProperties(task, resp);
        return resp;
    }

    private String buildImagePrompt(ImageGenerateRequest request) {
        StringBuilder sb = new StringBuilder(request.getPrompt());
        if (request.getStyle() != null && !request.getStyle().isBlank()) {
            sb.append("，风格：").append(request.getStyle());
        }
        if (request.getSize() != null && !request.getSize().isBlank()) {
            sb.append("，尺寸：").append(request.getSize());
        }
        return sb.toString();
    }

    private String buildPlaceholderImageUrl(String style) {
        String bg = "#E7E0D5";
        String fg = "#782F35";
        if (style != null) {
            switch (style) {
                case "ink" -> { bg = "#2C2C2C"; fg = "#F7F4EE"; }
                case "oil" -> { bg = "#5C4A3A"; fg = "#E8C4A0"; }
                case "watercolor" -> { bg = "#A8C5DA"; fg = "#2C3E50"; }
                case "chinese-painting" -> { bg = "#F0E6D2"; fg = "#6B4423"; }
                case "realistic" -> { bg = "#8B7355"; fg = "#F5E6D3"; }
                default -> { }
            }
        }
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='768' height='768' viewBox='0 0 768 768'>"
                + "<rect width='768' height='768' fill='" + bg + "'/>"
                + "<text x='384' y='384' font-family='serif' font-size='32' fill='" + fg + "' text-anchor='middle' dominant-baseline='middle'>遗韵 · AI 生成</text>"
                + "</svg>";
        return "data:image/svg+xml;base64," + java.util.Base64.getEncoder().encodeToString(svg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
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