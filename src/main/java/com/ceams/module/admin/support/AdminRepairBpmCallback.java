package com.ceams.module.admin.support;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.admin.entity.AdminRepair;
import com.ceams.module.admin.enums.RepairStatus;
import com.ceams.module.admin.mapper.AdminRepairMapper;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.support.BpmBizCallback;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 报修审批回调。
 *
 * <p>通过：工单 SUBMITTED → ASSIGNED（回写 assigned_at，维修人留空待派单）；
 * 驳回 / 撤回 / 终止：工单置 REJECTED。
 *
 * <p>幂等：工单已是 ASSIGNED / PROCESSING / DONE / ACCEPTED / REJECTED 直接返回。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminRepairBpmCallback implements BpmBizCallback {

    private final AdminRepairMapper repairMapper;

    @Override
    public BpmBizType bizType() {
        return BpmBizType.ADMIN_REPAIR;
    }

    @Override
    public void onInstanceFinished(BpmInstance instance, BpmInstanceStatus status) {
        if (instance == null || instance.getBizId() == null) {
            return;
        }
        AdminRepair repair = repairMapper.selectById(instance.getBizId());
        if (repair == null) {
            log.warn("报修工单 {} 不存在，忽略回调", instance.getBizId());
            return;
        }
        if (repair.getStatus() == RepairStatus.ASSIGNED
                || repair.getStatus() == RepairStatus.PROCESSING
                || repair.getStatus() == RepairStatus.DONE
                || repair.getStatus() == RepairStatus.ACCEPTED
                || repair.getStatus() == RepairStatus.REJECTED) {
            log.info("报修工单 {} 已是 {}，跳过重复回调", repair.getId(), repair.getStatus());
            return;
        }
        if (status == BpmInstanceStatus.APPROVED) {
            approve(repair);
            return;
        }
        if (status == BpmInstanceStatus.REJECTED
                || status == BpmInstanceStatus.WITHDRAWN
                || status == BpmInstanceStatus.TERMINATED) {
            reject(repair);
            return;
        }
        log.debug("报修工单 {} 收到非终态 {}，忽略", repair.getId(), status);
    }

    /** 审批通过：SUBMITTED → ASSIGNED，维修人留空待行政派单。 */
    private void approve(AdminRepair repair) {
        AdminRepair patch = new AdminRepair();
        // 条件更新已保证并发安全，version 置空避免叠加乐观锁导致条件更新失效
        patch.setVersion(null);
        patch.setStatus(RepairStatus.ASSIGNED);
        patch.setAssignedAt(LocalDateTime.now());
        int rows =
                repairMapper.update(
                        patch,
                        new LambdaUpdateWrapper<AdminRepair>()
                                .eq(AdminRepair::getId, repair.getId())
                                .eq(AdminRepair::getStatus, RepairStatus.SUBMITTED));
        if (rows == 0) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "报修工单状态已变更，请刷新后重试");
        }
        log.info("报修工单 {} 审批通过，置 ASSIGNED", repair.getId());
    }

    /** 驳回 / 撤回 / 终止：工单置 REJECTED。 */
    private void reject(AdminRepair repair) {
        AdminRepair patch = new AdminRepair();
        patch.setVersion(null);
        patch.setStatus(RepairStatus.REJECTED);
        repairMapper.update(
                patch,
                new LambdaUpdateWrapper<AdminRepair>()
                        .eq(AdminRepair::getId, repair.getId())
                        .eq(AdminRepair::getStatus, RepairStatus.SUBMITTED));
        log.info("报修工单 {} 审批驳回，置 REJECTED", repair.getId());
    }
}
