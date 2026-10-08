package com.yiyun.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 非遗分类实体
 * <p>
 * 对应数据库表 heritage_category，存储非遗项目的分类信息（如传统美术、传统技艺等）。
 */
@Data
@TableName("heritage_category")
public class HeritageCategory {

    /** 分类 ID（主键，自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类名称（唯一，如"传统技艺"） */
    private String name;

    /** 分类描述 */
    private String description;

    /** 排序值，数值越小越靠前 */
    private Integer sortNum;

    /** 状态：0 禁用，1 正常 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}