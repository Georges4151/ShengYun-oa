package com.ceams.module.bpm.vo;

import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 流程实例视图（列表与详情概要）。 */
@Data
public class InstanceVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long templateVersionId;
    private String bizType;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long bizId;

    @JsonSerialize(using = ToStringSerializer.class)
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
    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> currentAssigneeIds;
}
