package com.ceams.module.iam.vo;

import com.ceams.module.iam.enums.UserStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户视图对象（敏感字段脱敏，密码绝不外泄）。
 */
@Data
public class UserVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String username;

    private String name;

    private String email;

    private String phone;

    private UserStatus status;

    private List<RoleVO> roles;

    private LocalDateTime createTime;
}