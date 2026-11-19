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
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 调岗审批回调：通过后同步更新员工部门/岗位，并同步系统用户的部门归属。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HrTransferBpmCallback implements BpmBizCallback {

    private final HrEmployeeChangeMapper changeMapper;
    private final HrEmployeeMapper employeeMapper;
    private final UserMapper userMapper;

    @Override
    public BpmBizType bizType() {
        return BpmBizType.HR_TRANSFER;
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
                log.warn("调岗审批 {} 对应员工 {} 不存在，忽略业务回写", change.getId(), change.getEmployeeId());
                return;
            }
            if (change.getTargetDeptId() != null) {
                employee.setDeptId(change.getTargetDeptId());
            }
            if (change.getTargetPositionId() != null) {
                employee.setPositionId(change.getTargetPositionId());
            }
            employeeMapper.updateById(employee);
            syncUserDept(employee.getUserId(), change.getTargetDeptId());
            log.info(
                    "员工 {} 调岗生效，部门 {}，岗位 {}",
                    employee.getId(),
                    employee.getDeptId(),
                    employee.getPositionId());
        }
        change.setApprovalStatus(target);
        changeMapper.updateById(change);
        log.info("调岗申请 {} 审批状态派生为 {}", change.getId(), target);
    }

    /** 同步 sys_user.dept_id，保证 BPM 数据范围与组织架构一致。 */
    private void syncUserDept(Long userId, Long deptId) {
        if (userId == null || deptId == null) {
            return;
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("调岗同步部门失败：用户 {} 不存在", userId);
            return;
        }
        user.setDeptId(deptId);
        userMapper.updateById(user);
    }

    private HrEmployeeChange loadChange(BpmInstance instance) {
        if (instance == null || instance.getBizId() == null) {
            return null;
        }
        return changeMapper.selectById(instance.getBizId());
    }
}
