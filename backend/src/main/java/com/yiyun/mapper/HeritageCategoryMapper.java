package com.yiyun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yiyun.entity.HeritageCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 非遗分类 Mapper
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，自动获得 CRUD 方法。
 */
@Mapper
public interface HeritageCategoryMapper extends BaseMapper<HeritageCategory> {
}