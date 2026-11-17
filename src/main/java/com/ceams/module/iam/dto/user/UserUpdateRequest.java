package com.ceams.module.iam.dto.user;

import com.ceams.module.iam.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 更新用户请求。 */
@Data
public class UserUpdateRequest {

    @Size(max = 100, message = "姓名长度不能超过 100")
    private String name;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;

    private UserStatus status;

    /** 所属部门 ID（不传则保持不变）。 */
    private Long deptId;

    /** 直属上级用户 ID（不传则保持不变）。 */
    private Long leaderId;
}
