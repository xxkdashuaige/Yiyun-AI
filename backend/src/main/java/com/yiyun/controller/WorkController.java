package com.yiyun.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yiyun.common.Result;
import com.yiyun.dto.SaveWorkRequest;
import com.yiyun.dto.WorkListRequest;
import com.yiyun.dto.WorkResponse;
import com.yiyun.service.WorkService;
import com.yiyun.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 用户作品接口控制器
 * <p>
 * 提供作品保存、列表、详情、删除等接口，需登录后访问。
 */
@RestController
@RequestMapping("/api/works")
public class WorkController {

    private final WorkService workService;

    public WorkController(WorkService workService) {
        this.workService = workService;
    }

    /**
     * 保存 AI 生成的作品
     */
    @PostMapping
    public Result<WorkResponse> saveWork(@Valid @RequestBody SaveWorkRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(workService.saveWork(request, userId));
    }

    /**
     * 分页查询当前用户的作品列表
     */
    @GetMapping
    public Result<IPage<WorkResponse>> listWorks(WorkListRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(workService.pageWorks(userId, request));
    }

    /**
     * 查询作品详情（含 RAG 来源）
     */
    @GetMapping("/{id}")
    public Result<WorkResponse> getWork(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(workService.getWorkDetail(id, userId));
    }

    /**
     * 删除作品（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteWork(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        workService.deleteWork(id, userId);
        return Result.success();
    }
}