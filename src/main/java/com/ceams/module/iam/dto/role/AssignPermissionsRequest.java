package com.ceams.module.iam.dto.role;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/** 为角色赋权请求。 */
@Data
public class AssignPermissionsRequest {

    @NotNull(message = "permissionIds 不能为空")
    private List<Long> permissionIds;
}
