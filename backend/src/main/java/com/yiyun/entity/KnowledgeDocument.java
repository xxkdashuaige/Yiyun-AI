package com.yiyun.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 非遗知识文档实体
 * <p>
 * 对应数据库表 knowledge_document，存储非遗项目的详细知识内容，用于 RAG 检索。
 */
@Data
@TableName("knowledge_document")
public class KnowledgeDocument {

    /** 知识文档 ID（主键，自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属非遗项目 ID（关联 heritage_project.id） */
    private Long heritageProjectId;

    /** 知识标题 */
    private String title;

    /** 知识类型（如"文化内涵"、"工艺技法"、"民俗"等） */
    private String docType;

    /** 知识正文 */
    private String content;

    /** 知识来源名称 */
    private String source;

    /** 知识来源链接 */
    private String sourceUrl;

    /** 作者 */
    private String author;

    /** 发布日期 */
    private LocalDate publishDate;

    /** 访问日期 */
    private LocalDate accessDate;

    /** 页码 */
    private String pageNo;

    /** 版权声明 */
    private String rightsNote;

    /** 是否已核验：0 否，1 是 */
    private Integer verified;

    /** 状态：0 禁用，1 正常 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}