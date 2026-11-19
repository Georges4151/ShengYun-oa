package com.ceams.module.hr.support;

import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.support.BpmBizCallback;
import com.ceams.module.hr.entity.HrLeaveRequest;
import com.ceams.module.hr.enums.HrApprovalStatus;
import com.ceams.module.hr.mapper.HrLeaveRequestMapper;
import com.ceams.module.hr.service.HrLeaveQuotaService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 请假审批回调：通过后按天数扣减假期额度；驳回/撤回/终止只改申请单状态。
 *
 * <p>额度扣减使用原子 UPDATE，额度不足时抛 BIZ_STATE_CONFLICT 以回滚审批事务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HrLeaveBpmCallback implements BpmBizCallback {

    private final HrLeaveRequestMapper leaveRequestMapper;
    private final HrLeaveQuotaService leaveQuotaService;

    @Override
    public BpmBizType bizType() {
        return BpmBizType.HR_LEAVE;
    }

    @Override
    public void onInstanceFinished(BpmInstance instance, BpmInstanceStatus status) {
        HrLeaveRequest leaveRequest = loadLeaveRequest(instance);
        if (leaveRequest == null) {
            return;
        }
        HrApprovalStatus target = HrBpmStatusResolver.resolve(status);
        if (target == null) {
            return;
        }
        if (target == HrApprovalStatus.APPROVED) {
            int year =
                    leaveRequest.getStartDate() == null
                            ? LocalDate.now().getYear()
                            : leaveRequest.getStartDate().getYear();
            leaveQuotaService.deduct(
                    leaveRequest.getEmployeeId(),
                    year,
                    leaveRequest.getLeaveType(),
                    leaveRequest.getDays());
            log.info(
                    "员工 {} 请假 {} 天审批通过，已扣减 {} 年 {} 额度",
                    leaveRequest.getEmployeeId(),
                    leaveRequest.getDays(),
                    year,
                    leaveRequest.getLeaveType());
        }
        leaveRequest.setApprovalStatus(target);
        leaveRequestMapper.updateById(leaveRequest);
        log.info("请假申请 {} 审批状态派生为 {}", leaveRequest.getId(), target);
    }

    private HrLeaveRequest loadLeaveRequest(BpmInstance instance) {
        if (instance == null || instance.getBizId() == null) {
            return null;
        }
        return leaveRequestMapper.selectById(instance.getBizId());
    }
}
