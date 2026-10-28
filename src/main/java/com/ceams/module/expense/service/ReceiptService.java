package com.ceams.module.expense.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.expense.dto.ApprovalActionRequest;
import com.ceams.module.expense.dto.ReceiptCreateRequest;
import com.ceams.module.expense.dto.ReceiptUpdateRequest;
import com.ceams.module.expense.dto.ReceiptVerifyRequest;
import com.ceams.module.expense.enums.PaymentStatus;
import com.ceams.module.expense.enums.ReceiptApprovalStatus;
import com.ceams.module.expense.enums.VerificationStatus;
import com.ceams.module.expense.vo.ApprovalRecordVO;
import com.ceams.module.expense.vo.ReceiptVO;
import com.ceams.module.expense.vo.ReceiptVerificationVO;

import java.util.List;

/**
 * 票据（报销）服务。
 */
public interface ReceiptService {

    PageResult<ReceiptVO> page(long page, long size, String receiptNumber, Long userId, Long projectId,
                               VerificationStatus verificationStatus, PaymentStatus paymentStatus,
                               ReceiptApprovalStatus approvalStatus);

    ReceiptVO detail(Long id);

    String create(ReceiptCreateRequest request);

    void update(Long id, ReceiptUpdateRequest request);

    void delete(Long id);

    void submit(Long id);

    void approve(Long id, ApprovalActionRequest request);

    void reject(Long id, ApprovalActionRequest request);

    void verify(Long id, ReceiptVerifyRequest request);

    List<ApprovalRecordVO> listApprovals(Long id);

    List<ReceiptVerificationVO> listVerifications(Long id);
}
