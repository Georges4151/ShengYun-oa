package com.ceams.module.iam.dto.role;

import com.ceams.module.iam.enums.RoleStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新角色请求。
 */
@Data
public class RoleUpdateRequest {

    @Size(max = 100, message = "角色名长度不能超过 100")
    private String name;

    private String description;

    private RoleStatus status;
}