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
import com.yiyun.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI 生成服务实现
 */
@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    private final AiServiceClient aiServiceClient;
    private final AiTaskMapper aiTaskMapper;
    private final ObjectMapper objectMapper;

    @Value("${ai.image.provider:dashscope}")
    private String imageProvider;

    @Value("${ai.image.model:qwen-image-3.0-pro}")
    private String imageModel;

    /** 外部任务轮询间隔（毫秒） */
    private static final long POLL_INTERVAL_MS = 3000L;
    /** 外部任务最大轮询次数（约 10 分钟） */
    private static final int MAX_POLL_TIMES = 200;

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

        // 在请求线程中提取 Token，因为异步线程无法访问 RequestContextHolder
        String token = SecurityUtil.getCurrentToken();

        CompletableFuture.runAsync(() -> {
            try {
                aiServiceClient.chatStream(request, emitter, token,
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
        task.setProvider(imageProvider);
        task.setModel(imageModel);
        task.setInputText(request.getPrompt());
        task.setPrompt(buildImagePrompt(request));
        task.setStatus("RUNNING");
        task.setStartedAt(LocalDateTime.now());
        task.setSourcesJson(buildSourcesJson(request));

        // 提交到 AI 服务（DashScope 异步任务），获取外部任务 ID
        String providerTaskId = aiServiceClient.submitImageTask(
                request.getPrompt(), request.getStyle(), request.getSize(), request.getCount());
        task.setProviderTaskId(providerTaskId);

        aiTaskMapper.insert(task);

        Long taskId = task.getId();

        // 异步轮询外部任务状态，直到完成或失败
        CompletableFuture.runAsync(() -> pollImageTask(taskId, providerTaskId));

        AiTaskResponse resp = new AiTaskResponse();
        BeanUtils.copyProperties(task, resp);
        return resp;
    }

    /**
     * 轮询 DashScope 图像生成任务状态，更新数据库。
     */
    private void pollImageTask(Long taskId, String providerTaskId) {
        for (int i = 0; i < MAX_POLL_TIMES; i++) {
            try {
                Thread.sleep(POLL_INTERVAL_MS);
                Map<String, Object> status = aiServiceClient.getImageTaskStatus(providerTaskId);
                String taskStatus = (String) status.get("status");

                if ("SUCCEEDED".equals(taskStatus)) {
                    AiTask update = new AiTask();
                    update.setId(taskId);
                    update.setStatus("SUCCESS");
                    update.setResultUrl((String) status.get("image_url"));
                    update.setFinishedAt(LocalDateTime.now());
                    aiTaskMapper.updateById(update);
                    log.info("图像任务 {} 生成成功", taskId);
                    return;
                } else if ("FAILED".equals(taskStatus)) {
                    AiTask update = new AiTask();
                    update.setId(taskId);
                    update.setStatus("FAILED");
                    update.setErrorMessage((String) status.get("error"));
                    update.setFinishedAt(LocalDateTime.now());
                    aiTaskMapper.updateById(update);
                    log.warn("图像任务 {} 生成失败: {}", taskId, status.get("error"));
                    return;
                }
                // RUNNING 状态继续轮询
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                markTaskFailed(taskId, "生成被中断");
                return;
            } catch (Exception e) {
                log.error("轮询图像任务 {} 状态异常: {}", taskId, e.getMessage());
                markTaskFailed(taskId, "查询任务状态异常: " + e.getMessage());
                return;
            }
        }
        // 超过最大轮询次数仍未完成
        markTaskFailed(taskId, "生成超时");
    }

    private void markTaskFailed(Long taskId, String errorMsg) {
        AiTask update = new AiTask();
        update.setId(taskId);
        update.setStatus("FAILED");
        update.setErrorMessage(errorMsg);
        update.setFinishedAt(LocalDateTime.now());
        aiTaskMapper.updateById(update);
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

    @Override
    public List<Map<String, Object>> listSessions() {
        return aiServiceClient.listSessions();
    }

    @Override
    public List<Map<String, Object>> getSessionMessages(Long sessionId) {
        return aiServiceClient.getSessionMessages(sessionId);
    }

    @Override
    public void deleteSession(Long sessionId) {
        aiServiceClient.deleteSession(sessionId);
    }
}