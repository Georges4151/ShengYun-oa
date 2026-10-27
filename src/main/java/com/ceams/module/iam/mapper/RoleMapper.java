package com.ceams.module.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.iam.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色 Mapper。
 */
@Mapper
public interface RoleMapper extends BaseMapper<SysRole> {
}