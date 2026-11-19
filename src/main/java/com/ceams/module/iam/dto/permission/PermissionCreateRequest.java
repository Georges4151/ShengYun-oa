package com.ceams.module.iam.dto.permission;

import com.ceams.module.iam.enums.PermissionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新建权限请求。 */
@Data
public class PermissionCreateRequest {

    @NotBlank(message = "权限编码不能为空")
    @Size(max = 100, message = "权限编码长度不能超过 100")
    @Pattern(regexp = "^[A-Za-z0-9:_-]{2,100}$", message = "权限编码只能包含字母、数字、冒号、下划线和连字符，长度 2-100")
    private String code;

    @NotBlank(message = "权限名不能为空")
    @Size(max = 100, message = "权限名长度不能超过 100")
    private String name;

    @NotNull(message = "权限类型不能为空")
    private PermissionType type;

    /** 父权限 ID，顶层为 null。 */
    private Long parentId;

    /** 前端路由（MENU）。 */
    @Size(max = 255, message = "路由长度不能超过 255")
    private String path;

    @Size(max = 100, message = "图标长度不能超过 100")
    private String icon;

    /** 排序，默认 0。 */
    private Integer sortOrder;
}
