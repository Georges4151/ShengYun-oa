package com.ceams.module.iam.vo;

import com.ceams.module.iam.enums.RoleDataScope;
import com.ceams.module.iam.enums.RoleStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import lombok.Data;

/** 角色视图对象。 */
@Data
public class RoleVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String code;

    private String name;

    private String description;

    private RoleStatus status;

    /** 数据范围：ALL/DEPT/SELF。 */
    private RoleDataScope dataScope;

    private LocalDateTime createTime;
}
