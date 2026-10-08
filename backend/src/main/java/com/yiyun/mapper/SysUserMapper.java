package com.yiyun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yiyun.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 * <p>
 * 继承 MyBatis-Plus 的 BaseMapper，自动获得 CRUD 方法。
 * 如需自定义 SQL，可在此接口定义方法并在 mapper XML 中实现。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}