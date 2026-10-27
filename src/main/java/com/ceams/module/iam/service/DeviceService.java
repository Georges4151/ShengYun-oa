package com.ceams.module.iam.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.iam.dto.device.DeviceBindRequest;
import com.ceams.module.iam.vo.IdResponse;
import com.ceams.module.iam.vo.UserDeviceVO;

/**
 * 设备绑定服务。
 */
public interface DeviceService {

    PageResult<UserDeviceVO> page(long page, long size);

    IdResponse bind(DeviceBindRequest request);

    void unbind(Long id);
}