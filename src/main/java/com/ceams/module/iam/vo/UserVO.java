package com.ceams.module.iam.vo;

import com.ceams.module.iam.enums.UserStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 用户视图对象（敏感字段脱敏，密码绝不外泄）。 */
@Data
public class UserVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String username;

    private String name;

    /** 头像访问地址（/api/v1/profile/avatars/{文件名}，未设置为空）。 */
    private String avatar;

    private String email;

    private String phone;

    private UserStatus status;

    private List<RoleVO> roles;

    /** 所属部门 ID（避免前端 JS 精度丢失，序列化为字符串）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long deptId;

    /** 直属上级用户 ID（避免前端 JS 精度丢失，序列化为字符串）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long leaderId;

    private LocalDateTime createTime;
}
