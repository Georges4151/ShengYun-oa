package com.ceams.module.bpm.vo;

import com.ceams.module.bpm.enums.CcReadStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import lombok.Data;

/** 抄送记录视图。 */
@Data
public class CcRecordVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long instanceId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long nodeId;
    private String nodeKey;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long receiverId;
    private CcReadStatus readStatus;
    private LocalDateTime createTime;

    /** 冗余实例信息。 */
    private String bizType;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long bizId;

    /** 发起人与当前节点名称（列表批量装配，避免前端二次查询）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long initiatorId;

    private String initiatorName;
    private String currentNodeName;
}
