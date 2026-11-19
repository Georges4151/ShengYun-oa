package com.ceams.module.admin.support;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.admin.entity.AdminVehicle;
import com.ceams.module.admin.entity.AdminVehicleApply;
import com.ceams.module.admin.enums.VehicleApplyStatus;
import com.ceams.module.admin.enums.VehicleStatus;
import com.ceams.module.admin.mapper.AdminVehicleApplyMapper;
import com.ceams.module.admin.mapper.AdminVehicleMapper;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.support.BpmBizCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用车审批回调。
 *
 * <p>通过：车辆 IDLE → USING（条件更新，影响行数为 0 即抛 {@code BIZ_STATE_CONFLICT}），
 * 申请 APPROVING → APPROVED；驳回 / 撤回 / 终止：申请置 REJECTED，车辆不变。
 *
 * <p>幂等：申请已是 APPROVED / USING / RETURNED 直接返回。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminVehicleBpmCallback implements BpmBizCallback {

    private final AdminVehicleMapper vehicleMapper;
    private final AdminVehicleApplyMapper applyMapper;

    @Override
    public BpmBizType bizType() {
        return BpmBizType.ADMIN_VEHICLE_APPLY;
    }

    @Override
    public void onInstanceFinished(BpmInstance instance, BpmInstanceStatus status) {
        if (instance == null || instance.getBizId() == null) {
            return;
        }
        AdminVehicleApply apply = applyMapper.selectById(instance.getBizId());
        if (apply == null) {
            log.warn("用车申请 {} 不存在，忽略回调", instance.getBizId());
            return;
        }
        if (apply.getStatus() == VehicleApplyStatus.APPROVED
                || apply.getStatus() == VehicleApplyStatus.USING
                || apply.getStatus() == VehicleApplyStatus.RETURNED) {
            log.info("用车申请 {} 已是 {}，跳过重复回调", apply.getId(), apply.getStatus());
            return;
        }
        if (status == BpmInstanceStatus.APPROVED) {
            approve(apply);
            return;
        }
        if (status == BpmInstanceStatus.REJECTED
                || status == BpmInstanceStatus.WITHDRAWN
                || status == BpmInstanceStatus.TERMINATED) {
            reject(apply);
            return;
        }
        log.debug("用车申请 {} 收到非终态 {}，忽略", apply.getId(), status);
    }

    /** 审批通过：车辆置 USING，申请置 APPROVED。 */
    private void approve(AdminVehicleApply apply) {
        AdminVehicle vehiclePatch = new AdminVehicle();
        // 条件更新已保证并发安全，version 置空避免叠加乐观锁导致条件更新失效
        vehiclePatch.setVersion(null);
        vehiclePatch.setStatus(VehicleStatus.USING);
        int vehicleRows =
                vehicleMapper.update(
                        vehiclePatch,
                        new LambdaUpdateWrapper<AdminVehicle>()
                                .eq(AdminVehicle::getId, apply.getVehicleId())
                                .eq(AdminVehicle::getStatus, VehicleStatus.IDLE));
        if (vehicleRows == 0) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "车辆当前不可用，无法确认用车");
        }

        AdminVehicleApply applyPatch = new AdminVehicleApply();
        applyPatch.setVersion(null);
        applyPatch.setStatus(VehicleApplyStatus.APPROVED);
        int applyRows =
                applyMapper.update(
                        applyPatch,
                        new LambdaUpdateWrapper<AdminVehicleApply>()
                                .eq(AdminVehicleApply::getId, apply.getId())
                                .eq(AdminVehicleApply::getStatus, VehicleApplyStatus.APPROVING));
        if (applyRows == 0) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "用车申请状态已变更，请刷新后重试");
        }
        log.info("用车申请 {} 审批通过，车辆 {} 置 USING", apply.getId(), apply.getVehicleId());
    }

    /** 驳回 / 撤回 / 终止：申请置 REJECTED，车辆状态不变。 */
    private void reject(AdminVehicleApply apply) {
        AdminVehicleApply patch = new AdminVehicleApply();
        patch.setVersion(null);
        patch.setStatus(VehicleApplyStatus.REJECTED);
        applyMapper.update(
                patch,
                new LambdaUpdateWrapper<AdminVehicleApply>()
                        .eq(AdminVehicleApply::getId, apply.getId())
                        .eq(AdminVehicleApply::getStatus, VehicleApplyStatus.APPROVING));
        log.info("用车申请 {} 审批驳回，置 REJECTED", apply.getId());
    }
}
