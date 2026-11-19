package com.ceams.module.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.iam.entity.RolePermission;
import org.apache.ibatis.annotations.Mapper;

/** 角色-权限关联 Mapper（物理删除）。 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {}
