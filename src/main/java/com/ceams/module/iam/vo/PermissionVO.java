package com.ceams.module.iam.vo;

import com.ceams.module.iam.enums.PermissionType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 权限视图对象（树节点）。 */
@Data
public class PermissionVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String code;

    private String name;

    private PermissionType type;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    private String path;

    private String icon;

    private Integer sort;

    private List<PermissionVO> children = new ArrayList<>();
}
