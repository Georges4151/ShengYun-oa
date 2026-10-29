package com.ceams.module.project.converter;

import com.ceams.module.project.entity.Project;
import com.ceams.module.project.vo.ProjectVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 项目台账实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface ProjectConverter {

    default ProjectVO toVO(Project project) {
        if (project == null) {
            return null;
        }
        ProjectVO vo = new ProjectVO();
        vo.setId(project.getId());
        vo.setProjectName(project.getProjectName());
        vo.setClientName(project.getClientName());
        vo.setProjectAddress(project.getProjectAddress());
        vo.setProjectType(project.getProjectType());
        vo.setStartDate(
                project.getStartDate() == null ? null : project.getStartDate().toLocalDate());
        vo.setEndDate(project.getEndDate() == null ? null : project.getEndDate().toLocalDate());
        vo.setContractAmount(project.getContractAmount());
        vo.setBudgetAmount(project.getBudgetAmount());
        vo.setActualAmount(project.getActualAmount());
        vo.setStatus(project.getStatus());
        vo.setDescription(project.getDescription());
        vo.setManagerId(project.getManagerId());
        vo.setManagerName(project.getManagerName());
        vo.setCreateTime(project.getCreateTime());
        return vo;
    }

    List<ProjectVO> toVOList(List<Project> projects);
}
