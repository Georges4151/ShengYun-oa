package com.ceams.module.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.iam.entity.SysUserDevice;
import org.apache.ibatis.annotations.Mapper;

/** 用户设备绑定 Mapper。 */
@Mapper
public interface UserDeviceMapper extends BaseMapper<SysUserDevice> {}
