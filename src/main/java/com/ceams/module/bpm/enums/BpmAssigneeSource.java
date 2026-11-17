package com.ceams.module.bpm.enums;

/** 审批人来源类型（bpm_task.source_type / bpm_assignee_snapshot.source_type）。 */
public enum BpmAssigneeSource {
    /** 指定用户。 */
    USER,
    /** 指定角色。 */
    ROLE,
    /** 部门主管（现有 IAM 无部门字段，解析为空时落 PENDING_ASSIGNEE）。 */
    DEPT_LEADER,
    /** 发起人直属上级（现有 IAM 无上级字段，解析为空时落 PENDING_ASSIGNEE）。 */
    SUPERIOR,
    /** 发起人本人（驳回发起人后的回流任务）。 */
    INITIATOR,
    /** 管理员改派。 */
    DELEGATE,
    /** 用户自助代理：assignee_id 为代理人，owner_assignee_id 为原审批人。 */
    PROXY
}
