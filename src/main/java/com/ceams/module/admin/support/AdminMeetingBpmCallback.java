package com.ceams.module.admin.support;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.admin.entity.AdminMeetingApply;
import com.ceams.module.admin.enums.MeetingApplyStatus;
import com.ceams.module.admin.mapper.AdminMeetingApplyMapper;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.support.BpmBizCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 会议室预定审批回调。
 *
 * <p>通过：先以排除自身的口径二次校验时段冲突，冲突抛 {@code BIZ_STATE_CONFLICT} 回滚审批；
 * 校验通过后申请 APPROVING → APPROVED。驳回 / 撤回 / 终止：申请置 REJECTED。
 *
 * <p>幂等：申请已是 APPROVED / REJECTED 直接返回。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminMeetingBpmCallback implements BpmBizCallback {

    private final AdminMeetingApplyMapper applyMapper;
    private final AdminConflictGuard conflictGuard;

    @Override
    public BpmBizType bizType() {
        return BpmBizType.ADMIN_MEETING_APPLY;
    }

    @Override
    public void onInstanceFinished(BpmInstance instance, BpmInstanceStatus status) {
        if (instance == null || instance.getBizId() == null) {
            return;
        }
        AdminMeetingApply apply = applyMapper.selectById(instance.getBizId());
        if (apply == null) {
            log.warn("会议室预定 {} 不存在，忽略回调", instance.getBizId());
            return;
        }
        if (apply.getStatus() == MeetingApplyStatus.APPROVED
                || apply.getStatus() == MeetingApplyStatus.REJECTED) {
            log.info("会议室预定 {} 已是 {}，跳过重复回调", apply.getId(), apply.getStatus());
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
        log.debug("会议室预定 {} 收到非终态 {}，忽略", apply.getId(), status);
    }

    /** 审批通过：二次冲突校验（排除自身）后置 APPROVED。 */
    private void approve(AdminMeetingApply apply) {
        if (conflictGuard.hasMeetingConflict(
                apply.getRoomId(),
                apply.getStartTime(),
                apply.getEndTime(),
                apply.getId())) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "会议室该时段已被占用，请改期或驳回");
        }

        AdminMeetingApply patch = new AdminMeetingApply();
        patch.setVersion(null);
        patch.setStatus(MeetingApplyStatus.APPROVED);
        int rows =
                applyMapper.update(
                        patch,
                        new LambdaUpdateWrapper<AdminMeetingApply>()
                                .eq(AdminMeetingApply::getId, apply.getId())
                                .eq(AdminMeetingApply::getStatus, MeetingApplyStatus.APPROVING));
        if (rows == 0) {
            log.info("会议室预定 {} 状态已变更，跳过重复回调", apply.getId());
            return;
        }
        log.info("会议室预定 {} 审批通过，会议室 {}", apply.getId(), apply.getRoomId());
    }

    /** 驳回 / 撤回 / 终止：预定置 REJECTED。 */
    private void reject(AdminMeetingApply apply) {
        AdminMeetingApply patch = new AdminMeetingApply();
        patch.setVersion(null);
        patch.setStatus(MeetingApplyStatus.REJECTED);
        applyMapper.update(
                patch,
                new LambdaUpdateWrapper<AdminMeetingApply>()
                        .eq(AdminMeetingApply::getId, apply.getId())
                        .eq(AdminMeetingApply::getStatus, MeetingApplyStatus.APPROVING));
        log.info("会议室预定 {} 审批驳回，置 REJECTED", apply.getId());
    }
}
