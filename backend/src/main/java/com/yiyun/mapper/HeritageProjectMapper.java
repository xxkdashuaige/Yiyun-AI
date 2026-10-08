package com.yiyun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yiyun.dto.HeritageProjectListItemResponse;
import com.yiyun.entity.HeritageProject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 非遗项目 Mapper
 * <p>
 * 继承 BaseMapper，额外提供分页关联查询（关联分类表获取分类名称）。
 */
@Mapper
public interface HeritageProjectMapper extends BaseMapper<HeritageProject> {

    /**
     * 分页查询非遗项目列表，关联分类表获取分类名称
     *
     * @param page       MyBatis-Plus 分页对象
     * @param categoryId 分类 ID（可选）
     * @param region     地域关键字（可选）
     * @return 分页结果
     */
    @Select("<script>" +
            "SELECT p.id, p.name, p.category_id AS categoryId, c.name AS categoryName, " +
            "p.region, p.description, p.cover_url AS coverUrl " +
            "FROM heritage_project p " +
            "LEFT JOIN heritage_category c ON p.category_id = c.id " +
            "WHERE p.status = 1 " +
            "<if test='categoryId != null'> AND p.category_id = #{categoryId} </if>" +
            "<if test='region != null and region != \"\"'> AND p.region LIKE CONCAT('%', #{region}, '%') </if>" +
            "ORDER BY p.id DESC" +
            "</script>")
    IPage<HeritageProjectListItemResponse> selectProjectPage(IPage<HeritageProjectListItemResponse> page,
                                                              @Param("categoryId") Long categoryId,
                                                              @Param("region") String region);
}