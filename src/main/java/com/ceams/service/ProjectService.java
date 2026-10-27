package com.ceams.service;

import com.ceams.dao.ProjectMapper;
import com.ceams.model.PageResult;
import com.ceams.model.Pagination;
import com.ceams.model.dto.ProjectCostAnalysisDTO;
import com.ceams.model.entity.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.ceams.model.event.AccountDataChangedEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ProjectService {

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public List<Project> getByStatus(String status) {
        return projectMapper.selectByStatus(status);
    }

    public List<Project> getByClient(String clientName) {
        return projectMapper.selectByClient(clientName);
    }

    public Map<String, Object> getProjectStatistics() {
        return projectMapper.getProjectStatistics();
    }

    public List<Map<String, Object>> getProjectCostAnalysis() {
        List<Map<String, Object>> result = projectMapper.getProjectCostAnalysis();
        System.out.println("ProjectService.getProjectCostAnalysis() called, result size: "
                + (result != null ? result.size() : "null"));
        return result;
    }

    /**
     * 获取项目成本分析数据，返回DTO对象列表
     * 
     * @return 项目成本分析DTO对象列表
     */
    public List<ProjectCostAnalysisDTO> getProjectCostAnalysisDTO() {
        List<Map<String, Object>> result = projectMapper.getProjectCostAnalysis();
        System.out.println("ProjectService.getProjectCostAnalysisDTO() called, result size: "
                + (result != null ? result.size() : "null"));

        // 转换为DTO对象列表
        List<ProjectCostAnalysisDTO> dtoList = new ArrayList<>();
        for (Map<String, Object> map : result) {
            Integer projectId = (Integer) map.get("project_id");
            String projectName = (String) map.get("project_name");
            BigDecimal contractAmount = (BigDecimal) map.get("contract_amount");
            BigDecimal budgetAmount = (BigDecimal) map.get("budget_amount");
            BigDecimal actualAmount = (BigDecimal) map.get("actual_amount");
            BigDecimal budgetSurplus = (BigDecimal) map.get("budget_surplus");
            Double costOverrunRate = null;
            Object costOverrunRateObj = map.get("cost_overrun_rate");
            if (costOverrunRateObj != null) {
                if (costOverrunRateObj instanceof BigDecimal) {
                    costOverrunRate = ((BigDecimal) costOverrunRateObj).doubleValue();
                } else if (costOverrunRateObj instanceof Double) {
                    costOverrunRate = (Double) costOverrunRateObj;
                } else if (costOverrunRateObj instanceof Number) {
                    costOverrunRate = ((Number) costOverrunRateObj).doubleValue();
                }
            }

            ProjectCostAnalysisDTO dto = new ProjectCostAnalysisDTO(
                    projectId, projectName, contractAmount, budgetAmount, actualAmount, budgetSurplus, costOverrunRate);
            dtoList.add(dto);
        }

        return dtoList;
    }

    public boolean addProject(Project project) {
        boolean result = projectMapper.insert(project) > 0;
        if (result) {
            eventPublisher.publishEvent(new AccountDataChangedEvent(this, "添加项目: " + project.getProjectName()));
        }
        return result;
    }

    public boolean updateProject(Project project) {
        boolean result = projectMapper.update(project) > 0;
        if (result) {
            eventPublisher.publishEvent(new AccountDataChangedEvent(this, "更新项目: " + project.getProjectName()));
        }
        return result;
    }

    public boolean deleteProject(Integer projectId) {
        Project project = getProjectDetail(projectId);
        boolean result = projectMapper.deleteById(projectId) > 0;
        if (result && project != null) {
            eventPublisher.publishEvent(new AccountDataChangedEvent(this, "删除项目: " + project.getProjectName()));
        }
        return result;
    }

    public Project getProjectDetail(Integer projectId) {
        return projectMapper.selectById(projectId);
    }

    public List<Project> searchProjects(String keyword) {
        // 根据关键字搜索项目
        return projectMapper.selectByKeyword(keyword);
    }

    public List<Project> getAllProjects() {
        return projectMapper.selectAll();
    }

    /**
     * 分页获取所有项目
     * 
     * @param pagination 分页参数
     * @return 分页结果
     */
    public PageResult<Project> getProjectsByPage(Pagination pagination) {
        List<Project> list = projectMapper.selectByPage(pagination);
        long total = projectMapper.count();
        return new PageResult<>(list, total, pagination.getPageNum(), pagination.getPageSize());
    }

    /**
     * 分页根据关键字搜索项目
     * 
     * @param pagination 分页参数
     * @return 分页结果
     */
    public PageResult<Project> searchProjectsByPage(Pagination pagination) {
        List<Project> list = projectMapper.selectByKeywordAndPage(pagination);
        long total = projectMapper.countByKeyword(pagination.getKeyword());
        return new PageResult<>(list, total, pagination.getPageNum(), pagination.getPageSize());
    }

    /**
     * 获取所有项目记录总数
     * 
     * @return 项目记录总数
     */
    public long count() {
        return projectMapper.count();
    }

    /**
     * 根据项目经理ID查询项目列表
     * 
     * @param managerId 项目经理ID
     * @return 项目列表
     */
    public List<Project> getProjectsByManagerId(Integer managerId) {
        return projectMapper.selectByManagerId(managerId);
    }

    /**
     * 绑定项目经理到项目
     * 
     * @param projectId   项目ID
     * @param managerId   项目经理ID
     * @param managerName 项目经理姓名
     * @return 是否成功
     */
    public boolean bindManager(Integer projectId, Integer managerId, String managerName) {
        int result = projectMapper.updateManager(projectId, managerId, managerName);
        if (result > 0) {
            Project project = getProjectDetail(projectId);
            if (project != null) {
                eventPublisher.publishEvent(new AccountDataChangedEvent(this,
                        "项目【" + project.getProjectName() + "】绑定项目经理: " + managerName));
            }
            return true;
        }
        return false;
    }

    /**
     * 解绑项目经理
     * 
     * @param projectId 项目ID
     * @return 是否成功
     */
    public boolean unbindManager(Integer projectId) {
        int result = projectMapper.updateManager(projectId, null, null);
        if (result > 0) {
            Project project = getProjectDetail(projectId);
            if (project != null) {
                eventPublisher.publishEvent(new AccountDataChangedEvent(this,
                        "项目【" + project.getProjectName() + "】解绑项目经理"));
            }
            return true;
        }
        return false;
    }

    /**
     * 检查用户是否为指定项目的项目经理
     * 
     * @param userId    用户ID
     * @param projectId 项目ID
     * @return 是否为项目经理
     */
    public boolean isProjectManager(Integer userId, Integer projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            return false;
        }
        return userId != null && userId.equals(project.getManagerId());
    }
}