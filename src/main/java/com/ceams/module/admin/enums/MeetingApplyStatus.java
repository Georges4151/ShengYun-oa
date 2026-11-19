package com.ceams.module.admin.enums;

import java.util.Locale;

/** 会议室预定状态（admin_meeting_apply.status）。 */
public enum MeetingApplyStatus {
    /** 审批中。 */
    APPROVING,
    /** 审批通过。 */
    APPROVED,
    /** 已驳回 / 撤回 / 终止。 */
    REJECTED,
    /** 已取消。 */
    CANCELLED;

    /** 编码解析，未知返回 null。 */
    public static MeetingApplyStatus from(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return valueOf(code.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
