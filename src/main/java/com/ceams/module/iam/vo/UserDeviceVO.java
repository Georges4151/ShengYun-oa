package com.ceams.module.iam.vo;

import com.ceams.module.iam.enums.DeviceStatus;
import com.ceams.module.iam.enums.Platform;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import lombok.Data;

/** 用户设备绑定视图对象。 */
@Data
public class UserDeviceVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String deviceId;

    private String deviceName;

    private Platform platform;

    private String lastLoginIp;

    private DeviceStatus status;

    private LocalDateTime bindTime;

    private LocalDateTime lastLoginTime;
}
