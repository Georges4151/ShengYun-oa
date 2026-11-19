package com.ceams.module.project.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.project.dto.changerequest.ChangeRequestCreateRequest;
import com.ceams.module.project.dto.changerequest.ChangeRequestUpdateRequest;
import com.ceams.module.project.enums.ApprovalStatus;
import com.ceams.module.project.vo.ChangeRequestVO;

/** 变更请求服务。 */
public interface ChangeRequestService {

    PageResult<ChangeRequestVO> page(long page, long size, Long projectId, ApprovalStatus status);

    ChangeRequestVO detail(Long id);

    String create(ChangeRequestCreateRequest request);

    void update(Long id, ChangeRequestUpdateRequest request);

    void delete(Long id);

    /** 提交：发起 BPM 项目变更流程（部门审批 → 结束）。 */
    void submit(Long id);
}
