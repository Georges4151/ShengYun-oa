package com.ceams.module.iam.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新建角色请求。 */
@Data
public class RoleCreateRequest {

    @NotBlank(message = "角色编码不能为空")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,50}$", message = "角色编码须为大写字母开头，可含大写字母、数字、下划线，长度 2-50")
    private String code;

    @NotBlank(message = "角色名不能为空")
    @Size(max = 100, message = "角色名长度不能超过 100")
    private String name;

    private String description;
}
