package com.ceams.module.iam.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.common.crypto.SensitiveTypeHandler;
import com.ceams.module.iam.enums.UserStatus;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 系统用户（sys_user）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_user", autoResultMap = true)
public class SysUser extends BaseEntity {

    /** 登录账号。 */
    private String username;

    /** 密码（BCrypt）。 */
    private String passwordHash;

    /** 真实姓名。 */
    private String name;

    /** 邮箱。 */
    private String email;

    /** 手机号（库内加密，接口脱敏）。 */
    @TableField(typeHandler = SensitiveTypeHandler.class)
    private String phone;

    /** 状态。 */
    private UserStatus status;

    /** 最后登录时间。 */
    private LocalDateTime lastLoginTime;

    /** 最后登录 IP。 */
    private String lastLoginIp;

    /** 所属部门 ID（逻辑引用 sys_dept.id，可空）。 */
    private Long deptId;

    /** 直属上级用户 ID（逻辑引用 sys_user.id，可空）。 */
    private Long leaderId;

    /** 企业微信成员账号（唯一；手工本地账号为空，由组织同步回填）。 */
    private String wecomUserid;
}
