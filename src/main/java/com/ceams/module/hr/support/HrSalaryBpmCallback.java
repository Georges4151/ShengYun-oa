package com.ceams.module.hr.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.AuditAction;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.bpm.support.BpmBizCallback;
import com.ceams.module.hr.entity.HrSalary;
import com.ceams.module.hr.entity.HrSalaryChangeApply;
import com.ceams.module.hr.enums.SalaryChangeStatus;
import com.ceams.module.hr.enums.SalaryStatus;
import com.ceams.module.hr.mapper.HrSalaryChangeApplyMapper;
import com.ceams.module.hr.mapper.HrSalaryMapper;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 薪资变动审批回调。
 *
 * <p>通过：旧 ACTIVE 置 HISTORY → 用 after_snapshot 解密后的金额插入新 ACTIVE（回填 change_apply_id）
 * → 申请置 APPROVED → 写审计快照。驳回 / 撤回 / 终止：申请统一置 REJECTED，台账不变。
 *
 * <p>幂等：申请已是 APPROVED 直接返回；已存在同 change_apply_id 的台账时跳过插入；
 * 数据库唯一键 {@code uk_hr_salary_active} 兜底保证同一员工只有一条 ACTIVE。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HrSalaryBpmCallback implements BpmBizCallback {

    /** 审计目标类型。 */
    private static final String AUDIT_TARGET = "HR_SALARY_CHANGE";

    /** 审计脱敏字段（金额仅以密文进入审计 JSON）。 */
    private static final String AUDIT_MASKED_FIELDS =
            "baseSalary,postSalary,performanceBase,allowance";

    private final HrSalaryMapper salaryMapper;
    private final HrSalaryChangeApplyMapper changeApplyMapper;
    private final SalarySnapshotCodec snapshotCodec;
    private final BpmAuditService bpmAuditService;

    @Override
    public BpmBizType bizType() {
        return BpmBizType.HR_SALARY_CHANGE;
    }

    @Override
    public void onInstanceFinished(BpmInstance instance, BpmInstanceStatus status) {
        handle(instance, status);
    }

    /** 供薪资更正回调复用（更正模板与调薪模板共用同一套台账派生逻辑）。 */
    void handle(BpmInstance instance, BpmInstanceStatus status) {
        HrSalaryChangeApply apply = loadApply(instance);
        if (apply == null) {
            return;
        }
        if (apply.getStatus() == SalaryChangeStatus.APPROVED) {
            log.info("薪资变动申请 {} 已审批通过，跳过重复回调", apply.getId());
            return;
        }
        if (status == BpmInstanceStatus.APPROVED) {
            approve(apply);
            return;
        }
        if (status == BpmInstanceStatus.REJECTED
                || status == BpmInstanceStatus.WITHDRAWN
                || status == BpmInstanceStatus.TERMINATED) {
            reject(apply, status);
            return;
        }
        log.debug("薪资变动申请 {} 收到非终态 {}，忽略", apply.getId(), status);
    }

    /** 审批通过：生成新 ACTIVE 台账并把旧记录转为 HISTORY。 */
    private void approve(HrSalaryChangeApply apply) {
        SalarySnapshotCodec.SalarySnapshot after = snapshotCodec.decode(apply.getAfterSnapshot());
        if (after == null) {
            throw BusinessException.of(
                    ErrorCode.BIZ_UNPROCESSABLE, "薪资变动申请缺少变更后快照，无法生效");
        }
        Long generated =
                salaryMapper.selectCount(
                        new LambdaQueryWrapper<HrSalary>()
                                .eq(HrSalary::getChangeApplyId, apply.getId()));
        if (generated == null || generated == 0) {
            HrSalary current = selectActive(apply.getEmployeeId());
            if (current != null) {
                current.setStatus(SalaryStatus.HISTORY);
                salaryMapper.updateById(current);
            }
            HrSalary salary = new HrSalary();
            salary.setEmployeeId(apply.getEmployeeId());
            salary.setEffectiveDate(
                    apply.getEffectiveDate() != null
                            ? apply.getEffectiveDate()
                            : after.effectiveDate());
            salary.setBaseSalaryEnc(toPlain(after.baseSalary()));
            salary.setPostSalaryEnc(toPlain(after.postSalary()));
            salary.setPerformanceBaseEnc(toPlain(after.performanceBase()));
            salary.setAllowanceEnc(toPlain(after.allowance()));
            salary.setStatus(SalaryStatus.ACTIVE);
            salary.setChangeApplyId(apply.getId());
            salary.setRemark("薪资变动申请 " + apply.getId() + " 审批通过");
            salaryMapper.insert(salary);
            log.info(
                    "薪资变动申请 {} 生效，员工 {} 生成新 ACTIVE 台账 {}",
                    apply.getId(),
                    apply.getEmployeeId(),
                    salary.getId());
        } else {
            log.info("薪资变动申请 {} 已生成生效台账，跳过重复插入", apply.getId());
        }
        apply.setStatus(SalaryChangeStatus.APPROVED);
        changeApplyMapper.updateById(apply);
        recordAudit(apply);
    }

    /** 驳回 / 撤回 / 终止：只改申请单状态，不触碰台账。 */
    private void reject(HrSalaryChangeApply apply, BpmInstanceStatus status) {
        apply.setStatus(SalaryChangeStatus.REJECTED);
        changeApplyMapper.updateById(apply);
        log.info("薪资变动申请 {} 审批终态 {}，派生为 REJECTED", apply.getId(), status);
    }

    /** 写审计快照：before/after 直接复用密文 JSON，避免明文金额落审计表。 */
    private void recordAudit(HrSalaryChangeApply apply) {
        bpmAuditService.record(
                AUDIT_TARGET,
                apply.getId(),
                AuditAction.APPROVE,
                snapshotCodec.toJsonNode(apply.getBeforeSnapshot()),
                snapshotCodec.toJsonNode(apply.getAfterSnapshot()),
                AUDIT_MASKED_FIELDS,
                SecurityUtils.getCurrentUserIdOrDefault());
    }

    private HrSalary selectActive(Long employeeId) {
        return salaryMapper.selectOne(
                new LambdaQueryWrapper<HrSalary>()
                        .eq(HrSalary::getEmployeeId, employeeId)
                        .eq(HrSalary::getStatus, SalaryStatus.ACTIVE)
                        .last("LIMIT 1"));
    }

    private HrSalaryChangeApply loadApply(BpmInstance instance) {
        if (instance == null || instance.getBizId() == null) {
            return null;
        }
        return changeApplyMapper.selectById(instance.getBizId());
    }

    private String toPlain(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }
}
