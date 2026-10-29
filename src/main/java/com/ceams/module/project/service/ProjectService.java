package com.ceams.module.project.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.project.dto.project.ProjectCreateRequest;
import com.ceams.module.project.dto.project.ProjectUpdateRequest;
import com.ceams.module.project.enums.ProjectStatus;
import com.ceams.module.project.vo.ProjectVO;

/** 项目台账服务。 */
public interface ProjectService {

    PageResult<ProjectVO> page(long page, long size, String keyword, ProjectStatus status);

    ProjectVO detail(Long id);

    String create(ProjectCreateRequest request);

    void update(Long id, ProjectUpdateRequest request);

    void delete(Long id);
}
