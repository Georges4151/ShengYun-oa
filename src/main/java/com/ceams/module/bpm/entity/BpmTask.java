package com.ceams.module.bpm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.bpm.enums.BpmAssigneeSource;
import com.ceams.module.bpm.enums.BpmTaskStatus;
import com.ceams.module.bpm.enums.TaskType;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 审批任务（bpm_task），全系统唯一待办来源。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bpm_task")
public class BpmTask extends BaseEntity {

    /** 实例 ID。 */
    private Long instanceId;

    /** 节点 ID。 */
    private Long nodeId;

    /** 处理人 ID（解析为空时为 null）。 */
    private Long assigneeId;

    /** 被代理人 ID：有值表示 assignee_id 是代理处理人，本字段为原审批人。 */
    private Long ownerAssigneeId;

    /** 审批人来源。 */
    private BpmAssigneeSource sourceType;

    /** 任务类型。 */
    private TaskType taskType = TaskType.APPROVE;

    /** 任务状态。 */
    private BpmTaskStatus status = BpmTaskStatus.PENDING;

    /** 超时时间。 */
    private LocalDateTime dueAt;

    /** 超时提醒时间（NULL 表示尚未提醒；用于保证同一任务仅提醒一次）。 */
    private LocalDateTime remindedAt;
}
