package com.ceams.module.iam.converter;

import com.ceams.module.iam.dto.permission.PermissionCreateRequest;
import com.ceams.module.iam.dto.permission.PermissionUpdateRequest;
import com.ceams.module.iam.entity.SysPermission;
import com.ceams.module.iam.vo.PermissionVO;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/** 权限实体 <-> VO/DTO 转换。 */
@Mapper(componentModel = "spring")
public interface PermissionConverter {

    @Mapping(target = "sort", source = "sortOrder")
    @Mapping(target = "children", ignore = true)
    PermissionVO toVO(SysPermission permission);

    List<PermissionVO> toVOList(List<SysPermission> permissions);

    /** 请求 -> 实体；审计字段由 MyBatis-Plus 自动填充。 */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    SysPermission toEntity(PermissionCreateRequest request);

    /** 请求 -> 实体增量更新（null 字段保持原值）。 */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntity(PermissionUpdateRequest request, @MappingTarget SysPermission permission);
}
