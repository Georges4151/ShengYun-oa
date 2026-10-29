package com.ceams.module.iam.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.sign.SignSecretResolver;
import com.ceams.module.iam.entity.SysUserDevice;
import com.ceams.module.iam.mapper.UserDeviceMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 设备 AppSecret 解析器：按请求头 {@code X-Device-Id} 解析设备签名密钥。
 *
 * <p>实现 {@link SignSecretResolver}，落地「签名请求必须带设备标识并按设备 AppSecret 校验」， 不再接受全局共享密钥。查询走 MyBatis-Plus
 * LambdaWrapper；{@code app_secret} 为库内加密字段， 实体已开启 {@code autoResultMap}，读取时经 {@code
 * SensitiveTypeHandler} 自动解密为明文。
 */
@Component
@RequiredArgsConstructor
public class DeviceAppSecretResolver implements SignSecretResolver {

    /** 设备标识请求头。 */
    public static final String HEADER_DEVICE_ID = "X-Device-Id";

    private final UserDeviceMapper userDeviceMapper;

    @Override
    public String resolve(HttpServletRequest request) {
        String deviceId = request.getHeader(HEADER_DEVICE_ID);
        if (StringUtils.isBlank(deviceId)) {
            throw BusinessException.of(ErrorCode.SIGN_INVALID, "缺少设备标识 X-Device-Id");
        }
        SysUserDevice device =
                userDeviceMapper.selectOne(
                        new LambdaQueryWrapper<SysUserDevice>()
                                .eq(SysUserDevice::getDeviceId, deviceId)
                                .last("LIMIT 1"));
        if (device == null || StringUtils.isBlank(device.getAppSecret())) {
            throw BusinessException.of(ErrorCode.SIGN_INVALID, "设备未绑定或签名密钥缺失");
        }
        return device.getAppSecret();
    }
}
