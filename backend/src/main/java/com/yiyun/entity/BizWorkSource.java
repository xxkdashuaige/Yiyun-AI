package com.yiyun.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 作品知识来源实体
 * <p>
 * 对应数据库表 biz_work_source，记录 RAG 检索到的知识来源，用于前端展示引用信息。
 */
@Data
@TableName("biz_work_source")
public class BizWorkSource {

    /** 来源 ID（主键，自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作品 ID（关联 biz_user_work.id） */
    private Long workId;

    /** 非遗项目 ID（关联 heritage_project.id） */
    private Long heritageProjectId;

    /** 来源类型 */
    private String sourceType;

    /** 来源标题 */
    private String sourceTitle;

    /** 检索到的知识内容 */
    private String sourceContent;

    /** RAG 检索得分 */
    private BigDecimal retrievalScore;

    /** 创建时间 */
    private LocalDateTime createdAt;
}