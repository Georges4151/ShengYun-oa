package com.ceams.module.bpm.vo;

import com.ceams.module.bpm.enums.BpmNodeStatus;
import com.ceams.module.bpm.enums.BpmNodeType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 节点实例视图。 */
@Data
public class NodeVO {

    private Long id;
    private String nodeKey;
    private BpmNodeType nodeType;
    private Integer seqNo;
    private BpmNodeStatus status;
    private LocalDateTime arrivedAt;
    private LocalDateTime completedAt;
    private List<TaskVO> tasks;
}
