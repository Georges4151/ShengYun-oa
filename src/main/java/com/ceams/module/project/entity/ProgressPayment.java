package com.ceams.module.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.project.enums.ApprovalStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 进度款（biz_progress_payment）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_progress_payment")
public class ProgressPayment extends BaseEntity {

    /** 项目 ID（逻辑引用 biz_project.id）。 */
    private Long projectId;

    /** 期次（如 第1期）。 */
    private String paymentPeriod;

    /** 申请金额。 */
    private BigDecimal applicationAmount;

    /** 批准金额。 */
    private BigDecimal approvedAmount;

    /** 实付金额。 */
    private BigDecimal paymentAmount;

    /** 申请日期。 */
    private LocalDateTime applicationDate;

    /** 审批日期。 */
    private LocalDateTime approvalDate;

    /** 付款日期。 */
    private LocalDateTime paymentDate;

    /** 审批状态。 */
    private ApprovalStatus status;

    /** 申请人 ID（逻辑引用 sys_user.id）。 */
    private Long applicationBy;

    /** 审批人 ID（逻辑引用 sys_user.id）。 */
    private Long approvedBy;
}