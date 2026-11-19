package com.ceams.module.bpm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 流程实例（bpm_instance）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bpm_instance")
public class BpmInstance extends BaseEntity {

    /** 模板版本 ID（历史实例锁定原版本）。 */
    private Long templateVersionId;

    /** 业务类型。 */
    private String bizType;

    /** 业务单据 ID。 */
    private Long bizId;

    /** 发起人 ID。 */
    private Long initiatorId;

    /** 实例状态。 */
    private BpmInstanceStatus status = BpmInstanceStatus.RUNNING;

    /** 发起时间。 */
    private LocalDateTime startedAt;

    /** 结束时间。 */
    private LocalDateTime endedAt;
}
