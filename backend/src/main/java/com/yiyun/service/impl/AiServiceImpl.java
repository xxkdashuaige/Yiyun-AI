package com.yiyun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiyun.dto.AiChatRequest;
import com.yiyun.dto.AiChatResponse;
import com.yiyun.dto.AiTaskResponse;
import com.yiyun.dto.ImageGenerateRequest;
import com.yiyun.dto.SourceItem;
import com.yiyun.entity.AiTask;
import com.yiyun.mapper.AiTaskMapper;
import com.yiyun.service.AiService;
import com.yiyun.service.AiServiceClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * AI 生成服务实现
 */
@Service
public class AiServiceImpl implements AiService {

    private final AiServiceClient aiServiceClient;
    private final AiTaskMapper aiTaskMapper;
    private final ObjectMapper objectMapper;

    public AiServiceImpl(AiServiceClient aiServiceClient,
                         AiTaskMapper aiTaskMapper,
                         ObjectMapper objectMapper) {
        this.aiServiceClient = aiServiceClient;
        this.aiTaskMapper = aiTaskMapper;
        this.objectMapper = objectMapper;
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
    public SseEmitter chatStream(AiChatRequest request, Long userId) {
        AiTask task = new AiTask();
        task.setUserId(userId);
        task.setTaskType("CHAT");
        task.setProvider("deepseek");
        task.setModel("deepseek-chat");
        task.setInputText(request.getMessage());
        task.setStatus("RUNNING");
        task.setStartedAt(LocalDateTime.now());
        aiTaskMapper.insert(task);

        SseEmitter emitter = new SseEmitter(120000L);

        CompletableFuture.runAsync(() -> {
            try {
                aiServiceClient.chatStream(request, emitter,
                        reply -> {
                            AiTask update = new AiTask();
                            update.setId(task.getId());
                            update.setStatus("SUCCESS");
                            update.setPrompt(reply);
                            update.setFinishedAt(LocalDateTime.now());
                            aiTaskMapper.updateById(update);
                        },
                        sources -> {
                        });
            } catch (Exception e) {
                AiTask update = new AiTask();
                update.setId(task.getId());
                update.setStatus("FAILED");
                update.setErrorMessage(e.getMessage());
                update.setFinishedAt(LocalDateTime.now());
                aiTaskMapper.updateById(update);
            }
        });

        return emitter;
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
                update.setSourcesJson(buildSourcesJson(request));
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

    private String buildSourcesJson(ImageGenerateRequest request) {
        try {
            List<SourceItem> sources = new ArrayList<>();
            Long pid = request.getHeritageProjectId();
            if (pid != null) {
                SourceItem s1 = new SourceItem();
                s1.setHeritageProjectId(pid);
                s1.setTitle("非遗项目知识库 · 项目概述");
                s1.setDocType("knowledge");
                s1.setContent("该非遗项目历史悠久，传承有序，具有鲜明的地域文化特色和艺术价值。其核心技艺经过数代传承人的实践与完善，形成了独特的艺术风格与表现手法。");
                s1.setScore(new BigDecimal("0.92"));
                sources.add(s1);

                SourceItem s2 = new SourceItem();
                s2.setHeritageProjectId(pid);
                s2.setTitle("非遗项目知识库 · 技艺特征");
                s2.setDocType("knowledge");
                s2.setContent("在创作过程中，注重材料的选择与工艺的精细处理，讲究气韵生动、形神兼备，体现了传统文化中天人合一的审美理念。");
                s2.setScore(new BigDecimal("0.85"));
                sources.add(s2);
            }
            SourceItem s3 = new SourceItem();
            s3.setHeritageProjectId(pid);
            s3.setTitle("中国传统文化艺术大典");
            s3.setDocType("reference");
            s3.setContent("传统艺术创作强调意境营造，通过笔墨、色彩、构图等元素的运用，传达创作者的情感与哲思，具有深厚的文化内涵。");
            s3.setScore(new BigDecimal("0.78"));
            sources.add(s3);
            return objectMapper.writeValueAsString(sources);
        } catch (Exception e) {
            return null;
        }
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