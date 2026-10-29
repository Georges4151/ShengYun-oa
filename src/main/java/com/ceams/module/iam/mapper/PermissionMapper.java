package com.ceams.module.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.iam.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;

/** 权限 Mapper。 */
@Mapper
public interface PermissionMapper extends BaseMapper<SysPermission> {}
