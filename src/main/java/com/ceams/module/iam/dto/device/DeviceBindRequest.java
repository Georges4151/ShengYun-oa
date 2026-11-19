package com.ceams.module.iam.dto.device;

import com.ceams.module.iam.enums.Platform;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 设备绑定请求。 */
@Data
public class DeviceBindRequest {

    @NotBlank(message = "deviceId 不能为空")
    private String deviceId;

    private String deviceName;

    private Platform platform;
}
