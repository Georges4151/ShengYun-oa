package com.ceams.module.project.support;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ceams.common.enums.ContractApprovalStatus;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.support.BpmBizCallback;
import com.ceams.module.project.entity.SubcontractorContract;
import com.ceams.module.project.mapper.SubcontractorContractMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 分包合同审批回调。
 *
 * <p>只写回审批状态快照 {@code approval_status}，不修改合同业务状态 {@code status}，
 * 并保留 {@code bpm_instance_id} 便于从合同查看流程。
 *
 * <p>幂等：重复回调不报错；已是终态（APPROVED / REJECTED）时直接跳过，避免旧实例覆盖新结果。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubcontractorContractBpmCallback implements BpmBizCallback {

    private final SubcontractorContractMapper contractMapper;

    @Override
    public BpmBizType bizType() {
        return BpmBizType.SUBCONTRACT_CONTRACT;
    }

    @Override
    public void onInstanceFinished(BpmInstance instance, BpmInstanceStatus status) {
        if (instance == null || instance.getBizId() == null) {
            return;
        }
        ContractApprovalStatus target = resolveTarget(status);
        if (target == null) {
            log.debug("分包合同 {} 收到非终态 {}，忽略回调", instance.getBizId(), status);
            return;
        }
        SubcontractorContract contract = contractMapper.selectById(instance.getBizId());
        if (contract == null) {
            log.warn("分包合同 {} 不存在，忽略回调", instance.getBizId());
            return;
        }
        if (contract.getApprovalStatus() == target) {
            log.info("分包合同 {} 审批状态已是 {}，跳过重复回调", contract.getId(), target);
            return;
        }
        if (contract.getApprovalStatus() == ContractApprovalStatus.APPROVED
                || contract.getApprovalStatus() == ContractApprovalStatus.REJECTED) {
            log.info(
                    "分包合同 {} 审批状态已是终态 {}，跳过回调 {}",
                    contract.getId(),
                    contract.getApprovalStatus(),
                    target);
            return;
        }
        SubcontractorContract patch = new SubcontractorContract();
        // paidAmount 字段有默认值 ZERO，条件更新时必须显式置空，避免把已付金额覆盖为 0
        patch.setPaidAmount(null);
        // 条件更新已保证并发安全，version 置空避免叠加乐观锁导致条件更新失效
        patch.setVersion(null);
        patch.setApprovalStatus(target);
        contractMapper.update(
                patch,
                new LambdaUpdateWrapper<SubcontractorContract>()
                        .eq(SubcontractorContract::getId, contract.getId())
                        .in(
                                SubcontractorContract::getApprovalStatus,
                                ContractApprovalStatus.NONE,
                                ContractApprovalStatus.PENDING));
        log.info("分包合同 {} 审批回调完成，状态置 {}", contract.getId(), target);
    }

    /** APPROVED → APPROVED；驳回/撤回/终止 → REJECTED；其余返回 null。 */
    private ContractApprovalStatus resolveTarget(BpmInstanceStatus status) {
        if (status == BpmInstanceStatus.APPROVED) {
            return ContractApprovalStatus.APPROVED;
        }
        if (status == BpmInstanceStatus.REJECTED
                || status == BpmInstanceStatus.WITHDRAWN
                || status == BpmInstanceStatus.TERMINATED) {
            return ContractApprovalStatus.REJECTED;
        }
        return null;
    }
}
