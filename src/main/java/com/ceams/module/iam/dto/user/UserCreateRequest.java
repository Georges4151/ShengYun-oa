package com.ceams.module.iam.dto.user;

import com.ceams.module.iam.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/** 新建用户请求。 */
@Data
public class UserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,50}$", message = "用户名只能包含字母、数字和下划线，长度 3-50")
    private String username;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 100, message = "姓名长度不能超过 100")
    private String name;

    @Size(min = 6, max = 128, message = "密码长度须为 6-128")
    private String password;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;

    private UserStatus status;

    /** 初始角色 ID 集合。 */
    private List<Long> roleIds;

    /** 所属部门 ID（可空）。 */
    private Long deptId;
}
