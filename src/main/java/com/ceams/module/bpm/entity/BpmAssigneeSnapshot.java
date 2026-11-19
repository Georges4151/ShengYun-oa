package com.ceams.module.bpm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.bpm.enums.BpmAssigneeSource;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 审批人快照（bpm_assignee_snapshot）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bpm_assignee_snapshot")
public class BpmAssigneeSnapshot extends BaseEntity {

    /** 节点 ID。 */
    private Long nodeId;

    /** 审批人 ID。 */
    private Long assigneeId;

    /** 解析来源。 */
    private BpmAssigneeSource sourceType;

    /** 解析时间。 */
    private LocalDateTime resolvedAt;
}
