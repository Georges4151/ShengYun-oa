package com.ceams.module.admin.support;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.admin.entity.AdminMaterial;
import com.ceams.module.admin.entity.AdminMaterialApply;
import com.ceams.module.admin.enums.MaterialApplyStatus;
import com.ceams.module.admin.mapper.AdminMaterialApplyMapper;
import com.ceams.module.admin.mapper.AdminMaterialMapper;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.AuditAction;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.bpm.support.BpmBizCallback;
import java.math.BigDecimal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 易耗品申领审批回调。
 *
 * <p>通过：按条件扣减库存（{@code stock_qty >= quantity}），扣减成功后将申领单置 APPROVED；
 * 扣减失败抛 {@code BIZ_STATE_CONFLICT}，审批事务回滚，任务保持待办。
 * 驳回 / 撤回 / 终止：申领单置 REJECTED，库存不变。
 *
 * <p>幂等：申领单已 APPROVED / ISSUED 时直接返回。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminMaterialBpmCallback implements BpmBizCallback {

    /** 审计目标类型。 */
    private static final String AUDIT_TARGET = "ADMIN_MATERIAL";

    private final AdminMaterialMapper materialMapper;
    private final AdminMaterialApplyMapper applyMapper;
    private final BpmAuditService bpmAuditService;

    @Override
    public BpmBizType bizType() {
        return BpmBizType.ADMIN_MATERIAL_APPLY;
    }

    @Override
    public void onInstanceFinished(BpmInstance instance, BpmInstanceStatus status) {
        if (instance == null || instance.getBizId() == null) {
            return;
        }
        AdminMaterialApply apply = applyMapper.selectById(instance.getBizId());
        if (apply == null) {
            log.warn("易耗品申领单 {} 不存在，忽略回调", instance.getBizId());
            return;
        }
        if (apply.getStatus() == MaterialApplyStatus.APPROVED
                || apply.getStatus() == MaterialApplyStatus.ISSUED) {
            log.info("申领单 {} 已审批通过，跳过重复回调", apply.getId());
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
        log.debug("易耗品申领单 {} 收到非终态 {}，忽略", apply.getId(), status);
    }

    /** 审批通过：条件扣减库存并置申领单 APPROVED。 */
    private void approve(AdminMaterialApply apply) {
        AdminMaterial material = materialMapper.selectById(apply.getMaterialId());
        if (material == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "申领物料不存在，无法扣减库存");
        }
        BigDecimal before = material.getStockQty() == null ? BigDecimal.ZERO : material.getStockQty();

        UpdateWrapper<AdminMaterial> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", apply.getMaterialId())
                .ge("stock_qty", apply.getQuantity())
                .setSql("stock_qty = stock_qty - " + apply.getQuantity().toPlainString());
        int stockRows = materialMapper.update(null, wrapper);
        if (stockRows == 0) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "库存不足，无法完成申领扣减");
        }

        AdminMaterialApply patch = new AdminMaterialApply();
        patch.setVersion(null);
        patch.setStatus(MaterialApplyStatus.APPROVED);
        int applyRows =
                applyMapper.update(
                        patch,
                        new LambdaUpdateWrapper<AdminMaterialApply>()
                                .eq(AdminMaterialApply::getId, apply.getId())
                                .eq(AdminMaterialApply::getStatus, MaterialApplyStatus.APPROVING));
        if (applyRows == 0) {
            // 库存已扣减但状态未推进，抛冲突让审批事务回滚，避免库存与单据不一致
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "申领单状态已变更，请刷新后重试");
        }

        recordAudit(apply, before, before.subtract(apply.getQuantity()));
        log.info(
                "申领单 {} 审批通过，物料 {} 库存 {} → {}",
                apply.getId(),
                apply.getMaterialId(),
                before,
                before.subtract(apply.getQuantity()));
    }

    /** 驳回 / 撤回 / 终止：申领单置 REJECTED，库存不变。 */
    private void reject(AdminMaterialApply apply) {
        AdminMaterialApply patch = new AdminMaterialApply();
        patch.setVersion(null);
        patch.setStatus(MaterialApplyStatus.REJECTED);
        applyMapper.update(
                patch,
                new LambdaUpdateWrapper<AdminMaterialApply>()
                        .eq(AdminMaterialApply::getId, apply.getId())
                        .eq(AdminMaterialApply::getStatus, MaterialApplyStatus.APPROVING));
        log.info("易耗品申领单 {} 审批驳回，置 REJECTED", apply.getId());
    }

    /** 扣减库存写审计快照（复用 STOCK_IN 动作，记录扣减前后库存）。 */
    private void recordAudit(AdminMaterialApply apply, BigDecimal before, BigDecimal after) {
        bpmAuditService.record(
                AUDIT_TARGET,
                apply.getMaterialId(),
                AuditAction.STOCK_IN,
                Map.of("stockQty", before.toPlainString()),
                Map.of("stockQty", after.toPlainString()),
                null,
                SecurityUtils.getCurrentUserIdOrDefault());
    }
}
