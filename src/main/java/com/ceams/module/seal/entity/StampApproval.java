package com.ceams.module.seal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.seal.enums.ApprovalStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用印审批（biz_stamp_approval）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_stamp_approval")
public class StampApproval extends BaseEntity {

    /** 用印申请 ID（逻辑引用 biz_electronic_seal_application.id）。 */
    private Long stampId;

    /** 印章 ID（逻辑引用 biz_electronic_seal.id）。 */
    private Long sealId;

    /** 印章名（冗余）。 */
    private String sealName;

    /** 印章编码（冗余）。 */
    private String sealCode;

    /** 印章权限。 */
    private String sealPermission;

    /** 项目 ID（逻辑引用 biz_project.id）。 */
    private Long projectId;

    /** 项目名（冗余）。 */
    private String projectName;

    /** 合同名。 */
    private String contractName;

    /** 合同文件路径。 */
    private String contractPath;

    /** 原始合同路径。 */
    private String originalContractPath;

    /** 用印位置（多位置）。 */
    private String stampPositions;

    /** 申请人 ID（逻辑引用 sys_user.id）。 */
    private Long applicantId;

    /** 申请人姓名（冗余）。 */
    private String applicantName;

    /** 审批人 ID（逻辑引用 sys_user.id）。 */
    private Long approverId;

    /** 审批人姓名（冗余）。 */
    private String approverName;

    /** 状态：PENDING/APPROVED/REJECTED。 */
    private ApprovalStatus status = ApprovalStatus.PENDING;

    /** 驳回原因。 */
    private String rejectReason;

    /** 申请时间。 */
    private LocalDateTime applyTime;

    /** 审批时间。 */
    private LocalDateTime approvalTime;
}
