package com.ceams.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import lombok.Data;

/** 访客登记请求。 */
@Data
public class VisitorCreateRequest {

    /** 访客姓名。 */
    @NotBlank(message = "访客姓名不能为空")
    private String visitorName;

    /** 手机号（中国大陆 11 位）。 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 来访单位。 */
    private String company;

    /** 被访人 ID，为空时默认当前登录用户。 */
    private Long hostUserId;

    /** 来访日期。 */
    @NotNull(message = "来访日期不能为空")
    private LocalDate visitDate;

    /** 来访事由。 */
    private String purpose;
}
