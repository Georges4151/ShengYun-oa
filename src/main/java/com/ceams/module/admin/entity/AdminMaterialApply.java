package com.ceams.module.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.admin.enums.MaterialApplyStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 行政易耗品申领单（admin_material_apply）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_material_apply")
public class AdminMaterialApply extends BaseEntity {

    /** 物料 ID（逻辑引用 admin_material.id）。 */
    private Long materialId;

    /** 申请人 ID（sys_user.id）。 */
    private Long applicantId;

    /** 申领数量（>0）。 */
    private BigDecimal quantity;

    /** 申领事由。 */
    private String reason;

    /** BPM 实例 ID。 */
    private Long bpmInstanceId;

    /** 状态：DRAFT/APPROVING/APPROVED/REJECTED/ISSUED。 */
    private MaterialApplyStatus status;

    /** 发放时间。 */
    private LocalDateTime issuedAt;

    /** 发放人 ID。 */
    private Long issuedBy;
}
