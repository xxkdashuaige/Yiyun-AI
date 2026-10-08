package com.yiyun.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yiyun.common.Result;
import com.yiyun.dto.HeritageProjectDetailResponse;
import com.yiyun.dto.HeritageProjectListItemResponse;
import com.yiyun.dto.HeritageProjectListRequest;
import com.yiyun.entity.HeritageCategory;
import com.yiyun.service.HeritageCategoryService;
import com.yiyun.service.HeritageProjectService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 非遗内容接口控制器
 * <p>
 * 提供非遗分类、项目列表、项目详情等公开接口。
 * 路径 /api/heritage/** 在 SecurityConfig 中已配置为 permitAll，无需登录。
 */
@RestController
@RequestMapping("/api/heritage")
public class HeritageController {

    private final HeritageCategoryService heritageCategoryService;
    private final HeritageProjectService heritageProjectService;

    public HeritageController(HeritageCategoryService heritageCategoryService,
                              HeritageProjectService heritageProjectService) {
        this.heritageCategoryService = heritageCategoryService;
        this.heritageProjectService = heritageProjectService;
    }

    /**
     * 获取所有非遗分类
     *
     * @return 分类列表
     */
    @GetMapping("/categories")
    public Result<List<HeritageCategory>> listCategories() {
        return Result.success(heritageCategoryService.listAll());
    }

    /**
     * 分页查询非遗项目
     *
     * @param request 查询参数（categoryId、region、pageNum、pageSize）
     * @return 分页结果
     */
    @GetMapping("/projects")
    public Result<IPage<HeritageProjectListItemResponse>> listProjects(HeritageProjectListRequest request) {
        return Result.success(heritageProjectService.pageList(request));
    }

    /**
     * 获取非遗项目详情（含知识文档）
     *
     * @param id 项目 ID
     * @return 项目详情
     */
    @GetMapping("/projects/{id}")
    public Result<HeritageProjectDetailResponse> getProjectDetail(@PathVariable Long id) {
        return Result.success(heritageProjectService.detail(id));
    }
}