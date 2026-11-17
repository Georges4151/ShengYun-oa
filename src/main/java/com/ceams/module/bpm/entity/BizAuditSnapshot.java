package com.ceams.module.bpm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 业务变更审计快照（biz_audit_snapshot）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_audit_snapshot")
public class BizAuditSnapshot extends BaseEntity {

    /** 目标类型：BPM_TEMPLATE/BPM_INSTANCE/BPM_TASK。 */
    private String targetType;

    /** 目标 ID。 */
    private Long targetId;

    /** 动作。 */
    private String action;

    /** 变更前 JSON。 */
    private String beforeJson;

    /** 变更后 JSON。 */
    private String afterJson;

    /** 脱敏字段清单（英文逗号分隔）。 */
    private String maskedFields;

    /** 操作人 ID。 */
    private Long operatorId;

    /** 链路追踪 ID。 */
    private String traceId;
}
