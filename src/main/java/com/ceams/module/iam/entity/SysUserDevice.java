package com.ceams.module.iam.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.common.crypto.SensitiveTypeHandler;
import com.ceams.module.iam.enums.DeviceStatus;
import com.ceams.module.iam.enums.Platform;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户设备绑定（sys_user_device）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_user_device", autoResultMap = true)
public class SysUserDevice extends BaseEntity {

    /** 用户 ID。 */
    private Long userId;

    /** 设备指纹。 */
    private String deviceId;

    /** 设备名。 */
    private String deviceName;

    /** 平台。 */
    private Platform platform;

    /** 最后登录 IP。 */
    private String lastLoginIp;

    /** 状态。 */
    private DeviceStatus status;

    /** 绑定时间。 */
    private LocalDateTime bindTime;

    /** 最后登录时间。 */
    private LocalDateTime lastLoginTime;

    /** 设备签名密钥（AppSecret，库内加密）。 */
    @TableField(typeHandler = SensitiveTypeHandler.class)
    private String appSecret;
}