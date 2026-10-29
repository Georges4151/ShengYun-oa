package com.ceams.module.project.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.project.dto.progresspayment.ProgressPaymentCreateRequest;
import com.ceams.module.project.dto.progresspayment.ProgressPaymentUpdateRequest;
import com.ceams.module.project.enums.ApprovalStatus;
import com.ceams.module.project.vo.ProgressPaymentVO;

/** 进度款服务。 */
public interface ProgressPaymentService {

    PageResult<ProgressPaymentVO> page(long page, long size, Long projectId, ApprovalStatus status);

    ProgressPaymentVO detail(Long id);

    String create(ProgressPaymentCreateRequest request);

    void update(Long id, ProgressPaymentUpdateRequest request);

    void delete(Long id);
}
