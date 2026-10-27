package com.ceams.module.iam.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 为用户分配角色请求。
 */
@Data
public class AssignRolesRequest {

    @NotNull(message = "roleIds 不能为空")
    private List<Long> roleIds;
}