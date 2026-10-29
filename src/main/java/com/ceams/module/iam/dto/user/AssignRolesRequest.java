package com.ceams.module.iam.dto.user;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/** 为用户分配角色请求。 */
@Data
public class AssignRolesRequest {

    @NotNull(message = "roleIds 不能为空")
    private List<Long> roleIds;
}
