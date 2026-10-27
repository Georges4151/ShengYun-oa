package com.ceams.dao;

import com.ceams.model.entity.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProjectMapper {
    /**
     * 根据ID查询项目
     * 
     * @param id 项目ID
     * @return 项目信息
     */
    Project selectById(Integer id);

    /**
     * 根据状态查询项目
     * 
     * @param status 项目状态
     * @return 项目列表
     */
    List<Project> selectByStatus(@Param("status") String status);

    /**
     * 根据客户名称查询项目
     * 
     * @param clientName 客户名称
     * @return 项目列表
     */
    List<Project> selectByClient(@Param("clientName") String clientName);

    /**
     * 获取项目统计信息
     * 
     * @return 统计信息
     */
    Map<String, Object> getProjectStatistics();

    /**
     * 获取项目成本分析
     * 
     * @return 成本分析列表
     */
    List<Map<String, Object>> getProjectCostAnalysis();

    /**
     * 插入项目
     * 
     * @param project 项目信息
     * @return 影响行数
     */
    int insert(Project project);

    /**
     * 更新项目
     * 
     * @param project 项目信息
     * @return 影响行数
     */
    int update(Project project);

    /**
     * 删除项目
     * 
     * @param id 项目ID
     * @return 影响行数
     */
    int deleteById(Integer id);

    /**
     * 查询所有项目
     * 
     * @return 项目列表
     */
    List<Project> selectAll();

    /**
     * 根据关键字搜索项目
     * 
     * @param keyword 搜索关键字
     * @return 项目列表
     */
    List<Project> selectByKeyword(@Param("keyword") String keyword);

    /**
     * 分页查询所有项目
     * 
     * @param pagination 分页参数
     * @return 项目列表
     */
    List<Project> selectByPage(@Param("pagination") com.ceams.model.Pagination pagination);

    /**
     * 统计所有项目数量
     * 
     * @return 项目数量
     */
    long count();

    /**
     * 分页根据关键字搜索项目
     * 
     * @param pagination 分页参数
     * @return 项目列表
     */
    List<Project> selectByKeywordAndPage(@Param("pagination") com.ceams.model.Pagination pagination);

    /**
     * 统计根据关键字搜索的项目数量
     * 
     * @param keyword 搜索关键字
     * @return 项目数量
     */
    long countByKeyword(@Param("keyword") String keyword);

    /**
     * 查询最近的项目
     * 
     * @param limit 限制数量
     * @return 项目列表
     */
    List<Project> selectRecentProjects(@Param("limit") int limit);

    /**
     * 统计本月新增项目数量
     * 
     * @return 本月新增项目数量
     */
    long countThisMonth();

    /**
     * 根据项目经理ID查询项目列表
     * 
     * @param managerId 项目经理ID
     * @return 项目列表
     */
    List<Project> selectByManagerId(@Param("managerId") Integer managerId);

    /**
     * 更新项目的项目经理
     * 
     * @param projectId   项目ID
     * @param managerId   项目经理ID
     * @param managerName 项目经理姓名
     * @return 影响行数
     */
    int updateManager(@Param("projectId") Integer projectId, @Param("managerId") Integer managerId,
            @Param("managerName") String managerName);
}