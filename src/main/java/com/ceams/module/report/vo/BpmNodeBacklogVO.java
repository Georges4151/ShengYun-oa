package com.ceams.module.report.vo;

import lombok.Data;

/** 审批效率报表：待办积压节点行（bpm_node 无节点名称列，直接以 node_key 标识）。 */
@Data
public class BpmNodeBacklogVO {

    /** 节点编码（bpm_node.node_key）。 */
    private String nodeKey;

    /** 该节点待处理任务数。 */
    private long pendingCount;
}
