package com.yiyun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yiyun.entity.HeritageCategory;
import com.yiyun.mapper.HeritageCategoryMapper;
import com.yiyun.service.HeritageCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 非遗分类服务实现
 */
@Service
public class HeritageCategoryServiceImpl implements HeritageCategoryService {

    private final HeritageCategoryMapper heritageCategoryMapper;

    public HeritageCategoryServiceImpl(HeritageCategoryMapper heritageCategoryMapper) {
        this.heritageCategoryMapper = heritageCategoryMapper;
    }

    /**
     * 查询所有启用的分类，按 sortNum 升序排列
     */
    @Override
    public List<HeritageCategory> listAll() {
        return heritageCategoryMapper.selectList(
                new LambdaQueryWrapper<HeritageCategory>()
                        .eq(HeritageCategory::getStatus, 1)
                        .orderByAsc(HeritageCategory::getSortNum)
        );
    }
}