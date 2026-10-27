package com.ceams.service;

import com.ceams.dao.SubcontractorMapper;
import com.ceams.dao.SubcontractorContractMapper;
import com.ceams.model.entity.Subcontractor;
import com.ceams.model.entity.SubcontractorContract;
import com.ceams.model.entity.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SubcontractorService {

    @Autowired
    private SubcontractorMapper subcontractorMapper;

    @Autowired
    private SubcontractorContractMapper subcontractorContractMapper;

    @Autowired
    private ProjectService projectService;

    public List<Subcontractor> getByQualification(String qualificationLevel) {
        return subcontractorMapper.selectByQualification(qualificationLevel);
    }

    public List<Subcontractor> getByCreditRating(String creditRating) {
        return subcontractorMapper.selectByCreditRating(creditRating);
    }

    public boolean addSubcontractor(Subcontractor subcontractor) {
        return subcontractorMapper.insert(subcontractor) > 0;
    }

    public boolean updateSubcontractor(Subcontractor subcontractor) {
        return subcontractorMapper.update(subcontractor) > 0;
    }

    public boolean deleteSubcontractor(Integer subcontractorId) {
        return subcontractorMapper.deleteById(subcontractorId) > 0;
    }

    public Subcontractor getSubcontractorDetail(Integer subcontractorId) {
        Subcontractor subcontractor = subcontractorMapper.selectById(subcontractorId);
        if (subcontractor != null) {
            // 加载关联的项目
            loadProjects(subcontractor);
            // 加载关联的合同
            loadContracts(subcontractor);
        }
        return subcontractor;
    }

    public List<Subcontractor> searchSubcontractors(Map<String, Object> params) {
        String name = (String) params.get("name");
        String contactPerson = (String) params.get("contactPerson");
        String phone = (String) params.get("phone");
        Integer projectId = (Integer) params.get("projectId");

        // 如果所有条件都为空，返回所有分包商
        if ((name == null || name.isEmpty()) &&
                (contactPerson == null || contactPerson.isEmpty()) &&
                (phone == null || phone.isEmpty()) &&
                projectId == null) {
            return getAllSubcontractorsWithDetails();
        }

        List<Subcontractor> subcontractors;
        if (projectId != null) {
            // 根据项目ID查询分包商
            subcontractors = subcontractorMapper.selectByProjectId(projectId);
        } else {
            // 根据其他条件查询分包商
            subcontractors = subcontractorMapper.selectByConditions(name, contactPerson, phone);
        }

        // 加载每个分包商的详细信息
        for (Subcontractor subcontractor : subcontractors) {
            loadProjects(subcontractor);
            loadContractCount(subcontractor);
        }
        return subcontractors;
    }

    public List<Subcontractor> getAllSubcontractors() {
        return getAllSubcontractorsWithDetails();
    }

    /**
     * 获取所有分包商，包含项目和合同信息
     */
    public List<Subcontractor> getAllSubcontractorsWithDetails() {
        List<Subcontractor> subcontractors = subcontractorMapper.selectAll();
        // 加载每个分包商的详细信息
        for (Subcontractor subcontractor : subcontractors) {
            loadProjects(subcontractor);
            loadContractCount(subcontractor);
        }
        return subcontractors;
    }

    /**
     * 加载分包商的关联项目
     */
    private void loadProjects(Subcontractor subcontractor) {
        List<Integer> projectIds = subcontractorMapper.selectProjectIdsBySubcontractorId(subcontractor.getId());
        if (projectIds != null && !projectIds.isEmpty()) {
            List<Project> projects = new ArrayList<>();
            List<String> projectNames = new ArrayList<>();
            for (Integer projectId : projectIds) {
                Project project = projectService.getProjectDetail(projectId);
                if (project != null) {
                    projects.add(project);
                    projectNames.add(project.getProjectName());
                }
            }
            subcontractor.setProjects(projects);
            subcontractor.setProjectNames(projectNames);
        }
    }

    /**
     * 加载分包商的关联合同
     */
    private void loadContracts(Subcontractor subcontractor) {
        List<SubcontractorContract> contracts = subcontractorContractMapper
                .selectBySubcontractorId(subcontractor.getId());
        subcontractor.setContracts(contracts);
        subcontractor.setContractCount(contracts != null ? contracts.size() : 0);
    }

    /**
     * 加载分包商的合同数量
     */
    private void loadContractCount(Subcontractor subcontractor) {
        Integer contractCount = subcontractorMapper.selectContractCountBySubcontractorId(subcontractor.getId());
        subcontractor.setContractCount(contractCount != null ? contractCount : 0);
    }

    /**
     * 为分包商添加项目关联
     */
    public boolean addProjectRelation(Integer subcontractorId, Integer projectId) {
        return subcontractorMapper.addProjectRelation(subcontractorId, projectId) > 0;
    }

    /**
     * 为分包商移除项目关联
     */
    public boolean removeProjectRelation(Integer subcontractorId, Integer projectId) {
        return subcontractorMapper.removeProjectRelation(subcontractorId, projectId) > 0;
    }

    /**
     * 添加分包商合同
     */
    public boolean addContract(SubcontractorContract contract) {
        return subcontractorContractMapper.insert(contract) > 0;
    }

    /**
     * 更新分包商合同
     */
    public boolean updateContract(SubcontractorContract contract) {
        return subcontractorContractMapper.update(contract) > 0;
    }

    /**
     * 删除分包商合同
     */
    public boolean deleteContract(Integer contractId) {
        return subcontractorContractMapper.deleteById(contractId) > 0;
    }

    /**
     * 获取分包商的合同列表
     */
    public List<SubcontractorContract> getSubcontractorContracts(Integer subcontractorId) {
        List<SubcontractorContract> contracts = subcontractorContractMapper.selectBySubcontractorId(subcontractorId);
        // 加载每个合同的项目信息
        for (SubcontractorContract contract : contracts) {
            if (contract.getProjectId() != null) {
                Project project = projectService.getProjectDetail(contract.getProjectId());
                contract.setProject(project);
            }
        }
        return contracts;
    }

    /**
     * 获取合同详情
     */
    public SubcontractorContract getContractDetail(Integer contractId) {
        return subcontractorContractMapper.selectById(contractId);
    }

    /**
     * 获取所有项目
     */
    public List<Project> getAllProjects() {
        return projectService.getAllProjects();
    }

    /**
     * 获取分包商的项目ID列表
     */
    public List<Integer> getSubcontractorProjectIds(Integer subcontractorId) {
        return subcontractorMapper.selectProjectIdsBySubcontractorId(subcontractorId);
    }

    /**
     * 设置分包商的项目关联
     */
    public boolean setSubcontractorProjects(Integer subcontractorId, List<Integer> projectIds) {
        try {
            // 先删除所有现有关联
            subcontractorMapper.deleteProjectRelations(subcontractorId);

            // 添加新的关联
            if (projectIds != null && !projectIds.isEmpty()) {
                for (Integer projectId : projectIds) {
                    subcontractorMapper.addProjectRelation(subcontractorId, projectId);
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取所有合同
     */
    public List<SubcontractorContract> getAllContracts() {
        return subcontractorContractMapper.selectAll();
    }
}
