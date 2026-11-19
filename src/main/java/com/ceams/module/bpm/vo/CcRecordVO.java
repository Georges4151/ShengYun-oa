package com.ceams.module.bpm.vo;

import com.ceams.module.bpm.enums.CcReadStatus;
import java.time.LocalDateTime;
import lombok.Data;

/** 抄送记录视图。 */
@Data
public class CcRecordVO {

    private Long id;
    private Long instanceId;
    private Long nodeId;
    private String nodeKey;
    private Long receiverId;
    private CcReadStatus readStatus;
    private LocalDateTime createTime;

    /** 冗余实例信息。 */
    private String bizType;
    private Long bizId;

    /** 发起人与当前节点名称（列表批量装配，避免前端二次查询）。 */
    private Long initiatorId;

    private String initiatorName;
    private String currentNodeName;
}
