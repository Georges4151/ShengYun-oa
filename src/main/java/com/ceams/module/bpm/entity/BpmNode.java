package com.ceams.module.bpm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.bpm.enums.BpmNodeStatus;
import com.ceams.module.bpm.enums.BpmNodeType;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 流程节点实例（bpm_node）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bpm_node")
public class BpmNode extends BaseEntity {

    /** 实例 ID。 */
    private Long instanceId;

    /** 节点编码（模板内唯一）。 */
    private String nodeKey;

    /** 节点类型。 */
    private BpmNodeType nodeType;

    /** 节点顺序号。 */
    private Integer seqNo;

    /** 节点状态。 */
    private BpmNodeStatus status = BpmNodeStatus.PENDING;

    /** 到达时间。 */
    private LocalDateTime arrivedAt;

    /** 完成时间。 */
    private LocalDateTime completedAt;
}
