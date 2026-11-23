package com.ceams.module.bpm.vo;

import com.ceams.module.bpm.enums.BpmNodeType;
import java.util.List;
import lombok.Data;

/** 发起前流程预览节点。 */
@Data
public class PreviewNodeVO {

    private String nodeKey;
    private String nodeName;
    private BpmNodeType nodeType;
    private Integer seqNo;

    /** 预计审批人（可能为空）。 */
    private List<FlowAssigneeVO> assignees;

    /** 预览节点均为预计，固定 true。 */
    private Boolean predicted = Boolean.TRUE;

    /** 审批人来源规则描述，用于无法解析到个人时展示。 */
    private String assigneeRule;
}
