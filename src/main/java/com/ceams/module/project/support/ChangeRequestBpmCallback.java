package com.ceams.module.project.support;

import com.ceams.common.security.SecurityUtils;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.support.BpmBizCallback;
import com.ceams.module.project.entity.ChangeRequest;
import com.ceams.module.project.enums.ApprovalStatus;
import com.ceams.module.project.mapper.ChangeRequestMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 项目变更流程回调：把 BPM 审批结果写回变更单。
 *
 * <p>变更流程固定为「部门审批 → 结束」；业务表 {@code status} 仅作快照，
 * 唯一判断依据为 {@code bpm_instance.status}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChangeRequestBpmCallback implements BpmBizCallback {

    private final ChangeRequestMapper changeRequestMapper;

    @Override
    public BpmBizType bizType() {
        return BpmBizType.CHANGE_REQUEST;
    }

    @Override
    public void onInstanceFinished(BpmInstance instance, BpmInstanceStatus status) {
        if (instance == null || instance.getBizId() == null) {
            return;
        }
        ChangeRequest changeRequest = changeRequestMapper.selectById(instance.getBizId());
        if (changeRequest == null) {
            return;
        }
        ApprovalStatus target =
                switch (status) {
                    case APPROVED -> ApprovalStatus.APPROVED;
                    case REJECTED -> ApprovalStatus.REJECTED;
                    case WITHDRAWN -> ApprovalStatus.DRAFT;
                    case TERMINATED -> ApprovalStatus.CANCELLED;
                    default -> null;
                };
        if (target == null) {
            return;
        }
        changeRequest.setStatus(target);
        if (target == ApprovalStatus.APPROVED || target == ApprovalStatus.REJECTED) {
            changeRequest.setApprovedBy(SecurityUtils.getCurrentUserId());
            changeRequest.setApprovedAt(LocalDateTime.now());
        }
        changeRequestMapper.updateById(changeRequest);
        log.info("变更请求 {} 审批状态派生为 {}", changeRequest.getId(), target);
    }
}
