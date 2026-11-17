package com.ceams.module.admin.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 访客二维码内容展示对象。 */
@Data
public class VisitorQrVO {

    /** 访客登记 ID。 */
    private Long visitorId;

    /** 一次性核销令牌。 */
    private String token;

    /** 令牌过期时间。 */
    private LocalDateTime expireAt;

    /** 来访日期。 */
    private LocalDate visitDate;

    /** 访客姓名。 */
    private String visitorName;
}
