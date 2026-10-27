package com.ceams.module.iam.dto.role;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 为角色赋权请求。
 */
@Data
public class AssignPermissionsRequest {

    @NotNull(message = "permissionIds 不能为空")
    private List<Long> permissionIds;
}