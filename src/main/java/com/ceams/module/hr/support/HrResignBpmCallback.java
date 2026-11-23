package com.ceams.module.hr.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.support.BpmBizCallback;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.entity.HrEmployeeChange;
import com.ceams.module.hr.enums.ChangeType;
import com.ceams.module.hr.enums.EmployeeStatus;
import com.ceams.module.hr.enums.HrApprovalStatus;
import com.ceams.module.hr.mapper.HrEmployeeChangeMapper;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.enums.UserStatus;
import com.ceams.module.iam.mapper.UserMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 离职审批回调：通过后员工置为离职、写离职日期，并停用系统账号。
 *
 * <p>存在未归还资产时阻止离职生效（抛异常回滚审批事务，避免资产流失）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HrResignBpmCallback implements BpmBizCallback {

    private final HrEmployeeChangeMapper changeMapper;
    private final HrEmployeeMapper employeeMapper;
    private final UserMapper userMapper;

    @Override
    public BpmBizType bizType() {
        return BpmBizType.HR_RESIGN;
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
            if (hasUnreturnedAssets(change.getEmployeeId())) {
                throw BusinessException.of(
                        ErrorCode.BIZ_STATE_CONFLICT, "存在未归还资产，无法完成离职审批");
            }
            HrEmployee employee = employeeMapper.selectById(change.getEmployeeId());
            if (employee == null) {
                log.warn("离职审批 {} 对应员工 {} 不存在，忽略业务回写", change.getId(), change.getEmployeeId());
                return;
            }
            boolean alreadyTerminated = employee.getStatus() == EmployeeStatus.TERMINATED;
            employee.setStatus(EmployeeStatus.TERMINATED);
            employee.setLeaveDate(change.getEffectiveDate());
            employeeMapper.updateById(employee);
            disableUser(employee.getUserId());
            if (!alreadyTerminated) {
                transferSubordinates(employee, change);
            }
            log.info("员工 {} 离职生效，离职日期 {}", employee.getId(), change.getEffectiveDate());
        }
        change.setApprovalStatus(target);
        changeMapper.updateById(change);
        log.info("离职申请 {} 审批状态派生为 {}", change.getId(), target);
    }

    /**
     * 是否存在未归还资产。
     *
     * <p>M1 阶段资产模块尚未建表（M3 上线），此处恒返回 false；
     * M3 接入 admin_asset_borrow 后替换为真实校验。
     */
    private boolean hasUnreturnedAssets(Long employeeId) {
        return false;
    }

    /**
     * 把离职员工的在职直接下属批量转交给申请单指定的上级，并写入变更记录。
     *
     * <p>存在在职下属但未指定转交上级、或转交上级已失效时抛异常回滚审批事务，
     * 保证离职审批通过后不会留下断链的汇报关系。
     */
    private void transferSubordinates(HrEmployee leaving, HrEmployeeChange change) {
        List<HrEmployee> subordinates =
                employeeMapper.selectList(
                        new LambdaQueryWrapper<HrEmployee>()
                                .eq(HrEmployee::getLeaderId, leaving.getId())
                                .ne(HrEmployee::getStatus, EmployeeStatus.TERMINATED)
                                .orderByAsc(HrEmployee::getEmployeeNo));
        if (subordinates.isEmpty()) {
            return;
        }
        Long targetId = change.getResignLeaderId();
        if (targetId == null) {
            throw BusinessException.of(
                    ErrorCode.BIZ_STATE_CONFLICT,
                    "该员工名下还有 " + subordinates.size() + " 名在职下属，但离职申请未指定转交上级");
        }
        HrEmployee target = employeeMapper.selectById(targetId);
        if (target == null
                || target.getStatus() == EmployeeStatus.TERMINATED
                || target.getUserId() == null
                || targetId.equals(leaving.getId())) {
            throw BusinessException.of(
                    ErrorCode.BIZ_STATE_CONFLICT, "下属转交上级已失效，无法完成离职审批");
        }
        for (HrEmployee subordinate : subordinates) {
            if (subordinate.getId().equals(targetId)) {
                throw BusinessException.of(
                        ErrorCode.BIZ_STATE_CONFLICT, "下属转交上级不能是待转交的下属");
            }
            subordinate.setLeaderId(targetId);
            employeeMapper.updateById(subordinate);

            HrEmployeeChange record = new HrEmployeeChange();
            record.setEmployeeId(subordinate.getId());
            record.setChangeType(ChangeType.LEADER_TRANSFER);
            record.setResignLeaderId(targetId);
            record.setEffectiveDate(change.getEffectiveDate());
            record.setReason(
                    "直属上级 " + leaving.getName() + " 离职，自动转交至 " + target.getName());
            record.setApprovalStatus(HrApprovalStatus.APPROVED);
            changeMapper.insert(record);
        }
        log.info(
                "离职员工 {} 名下 {} 名下属已转交给 {}",
                leaving.getId(),
                subordinates.size(),
                targetId);
    }

    /** 停用绑定的系统账号（未绑定账号时跳过）。 */
    private void disableUser(Long userId) {
        if (userId == null) {
            return;
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("离职停用账号失败：用户 {} 不存在", userId);
            return;
        }
        user.setStatus(UserStatus.DISABLED);
        userMapper.updateById(user);
        log.info("离职流程停用系统账号 {}", userId);
    }

    private HrEmployeeChange loadChange(BpmInstance instance) {
        if (instance == null || instance.getBizId() == null) {
            return null;
        }
        return changeMapper.selectById(instance.getBizId());
    }
}
