package com.ceams.module.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.iam.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/** 用户 Mapper。 */
@Mapper
public interface UserMapper extends BaseMapper<SysUser> {}
