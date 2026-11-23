package com.ceams.module.bpm.vo;

import com.ceams.module.bpm.enums.BpmNodeStatus;
import com.ceams.module.bpm.enums.BpmNodeType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 节点实例视图。 */
@Data
public class NodeVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String nodeKey;

    /** 节点名称（来自模板定义）。 */
    private String nodeName;

    private BpmNodeType nodeType;
    private Integer seqNo;
    private BpmNodeStatus status;
    private LocalDateTime arrivedAt;
    private LocalDateTime completedAt;
    private List<TaskVO> tasks;

    /** 节点审批人：已到达节点为任务审批人，未到达节点为预计审批人。 */
    private List<FlowAssigneeVO> assignees;

    /** 是否为预计审批人（节点尚未到达）。 */
    private Boolean predicted;

    /** 审批人来源规则描述（解析不到个人时展示）。 */
    private String assigneeRule;
}
