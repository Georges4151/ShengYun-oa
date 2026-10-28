package com.ceams.module.expense.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.expense.enums.ApprovalRecordStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 审批记录（biz_approval_record）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_approval_record")
public class ApprovalRecord extends BaseEntity {

    /** 票据 ID（逻辑引用 biz_receipt.id）。 */
    private Long receiptId;

    /** 审批级别（第几级）。 */
    private Integer approvalLevel;

    /** 审批人角色（代码值）。 */
    private String approverRole;

    /** 审批人 ID（逻辑引用 sys_user.id）。 */
    private Long approverId;

    /** 审批人姓名（冗余）。 */
    private String approverName;

    /** 审批结果。 */
    private ApprovalRecordStatus status;

    /** 审批意见。 */
    private String approvalComment;

    /** 审批时间。 */
    private LocalDateTime approvalTime;
}
