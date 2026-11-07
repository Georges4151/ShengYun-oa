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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 票据（报销）服务。 */
public interface ReceiptService {

    PageResult<ReceiptVO> page(
            long page,
            long size,
            String receiptNumber,
            Long userId,
            Long projectId,
            VerificationStatus verificationStatus,
            PaymentStatus paymentStatus,
            ReceiptApprovalStatus approvalStatus);

    ReceiptVO detail(Long id);

    /** 重复报销候选（只读）：排除自身，命中「同票号」或「同金额且同日」。 */
    List<ReceiptVO> findPotentialDuplicates(
            Long excludeId,
            String receiptNumber,
            BigDecimal amount,
            LocalDateTime dayStart,
            LocalDateTime dayEnd);

    String create(ReceiptCreateRequest request);

    void update(Long id, ReceiptUpdateRequest request);

    void delete(Long id);

    void submit(Long id);

    void approve(Long id, ApprovalActionRequest request);

    void reject(Long id, ApprovalActionRequest request);

    void verify(Long id, ReceiptVerifyRequest request);

    List<ApprovalRecordVO> listApprovals(Long id);

    List<ReceiptVerificationVO> listVerifications(Long id);

    /** 期间报销汇总（只读）：合计金额、状态分布与类型分布。 */
    ExpenseSummary summary(LocalDateTime start, LocalDateTime end, Long userId, String keyword);

    /** 待审批票据清单（只读）：数量、金额合计与明细。 */
    PendingApprovalPage pendingApprovals(Long userId, String keyword, Long projectId, int limit);

    /** 期间报销汇总结果。 */
    record ExpenseSummary(
            BigDecimal totalAmount,
            long totalCount,
            Map<String, Long> approvalStatusDistribution,
            Map<String, Long> paymentStatusDistribution,
            Map<String, Long> verificationStatusDistribution,
            Map<String, Long> typeDistribution) {}

    /** 待审批票据结果。 */
    record PendingApprovalPage(long pendingCount, BigDecimal pendingAmount, List<ReceiptVO> items) {

        public PendingApprovalPage {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }
}
