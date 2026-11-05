package com.yiyun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yiyun.dto.HeritageProjectDetailResponse;
import com.yiyun.dto.HeritageProjectListItemResponse;
import com.yiyun.dto.HeritageProjectListRequest;
import com.yiyun.dto.KnowledgeDocumentResponse;
import com.yiyun.entity.HeritageCategory;
import com.yiyun.entity.HeritageProject;
import com.yiyun.entity.KnowledgeDocument;
import com.yiyun.mapper.HeritageCategoryMapper;
import com.yiyun.mapper.HeritageProjectMapper;
import com.yiyun.mapper.KnowledgeDocumentMapper;
import com.yiyun.service.HeritageProjectService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 非遗项目服务实现
 */
@Service
public class HeritageProjectServiceImpl implements HeritageProjectService {

    private final HeritageProjectMapper heritageProjectMapper;
    private final HeritageCategoryMapper heritageCategoryMapper;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    public HeritageProjectServiceImpl(HeritageProjectMapper heritageProjectMapper,
                                      HeritageCategoryMapper heritageCategoryMapper,
                                      KnowledgeDocumentMapper knowledgeDocumentMapper) {
        this.heritageProjectMapper = heritageProjectMapper;
        this.heritageCategoryMapper = heritageCategoryMapper;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
    }

    /**
     * 分页查询非遗项目
     * <p>
     * 通过自定义 SQL 关联分类表，一次性获取分类名称。
     */
    @Override
    public IPage<HeritageProjectListItemResponse> pageList(HeritageProjectListRequest request) {
        Page<HeritageProjectListItemResponse> page = new Page<>(request.getPageNum(), request.getPageSize());
        return heritageProjectMapper.selectProjectPage(page, request.getCategoryId(), request.getRegion());
    }

    /**
     * 查询项目详情
     * <p>
     * 1. 查询项目基本信息
     * 2. 查询分类名称
     * 3. 查询关联的知识文档列表
     * 4. 组装返回
     */
    @Override
    public HeritageProjectDetailResponse detail(Long id) {
        // 1. 查询项目
        HeritageProject project = heritageProjectMapper.selectById(id);
        if (project == null || project.getStatus() != 1) {
            throw new RuntimeException("非遗项目不存在或已下架");
        }

        // 2. 查询分类名称
        HeritageCategory category = heritageCategoryMapper.selectById(project.getCategoryId());
        String categoryName = category != null ? category.getName() : null;

        // 3. 查询知识文档
        List<KnowledgeDocument> documents = knowledgeDocumentMapper.selectList(
                new LambdaQueryWrapper<KnowledgeDocument>()
                        .eq(KnowledgeDocument::getHeritageProjectId, id)
                        .eq(KnowledgeDocument::getStatus, 1)
        );

        // 4. 转换为响应对象
        List<KnowledgeDocumentResponse> docList = documents.stream().map(doc -> {
            KnowledgeDocumentResponse resp = new KnowledgeDocumentResponse();
            resp.setId(doc.getId());
            resp.setTitle(doc.getTitle());
            resp.setDocType(doc.getDocType());
            resp.setContent(doc.getContent());
            resp.setSource(doc.getSource());
            resp.setSourceUrl(doc.getSourceUrl());
            resp.setAuthor(doc.getAuthor());
            resp.setPublishDate(doc.getPublishDate());
            resp.setAccessDate(doc.getAccessDate());
            resp.setPageNo(doc.getPageNo());
            resp.setRightsNote(doc.getRightsNote());
            resp.setVerified(doc.getVerified());
            return resp;
        }).collect(Collectors.toList());

        // 5. 组装详情
        HeritageProjectDetailResponse detail = new HeritageProjectDetailResponse();
        detail.setId(project.getId());
        detail.setName(project.getName());
        detail.setCategoryId(project.getCategoryId());
        detail.setCategoryName(categoryName);
        detail.setRegion(project.getRegion());
        detail.setDescription(project.getDescription());
        detail.setHistory(project.getHistory());
        detail.setCulturalMeaning(project.getCulturalMeaning());
        detail.setCraft(project.getCraft());
        detail.setCoverUrl(project.getCoverUrl());
        detail.setDocuments(docList);
        return detail;
    }
}