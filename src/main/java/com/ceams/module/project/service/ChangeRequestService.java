package com.ceams.module.project.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.project.dto.changerequest.ApprovalActionRequest;
import com.ceams.module.project.dto.changerequest.ChangeRequestCreateRequest;
import com.ceams.module.project.dto.changerequest.ChangeRequestUpdateRequest;
import com.ceams.module.project.enums.ApprovalStatus;
import com.ceams.module.project.vo.ChangeRequestVO;

/**
 * 变更请求服务。
 */
public interface ChangeRequestService {

    PageResult<ChangeRequestVO> page(long page, long size, Long projectId, ApprovalStatus status);

    ChangeRequestVO detail(Long id);

    String create(ChangeRequestCreateRequest request);

    void update(Long id, ChangeRequestUpdateRequest request);

    void delete(Long id);

    void approve(Long id, ApprovalActionRequest request);

    void reject(Long id, ApprovalActionRequest request);
}