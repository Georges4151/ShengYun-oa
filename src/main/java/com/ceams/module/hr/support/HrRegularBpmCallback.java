package com.ceams.module.hr.support;

import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.support.BpmBizCallback;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.entity.HrEmployeeChange;
import com.ceams.module.hr.enums.EmployeeStatus;
import com.ceams.module.hr.enums.HrApprovalStatus;
import com.ceams.module.hr.mapper.HrEmployeeChangeMapper;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 转正审批回调：通过后员工状态置为 ACTIVE 并写入转正日期；驳回/撤回/终止只改申请单状态。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HrRegularBpmCallback implements BpmBizCallback {

    private final HrEmployeeChangeMapper changeMapper;
    private final HrEmployeeMapper employeeMapper;

    @Override
    public BpmBizType bizType() {
        return BpmBizType.HR_REGULAR;
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
                log.warn("转正审批 {} 对应员工 {} 不存在，忽略业务回写", change.getId(), change.getEmployeeId());
                return;
            }
            employee.setStatus(EmployeeStatus.ACTIVE);
            employee.setRegularDate(change.getEffectiveDate());
            employeeMapper.updateById(employee);
            log.info(
                    "员工 {} 转正生效，转正日期 {}",
                    employee.getId(),
                    change.getEffectiveDate());
        }
        change.setApprovalStatus(target);
        changeMapper.updateById(change);
        log.info("转正申请 {} 审批状态派生为 {}", change.getId(), target);
    }

    private HrEmployeeChange loadChange(BpmInstance instance) {
        if (instance == null || instance.getBizId() == null) {
            return null;
        }
        return changeMapper.selectById(instance.getBizId());
    }
}
