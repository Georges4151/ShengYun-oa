package com.ceams.module.bpm.support;

import com.ceams.module.bpm.enums.BpmAssigneeSource;
import java.util.List;
import lombok.Data;

/** 审批人解析结果（审批人 + 来源），用于同时写入快照与任务冗余字段。 */
@Data
public class BpmResolvedAssignee {

    /** 审批人 ID。 */
    private Long assigneeId;

    /** 来源类型。 */
    private BpmAssigneeSource sourceType;
}
