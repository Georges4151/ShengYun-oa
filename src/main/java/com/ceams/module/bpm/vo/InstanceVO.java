package com.ceams.module.bpm.vo;

import com.ceams.module.bpm.enums.BpmInstanceStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 流程实例视图（列表与详情概要）。 */
@Data
public class InstanceVO {

    private Long id;
    private Long templateVersionId;
    private String bizType;
    private Long bizId;
    private Long initiatorId;
    private BpmInstanceStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer version;
    private LocalDateTime createTime;

    /** 当前活动节点编码。 */
    private String currentNodeKey;

    /** 当前活动节点状态。 */
    private String currentNodeStatus;

    /** 当前节点处理人 ID 列表。 */
    private List<Long> currentAssigneeIds;
}
