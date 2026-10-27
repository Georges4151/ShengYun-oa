package com.ceams.module.iam.converter;

import com.ceams.module.iam.entity.SysRole;
import com.ceams.module.iam.vo.RoleVO;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 角色实体 <-> VO 转换。
 */
@Mapper(componentModel = "spring")
public interface RoleConverter {

    RoleVO toVO(SysRole role);

    List<RoleVO> toVOList(List<SysRole> roles);
}