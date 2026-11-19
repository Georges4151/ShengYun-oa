package com.ceams.module.bpm.enums;

/**
 * 节点类型（bpm_node.node_type）。
 *
 * <p>模板 JSON 中的 APPROVAL 节点会按 approveMode（ALL=会签 / ANY=或签）归一化为
 * COUNTERSIGN / OR_SIGN 落库；CONDITION 为条件节点、AUTO 为自动节点，M1 阶段仅做结构校验，
 * 到达后自动完成，条件求值留待后续里程碑。
 */
public enum BpmNodeType {
    /** 发起节点（虚拟节点，不生成待办）。 */
    START,
    /** 单人审批。 */
    SINGLE,
    /** 会签：全部同意才流转，任一驳回整体驳回。 */
    COUNTERSIGN,
    /** 或签：任一同意即流转，全部驳回才整体驳回。 */
    OR_SIGN,
    /** 抄送节点：只写抄送记录，不生成待办。 */
    CC,
    /** 条件节点（M1 仅做结构校验与默认出口，不执行表达式求值）。 */
    CONDITION,
    /** 自动节点（系统自动完成，不生成待办）。 */
    AUTO,
    /** 结束节点（虚拟节点）。 */
    END
}
