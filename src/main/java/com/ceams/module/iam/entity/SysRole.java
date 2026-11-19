package com.ceams.module.iam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.iam.enums.RoleDataScope;
import com.ceams.module.iam.enums.RoleStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 角色（sys_role）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    /** 角色编码（如 SYS_ADMIN）。 */
    private String code;

    /** 角色名。 */
    private String name;

    /** 描述。 */
    private String description;

    /** 状态。 */
    private RoleStatus status;

    /** 数据范围：ALL/DEPT/SELF，默认 SELF。 */
    private RoleDataScope dataScope;
}
