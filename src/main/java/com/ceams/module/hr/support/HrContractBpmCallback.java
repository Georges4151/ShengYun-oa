package com.ceams.module.hr.support;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ceams.common.enums.ContractApprovalStatus;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.support.BpmBizCallback;
import com.ceams.module.hr.entity.HrContract;
import com.ceams.module.hr.mapper.HrContractMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 劳动合同审批回调。
 *
 * <p>只写回审批状态快照 {@code approval_status}，不修改合同业务状态 {@code status}，
 * 并保留 {@code bpm_instance_id} 便于从合同查看流程。
 *
 * <p>幂等：重复回调不报错；已是终态（APPROVED / REJECTED）时直接跳过，避免旧实例覆盖新结果。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HrContractBpmCallback implements BpmBizCallback {

    private final HrContractMapper contractMapper;

    @Override
    public BpmBizType bizType() {
        return BpmBizType.HR_CONTRACT;
    }

    @Override
    public void onInstanceFinished(BpmInstance instance, BpmInstanceStatus status) {
        if (instance == null || instance.getBizId() == null) {
            return;
        }
        ContractApprovalStatus target = resolveTarget(status);
        if (target == null) {
            log.debug("劳动合同 {} 收到非终态 {}，忽略回调", instance.getBizId(), status);
            return;
        }
        HrContract contract = contractMapper.selectById(instance.getBizId());
        if (contract == null) {
            log.warn("劳动合同 {} 不存在，忽略回调", instance.getBizId());
            return;
        }
        if (contract.getApprovalStatus() == target) {
            log.info("劳动合同 {} 审批状态已是 {}，跳过重复回调", contract.getId(), target);
            return;
        }
        if (contract.getApprovalStatus() == ContractApprovalStatus.APPROVED
                || contract.getApprovalStatus() == ContractApprovalStatus.REJECTED) {
            log.info(
                    "劳动合同 {} 审批状态已是终态 {}，跳过回调 {}",
                    contract.getId(),
                    contract.getApprovalStatus(),
                    target);
            return;
        }
        HrContract patch = new HrContract();
        // 条件更新已保证并发安全，version 置空避免叠加乐观锁导致条件更新失效
        patch.setVersion(null);
        patch.setApprovalStatus(target);
        contractMapper.update(
                patch,
                new LambdaUpdateWrapper<HrContract>()
                        .eq(HrContract::getId, contract.getId())
                        .in(
                                HrContract::getApprovalStatus,
                                ContractApprovalStatus.NONE,
                                ContractApprovalStatus.PENDING));
        log.info("劳动合同 {} 审批回调完成，状态置 {}", contract.getId(), target);
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
