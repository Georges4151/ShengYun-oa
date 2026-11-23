package com.ceams.module.hr.service;

import com.ceams.module.hr.dto.JobLevelCreateRequest;
import com.ceams.module.hr.dto.JobLevelUpdateRequest;
import com.ceams.module.hr.vo.JobLevelVO;
import java.util.List;

/** 职级服务。 */
public interface HrJobLevelService {

    /** 全量职级（按排序号、编码升序）。 */
    List<JobLevelVO> list();

    /** 启用中的职级（仅 status=ENABLED，按排序号、编码升序），供下拉选择使用。 */
    List<JobLevelVO> enabledList();

    /** 新建职级，返回新增 ID。 */
    String create(JobLevelCreateRequest request);

    /** 修改职级（字段为 null 表示不修改）。 */
    void update(Long id, JobLevelUpdateRequest request);
}
