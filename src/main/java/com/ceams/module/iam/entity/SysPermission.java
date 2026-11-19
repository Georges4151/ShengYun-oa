package com.ceams.module.iam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.iam.enums.PermissionType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 权限（菜单/按钮/API，sys_permission）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class SysPermission extends BaseEntity {

    /** 权限标识（如 project:create）。 */
    private String code;

    /** 权限名。 */
    private String name;

    /** 类型。 */
    private PermissionType type;

    /** 父权限 ID（逻辑引用）。 */
    private Long parentId;

    /** 前端路由（MENU）。 */
    private String path;

    /** 图标。 */
    private String icon;

    /** 排序。 */
    private Integer sortOrder;
}
