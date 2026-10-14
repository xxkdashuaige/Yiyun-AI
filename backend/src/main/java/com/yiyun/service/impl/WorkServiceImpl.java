package com.yiyun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yiyun.dto.SourceItem;
import com.yiyun.dto.WorkListRequest;
import com.yiyun.dto.WorkResponse;
import com.yiyun.entity.BizUserWork;
import com.yiyun.entity.BizWorkSource;
import com.yiyun.mapper.BizUserWorkMapper;
import com.yiyun.mapper.BizWorkSourceMapper;
import com.yiyun.service.WorkService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户作品服务实现
 */
@Service
public class WorkServiceImpl implements WorkService {

    private final BizUserWorkMapper workMapper;
    private final BizWorkSourceMapper workSourceMapper;

    public WorkServiceImpl(BizUserWorkMapper workMapper,
                           BizWorkSourceMapper workSourceMapper) {
        this.workMapper = workMapper;
        this.workSourceMapper = workSourceMapper;
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