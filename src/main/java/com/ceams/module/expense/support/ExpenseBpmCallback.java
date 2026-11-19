package com.ceams.module.expense.support;

import com.ceams.common.security.SecurityUtils;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.support.BpmBizCallback;
import com.ceams.module.expense.entity.Receipt;
import com.ceams.module.expense.enums.ReceiptApprovalStatus;
import com.ceams.module.expense.enums.VerificationStatus;
import com.ceams.module.expense.mapper.ReceiptMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 报销业务流程回调：把 BPM 审批结果写回报销单。
 *
 * <p>报销流程固定为「部门审批 → 财务核验 → 结束」：
 *
 * <ul>
 *   <li>财务核验节点完成 → {@code verification_status = VERIFIED}（原独立核验接口已下线）；
 *   <li>实例终态 → {@code approval_status} 派生为 APPROVED / REJECTED / DRAFT（撤回）/ CANCELLED（终止）。
 * </ul>
 *
 * <p>业务表状态只是快照，唯一判断依据仍是 {@code bpm_instance.status}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpenseBpmCallback implements BpmBizCallback {

    /** 财务核验节点编码，与默认模板 {@code EXPENSE} 保持一致。 */
    public static final String NODE_FINANCE_VERIFY = "finance_verify";

    private final ReceiptMapper receiptMapper;

    @Override
    public BpmBizType bizType() {
        return BpmBizType.EXPENSE;
    }

    @Override
    public void onNodeCompleted(BpmInstance instance, String nodeKey) {
        if (!NODE_FINANCE_VERIFY.equals(nodeKey)) {
            return;
        }
        Receipt receipt = loadReceipt(instance);
        if (receipt == null || receipt.getVerificationStatus() == VerificationStatus.VERIFIED) {
            return;
        }
        receipt.setVerificationStatus(VerificationStatus.VERIFIED);
        receipt.setVerifiedBy(SecurityUtils.getCurrentUserId());
        receipt.setVerifiedAt(LocalDateTime.now());
        receiptMapper.updateById(receipt);
        log.info("报销单 {} 财务核验节点完成，核验状态置为 VERIFIED", receipt.getId());
    }

    @Override
    public void onInstanceFinished(BpmInstance instance, BpmInstanceStatus status) {
        Receipt receipt = loadReceipt(instance);
        if (receipt == null) {
            return;
        }
        ReceiptApprovalStatus target =
                switch (status) {
                    case APPROVED -> ReceiptApprovalStatus.APPROVED;
                    case REJECTED -> ReceiptApprovalStatus.REJECTED;
                    case WITHDRAWN -> ReceiptApprovalStatus.DRAFT;
                    case TERMINATED -> ReceiptApprovalStatus.CANCELLED;
                    default -> null;
                };
        if (target == null) {
            return;
        }
        ReceiptApprovalStatus current = receipt.getApprovalStatus();
        if (current == target) {
            // 重复终态回调：状态已一致，直接忽略，避免无意义写库
            log.debug("报销单 {} 审批状态已为 {}，忽略重复回调", receipt.getId(), target);
            return;
        }
        if (isTerminal(current)) {
            // 已定终态不允许被迟到回调覆盖（DRAFT 由撤回派生，不视为终态）
            log.warn(
                    "报销单 {} 已是终态 {}，忽略迟到的 {} 回调",
                    receipt.getId(),
                    current,
                    target);
            return;
        }
        receipt.setApprovalStatus(target);
        if (target == ReceiptApprovalStatus.REJECTED || target == ReceiptApprovalStatus.CANCELLED) {
            // 清空审批级别快照，避免前端展示过期的审批层级
            receipt.setCurrentApprovalLevel(0);
        }
        receiptMapper.updateById(receipt);
        log.info("报销单 {} 审批状态派生为 {}", receipt.getId(), target);
    }

    private static boolean isTerminal(ReceiptApprovalStatus status) {
        return status == ReceiptApprovalStatus.APPROVED
                || status == ReceiptApprovalStatus.REJECTED
                || status == ReceiptApprovalStatus.CANCELLED;
    }

    private Receipt loadReceipt(BpmInstance instance) {
        if (instance == null || instance.getBizId() == null) {
            return null;
        }
        return receiptMapper.selectById(instance.getBizId());
    }
}
