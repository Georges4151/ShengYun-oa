package com.ceams.module.iam.converter;

import com.ceams.module.iam.entity.SysUserDevice;
import com.ceams.module.iam.vo.UserDeviceVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 设备实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface DeviceConverter {

    UserDeviceVO toVO(SysUserDevice device);

    List<UserDeviceVO> toVOList(List<SysUserDevice> devices);
}
