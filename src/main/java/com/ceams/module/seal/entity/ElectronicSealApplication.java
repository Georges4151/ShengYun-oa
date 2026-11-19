package com.ceams.module.seal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.seal.enums.ApplicationStatus;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 用印申请（biz_electronic_seal_application）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_electronic_seal_application")
public class ElectronicSealApplication extends BaseEntity {

    /** 印章 ID（逻辑引用 biz_electronic_seal.id）。 */
    private Long sealId;

    /** 印章名（冗余）。 */
    private String sealName;

    /** 申请人 ID（逻辑引用 sys_user.id）。 */
    private Long applicantId;

    /** 申请人姓名（冗余）。 */
    private String applicantName;

    /** 用印事由。 */
    private String purpose;

    /** 状态：PENDING/APPROVED/REJECTED/CANCELLED。 */
    private ApplicationStatus status = ApplicationStatus.PENDING;

    /** 项目 ID（逻辑引用 biz_project.id）。 */
    private Long projectId;

    /** 项目名（冗余）。 */
    private String projectName;

    /** 合同名。 */
    private String contractName;

    /** 合同文件路径。 */
    private String contractPath;

    /** 用印位置。 */
    private String stampPosition;

    /** 审批人 ID（逻辑引用 sys_user.id）。 */
    private Long approverId;

    /** 审批人姓名（冗余）。 */
    private String approverName;

    /** 审批意见。 */
    private String approvalComment;

    /** 申请时间。 */
    private LocalDateTime applyTime;

    /** 审批时间。 */
    private LocalDateTime approvalTime;
}
