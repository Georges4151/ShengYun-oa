package com.ceams.module.admin.vo;

import com.ceams.module.admin.enums.VisitorStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 访客展示对象（手机号已脱敏）。 */
@Data
public class VisitorVO {

    /** 访客登记 ID。 */
    private Long id;

    /** 访客姓名。 */
    private String visitorName;

    /** 手机号（脱敏）。 */
    private String phone;

    /** 来访单位。 */
    private String company;

    /** 被访人 ID。 */
    private Long hostUserId;

    /** 来访日期。 */
    private LocalDate visitDate;

    /** 来访事由。 */
    private String purpose;

    /** 状态。 */
    private VisitorStatus status;

    /** 进入时间。 */
    private LocalDateTime checkinTime;

    /** 离开时间。 */
    private LocalDateTime checkoutTime;
}
