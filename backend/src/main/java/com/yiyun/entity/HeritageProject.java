package com.yiyun.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 非遗项目实体
 * <p>
 * 对应数据库表 heritage_project，存储非遗项目的完整信息，包括简介、历史、文化内涵、工艺等。
 */
@Data
@TableName("heritage_project")
public class HeritageProject {

    /** 非遗项目 ID（主键，自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 非遗项目名称 */
    private String name;

    /** 所属分类 ID（关联 heritage_category.id） */
    private Long categoryId;

    /** 地域 */
    private String region;

    /** 项目简介 */
    private String description;

    /** 历史背景 */
    private String history;

    /** 文化内涵 */
    private String culturalMeaning;

    /** 工艺与表现形式 */
    private String craft;

    /** 封面图片地址 */
    private String coverUrl;

    /** 状态：0 下架，1 正常 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}