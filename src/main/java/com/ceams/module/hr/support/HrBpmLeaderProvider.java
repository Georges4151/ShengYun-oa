package com.ceams.module.hr.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.support.BpmLeaderProvider;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.entity.HrEmployeeChange;
import com.ceams.module.hr.entity.HrCheckinSupplement;
import com.ceams.module.hr.entity.HrLeaveRequest;
import com.ceams.module.hr.entity.HrPerformanceReview;
import com.ceams.module.hr.entity.HrSalaryChangeApply;
import com.ceams.module.hr.enums.EmployeeStatus;
import com.ceams.module.hr.mapper.HrCheckinSupplementMapper;
import com.ceams.module.hr.mapper.HrEmployeeChangeMapper;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.mapper.HrLeaveRequestMapper;
import com.ceams.module.hr.mapper.HrPerformanceReviewMapper;
import com.ceams.module.hr.mapper.HrSalaryChangeApplyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * HR 侧直属上级解析实现（V50 起统一以 {@code hr_employee.leader_id} 为准）。
 *
 * <p>审批人解析只读取员工档案：先按业务单据定位员工，再取该员工的直属上级员工，
 * 最终返回上级员工绑定的系统用户 ID。任一环节缺失（员工不存在、未维护上级、
 * 上级已离职或未绑定账号）均记录告警并返回 null，由 BPM 侧落 PENDING_ASSIGNEE。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HrBpmLeaderProvider implements BpmLeaderProvider {

    private final HrEmployeeMapper employeeMapper;
    private final HrEmployeeChangeMapper employeeChangeMapper;
    private final HrPerformanceReviewMapper performanceReviewMapper;
    private final HrLeaveRequestMapper leaveRequestMapper;
    private final HrCheckinSupplementMapper checkinSupplementMapper;
    private final HrSalaryChangeApplyMapper salaryChangeApplyMapper;

    @Override
    public Long resolveLeaderUserId(BpmBizType bizType, Long bizId) {
        if (bizType == null || bizId == null) {
            return null;
        }
        Long employeeId = resolveEmployeeId(bizType, bizId);
        if (employeeId == null) {
            log.warn("直属上级解析为空：业务类型={}，业务单据={} 未定位到员工", bizType, bizId);
            return null;
        }
        return resolveLeaderUserIdByEmployeeId(employeeId);
    }

    @Override
    public Long resolveLeaderUserIdByUser(Long userId) {
        if (userId == null) {
            return null;
        }
        HrEmployee employee =
                employeeMapper.selectOne(
                        new LambdaQueryWrapper<HrEmployee>()
                                .eq(HrEmployee::getUserId, userId)
                                .last("LIMIT 1"));
        if (employee == null) {
            log.warn("直属上级解析为空：用户 {} 未绑定员工档案", userId);
            return null;
        }
        return resolveLeaderUserIdByEmployeeId(employee.getId());
    }

    /** 按业务类型定位业务单据上的员工 ID；未覆盖的业务类型返回 null。 */
    private Long resolveEmployeeId(BpmBizType bizType, Long bizId) {
        switch (bizType) {
            case HR_REGULAR:
            case HR_TRANSFER:
            case HR_RESIGN:
            case HR_PROMOTION:
                HrEmployeeChange change = employeeChangeMapper.selectById(bizId);
                return change == null ? null : change.getEmployeeId();
            case HR_PERFORMANCE:
                HrPerformanceReview review = performanceReviewMapper.selectById(bizId);
                return review == null ? null : review.getEmployeeId();
            case HR_LEAVE:
                HrLeaveRequest leave = leaveRequestMapper.selectById(bizId);
                return leave == null ? null : leave.getEmployeeId();
            case HR_CHECKIN_SUPPLEMENT:
                HrCheckinSupplement supplement = checkinSupplementMapper.selectById(bizId);
                return supplement == null ? null : supplement.getEmployeeId();
            case HR_SALARY_CHANGE:
            case HR_SALARY_CORRECTION:
                HrSalaryChangeApply apply = salaryChangeApplyMapper.selectById(bizId);
                return apply == null ? null : apply.getEmployeeId();
            default:
                return null;
        }
    }

    /** 员工 ID -> 直属上级员工绑定的系统用户 ID。 */
    private Long resolveLeaderUserIdByEmployeeId(Long employeeId) {
        HrEmployee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            log.warn("直属上级解析为空：员工 {} 不存在", employeeId);
            return null;
        }
        Long leaderEmployeeId = employee.getLeaderId();
        if (leaderEmployeeId == null) {
            log.warn("直属上级解析为空：员工 {} 未维护直属上级", employeeId);
            return null;
        }
        HrEmployee leader = employeeMapper.selectById(leaderEmployeeId);
        if (leader == null) {
            log.warn("直属上级解析为空：员工 {} 的上级员工 {} 不存在", employeeId, leaderEmployeeId);
            return null;
        }
        if (leader.getStatus() == EmployeeStatus.TERMINATED) {
            log.warn("直属上级解析为空：员工 {} 的上级员工 {} 已离职", employeeId, leaderEmployeeId);
            return null;
        }
        if (leader.getUserId() == null) {
            log.warn("直属上级解析为空：员工 {} 的上级员工 {} 未绑定系统账号", employeeId, leaderEmployeeId);
            return null;
        }
        return leader.getUserId();
    }
}
