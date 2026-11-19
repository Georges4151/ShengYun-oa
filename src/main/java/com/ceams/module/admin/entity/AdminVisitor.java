package com.ceams.module.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.common.crypto.SensitiveTypeHandler;
import com.ceams.module.admin.enums.VisitorStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 行政访客登记（admin_visitor）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "admin_visitor", autoResultMap = true)
public class AdminVisitor extends BaseEntity {

    /** 访客姓名。 */
    private String visitorName;

    /** 手机号（库内加密，接口脱敏）。 */
    @TableField(typeHandler = SensitiveTypeHandler.class)
    private String phoneEnc;

    /** 来访单位。 */
    private String company;

    /** 被访人 ID（sys_user.id）。 */
    private Long hostUserId;

    /** 来访日期。 */
    private LocalDate visitDate;

    /** 来访事由。 */
    private String purpose;

    /** 一次性核销令牌，核销后置 NULL。 */
    private String qrToken;

    /** 状态：PENDING/CHECKED_IN/CHECKED_OUT/CANCELLED。 */
    private VisitorStatus status;

    /** 进入时间。 */
    private LocalDateTime checkinTime;

    /** 离开时间。 */
    private LocalDateTime checkoutTime;
}
