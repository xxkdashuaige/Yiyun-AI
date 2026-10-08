package com.yiyun.service;

import com.yiyun.entity.HeritageCategory;

import java.util.List;

/**
 * 非遗分类服务接口
 */
public interface HeritageCategoryService {

    /**
     * 查询所有启用状态的分类（按 sortNum 升序）
     *
     * @return 分类列表
     */
    List<HeritageCategory> listAll();
}