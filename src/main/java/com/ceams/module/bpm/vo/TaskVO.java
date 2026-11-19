package com.ceams.module.bpm.vo;

import com.ceams.module.bpm.enums.BpmAssigneeSource;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.enums.BpmTaskStatus;
import com.ceams.module.bpm.enums.TaskType;
import java.time.LocalDateTime;
import lombok.Data;

/** 审批任务视图（统一待办）。 */
@Data
public class TaskVO {

    private Long id;
    private Long instanceId;
    private Long nodeId;
    private String nodeKey;
    private Long assigneeId;

    /** 被代理人 ID：有值表示当前任务由代理人处理，本字段为原审批人。 */
    private Long ownerAssigneeId;

    /** 被代理人姓名（配合 ownerAssigneeId 展示“代理人（代 XXX）”）。 */
    private String ownerAssigneeName;

    private BpmAssigneeSource sourceType;

    /** 任务版本号（乐观锁），同意/驳回/改派时必须回传。 */
    private Integer taskVersion;
    private TaskType taskType;
    private BpmTaskStatus status;
    private LocalDateTime dueAt;

    /** 是否已超时。 */
    private Boolean overdue;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 冗余实例信息，避免前端二次查询。 */
    private String bizType;
    private Long bizId;
    private BpmInstanceStatus instanceStatus;
    private Long initiatorId;

    /** 发起人姓名（列表批量装配，避免前端二次查询）。 */
    private String initiatorName;

    /** 当前节点名称（由模板 schema 的 nodeName 解析，解析失败时为 null）。 */
    private String currentNodeName;
}
