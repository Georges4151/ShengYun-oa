package com.ceams.module.bpm.support;

import com.ceams.module.bpm.enums.BpmNodeType;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 模板 JSON 中的单个节点定义。 */
@Data
public class BpmSchemaNode {

    /** 节点编码（实例内唯一）。 */
    private String nodeKey;

    /** 节点名称。 */
    private String nodeName;

    /** 顺序号（升序流转）。 */
    private Integer seqNo;

    /** 归一化后的节点类型。 */
    private BpmNodeType nodeType = BpmNodeType.SINGLE;

    /** 审批人来源配置。 */
    private List<BpmAssigneeSourceConfig> assignees = new ArrayList<>();

    /** 节点超时时长（小时）；为空时取全局默认值。 */
    private Integer dueHours;

    /** 显式后继节点编码（可选；为空时按 seqNo 顺序流转）。 */
    private String nextNodeKey;

    /** 条件/分支节点的默认出口编码（条件不命中时使用）。 */
    private String defaultNext;

    /** 条件表达式列表（按顺序求值，命中即取 targetNodeKey）。 */
    private List<BpmSchemaCondition> conditions = new ArrayList<>();

    /** 审批人仅含发起人本人时自动通过（避免自己审自己）。 */
    private Boolean autoApproveIfInitiator = Boolean.FALSE;

    /** 节点自动通过条件：命中任一条件即自动完成节点，不生成待办。 */
    private List<BpmSchemaCondition> autoApproveConditions = new ArrayList<>();
}
