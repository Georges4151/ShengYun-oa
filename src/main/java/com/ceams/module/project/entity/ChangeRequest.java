package com.ceams.module.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.project.enums.ApprovalStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 变更请求（biz_change_request）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_change_request")
public class ChangeRequest extends BaseEntity {

    /** 项目 ID（逻辑引用 biz_project.id）。 */
    private Long projectId;

    /** 变更类型（代码值）。 */
    private String requestType;

    /** 变更说明。 */
    private String description;

    /** 预估成本。 */
    private BigDecimal estimatedCost;

    /** 审批状态。 */
    private ApprovalStatus status;

    /** 申请人 ID（逻辑引用 sys_user.id）。 */
    private Long requestedBy;

    /** 审批人 ID（逻辑引用 sys_user.id）。 */
    private Long approvedBy;

    /** 审批时间。 */
    private LocalDateTime approvedAt;

    /** 审批意见。 */
    private String approvalComment;
}