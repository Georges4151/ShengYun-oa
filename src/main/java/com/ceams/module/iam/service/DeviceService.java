package com.ceams.module.iam.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.iam.dto.device.DeviceBindRequest;
import com.ceams.module.iam.vo.UserDeviceVO;

/**
 * 设备绑定服务。
 */
public interface DeviceService {

    PageResult<UserDeviceVO> page(long page, long size);

    String bind(DeviceBindRequest request);

    void unbind(Long id);
}