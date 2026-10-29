package com.ceams.module.iam.converter;

import com.ceams.module.iam.entity.SysPermission;
import com.ceams.module.iam.vo.PermissionVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** 权限实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface PermissionConverter {

    @Mapping(target = "sort", source = "sortOrder")
    @Mapping(target = "children", ignore = true)
    PermissionVO toVO(SysPermission permission);

    List<PermissionVO> toVOList(List<SysPermission> permissions);
}
