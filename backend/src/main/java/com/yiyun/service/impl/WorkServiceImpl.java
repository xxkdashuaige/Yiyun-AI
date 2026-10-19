package com.yiyun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiyun.dto.SaveWorkRequest;
import com.yiyun.dto.SourceItem;
import com.yiyun.dto.WorkListRequest;
import com.yiyun.dto.WorkResponse;
import com.yiyun.entity.AiTask;
import com.yiyun.entity.BizUserWork;
import com.yiyun.entity.BizWorkSource;
import com.yiyun.mapper.AiTaskMapper;
import com.yiyun.mapper.BizUserWorkMapper;
import com.yiyun.mapper.BizWorkSourceMapper;
import com.yiyun.service.WorkService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 用户作品服务实现
 */
@Service
public class WorkServiceImpl implements WorkService {

    private final BizUserWorkMapper workMapper;
    private final BizWorkSourceMapper workSourceMapper;
    private final AiTaskMapper aiTaskMapper;
    private final ObjectMapper objectMapper;

    public WorkServiceImpl(BizUserWorkMapper workMapper,
                           BizWorkSourceMapper workSourceMapper,
                           AiTaskMapper aiTaskMapper,
                           ObjectMapper objectMapper) {
        this.workMapper = workMapper;
        this.workSourceMapper = workSourceMapper;
        this.aiTaskMapper = aiTaskMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkResponse saveWork(SaveWorkRequest request, Long userId) {
        AiTask task = aiTaskMapper.selectById(request.getTaskId());
        if (task == null || !task.getUserId().equals(userId)) {
            throw new RuntimeException("任务不存在");
        }
        if (!"SUCCESS".equals(task.getStatus())) {
            throw new RuntimeException("任务未完成，无法保存");
        }

        BizUserWork work = new BizUserWork();
        work.setUserId(userId);
        work.setWorkType("IMAGE");
        work.setTitle(request.getTitle() != null && !request.getTitle().isBlank()
                ? request.getTitle() : "未命名作品");
        work.setPrompt(task.getInputText());
        work.setEnhancedPrompt(task.getPrompt());
        work.setStyle(request.getStyle());
        work.setResultUrl(task.getResultUrl());
        work.setThumbnailUrl(task.getResultUrl());
        work.setProvider(task.getProvider());
        work.setModel(task.getModel());
        work.setTaskId(task.getId());
        work.setStatus("SUCCESS");
        work.setCreatedAt(LocalDateTime.now());
        work.setUpdatedAt(LocalDateTime.now());
        workMapper.insert(work);

        List<SourceItem> sources = parseSources(task.getSourcesJson());
        for (SourceItem item : sources) {
            BizWorkSource ws = new BizWorkSource();
            ws.setWorkId(work.getId());
            ws.setHeritageProjectId(item.getHeritageProjectId() != null
                    ? item.getHeritageProjectId() : request.getHeritageProjectId());
            ws.setSourceType(item.getDocType());
            ws.setSourceTitle(item.getTitle());
            ws.setSourceContent(item.getContent());
            ws.setRetrievalScore(item.getScore());
            ws.setCreatedAt(LocalDateTime.now());
            workSourceMapper.insert(ws);
        }

        WorkResponse resp = new WorkResponse();
        BeanUtils.copyProperties(work, resp);
        resp.setSources(sources);
        return resp;
    }

    @SuppressWarnings("unchecked")
    private List<SourceItem> parseSources(String sourcesJson) {
        if (sourcesJson == null || sourcesJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<SourceItem> list = objectMapper.readValue(sourcesJson, new TypeReference<List<SourceItem>>() {});
            return list != null ? list : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public IPage<WorkResponse> pageWorks(Long userId, WorkListRequest request) {
        Page<BizUserWork> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<BizUserWork> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizUserWork::getUserId, userId)
                .ne(BizUserWork::getStatus, "DELETED")
                .orderByDesc(BizUserWork::getCreatedAt);
        if (request.getWorkType() != null && !request.getWorkType().isBlank()) {
            wrapper.eq(BizUserWork::getWorkType, request.getWorkType());
        }

        IPage<BizUserWork> workPage = workMapper.selectPage(page, wrapper);
        return workPage.convert(this::toResponse);
    }

    @Override
    public WorkResponse getWorkDetail(Long workId, Long userId) {
        BizUserWork work = workMapper.selectById(workId);
        if (work == null || !work.getUserId().equals(userId) || "DELETED".equals(work.getStatus())) {
            throw new RuntimeException("作品不存在");
        }
        WorkResponse response = toResponse(work);

        LambdaQueryWrapper<BizWorkSource> sourceWrapper = new LambdaQueryWrapper<>();
        sourceWrapper.eq(BizWorkSource::getWorkId, workId);
        List<BizWorkSource> sources = workSourceMapper.selectList(sourceWrapper);

        List<SourceItem> sourceItems = new ArrayList<>();
        for (BizWorkSource ws : sources) {
            SourceItem item = new SourceItem();
            item.setHeritageProjectId(ws.getHeritageProjectId());
            item.setTitle(ws.getSourceTitle());
            item.setDocType(ws.getSourceType());
            item.setContent(ws.getSourceContent());
            item.setScore(ws.getRetrievalScore());
            sourceItems.add(item);
        }
        response.setSources(sourceItems);
        return response;
    }

    @Override
    public void deleteWork(Long workId, Long userId) {
        BizUserWork work = workMapper.selectById(workId);
        if (work == null || !work.getUserId().equals(userId)) {
            throw new RuntimeException("作品不存在");
        }
        work.setStatus("DELETED");
        workMapper.updateById(work);
    }

    private WorkResponse toResponse(BizUserWork work) {
        WorkResponse resp = new WorkResponse();
        BeanUtils.copyProperties(work, resp);
        return resp;
    }
}