package com.ceams.module.seal.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.seal.dto.StampApprovalApproveRequest;
import com.ceams.module.seal.dto.StampApprovalCreateRequest;
import com.ceams.module.seal.dto.StampApprovalRejectRequest;
import com.ceams.module.seal.dto.StampApprovalUpdateRequest;
import com.ceams.module.seal.enums.ApprovalStatus;
import com.ceams.module.seal.vo.StampApprovalVO;

/** 用印审批服务。 */
public interface StampApprovalService {

    PageResult<StampApprovalVO> page(
            long page, long size, Long stampId, Long sealId, ApprovalStatus status);

    StampApprovalVO detail(Long id);

    String create(StampApprovalCreateRequest request);

    void update(Long id, StampApprovalUpdateRequest request);

    void approve(Long id, StampApprovalApproveRequest request);

    void reject(Long id, StampApprovalRejectRequest request);

    void delete(Long id);
}
