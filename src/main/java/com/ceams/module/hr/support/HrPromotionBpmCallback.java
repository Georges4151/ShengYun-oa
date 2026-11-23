package com.ceams.module.hr.support;

import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.support.BpmBizCallback;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.entity.HrEmployeeChange;
import com.ceams.module.hr.enums.HrApprovalStatus;
import com.ceams.module.hr.mapper.HrEmployeeChangeMapper;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 晋升审批回调：通过后立即将员工职级更新为目标职级；驳回/撤回/终止只改申请单状态。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HrPromotionBpmCallback implements BpmBizCallback {

    private final HrEmployeeChangeMapper changeMapper;
    private final HrEmployeeMapper employeeMapper;

    @Override
    public BpmBizType bizType() {
        return BpmBizType.HR_PROMOTION;
    }

    @Override
    public void onInstanceFinished(BpmInstance instance, BpmInstanceStatus status) {
        HrEmployeeChange change = loadChange(instance);
        if (change == null) {
            return;
        }
        HrApprovalStatus target = HrBpmStatusResolver.resolve(status);
        if (target == null) {
            return;
        }
        if (target == HrApprovalStatus.APPROVED) {
            HrEmployee employee = employeeMapper.selectById(change.getEmployeeId());
            if (employee == null) {
                // 员工档案缺失时仍需落终态，避免申请单长期占用「同员工同类型待审批」校验
                log.warn(
                        "晋升申请 {} 对应员工 {} 不存在，跳过职级回写，仅更新申请单状态",
                        change.getId(),
                        change.getEmployeeId());
            } else if (change.getTargetLevelId() != null) {
                employee.setLevelId(change.getTargetLevelId());
                employeeMapper.updateById(employee);
                log.info(
                        "员工 {} 晋升立即生效，职级 {}",
                        employee.getId(),
                        change.getTargetLevelId());
            }
        }
        change.setApprovalStatus(target);
        changeMapper.updateById(change);
        log.info("晋升申请 {} 审批状态派生为 {}", change.getId(), target);
    }

    private HrEmployeeChange loadChange(BpmInstance instance) {
        if (instance == null || instance.getBizId() == null) {
            return null;
        }
        return changeMapper.selectById(instance.getBizId());
    }
}
