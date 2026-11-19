package com.ceams.module.hr.support;

import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.hr.enums.HrApprovalStatus;

/**
 * BPM 实例终态 -> HR 申请单审批状态的统一映射。
 *
 * <p>撤回语义在 HR 域统一为 WITHDRAWN，管理员终止统一为 CANCELLED（与 expense 口径一致）。
 */
public final class HrBpmStatusResolver {

    private HrBpmStatusResolver() {}

    /**
     * 映射终态；运行中等非终态返回 null，表示不需要写业务快照。
     *
     * @param status BPM 实例状态
     * @return HR 审批状态
     */
    public static HrApprovalStatus resolve(BpmInstanceStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case APPROVED -> HrApprovalStatus.APPROVED;
            case REJECTED -> HrApprovalStatus.REJECTED;
            case WITHDRAWN -> HrApprovalStatus.WITHDRAWN;
            case TERMINATED -> HrApprovalStatus.CANCELLED;
            default -> null;
        };
    }
}
