package com.ceams.module.iam.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.common.security.JwtUtil;
import com.ceams.module.iam.converter.UserConverter;
import com.ceams.module.iam.entity.OperationLog;
import com.ceams.module.iam.entity.SysRefreshToken;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.entity.SysUserDevice;
import com.ceams.module.iam.enums.OperationType;
import com.ceams.module.iam.enums.Platform;
import com.ceams.module.iam.enums.UserStatus;
import com.ceams.module.iam.mapper.OperationLogMapper;
import com.ceams.module.iam.mapper.RefreshTokenMapper;
import com.ceams.module.iam.mapper.RoleMapper;
import com.ceams.module.iam.mapper.UserDeviceMapper;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.iam.mapper.UserRoleMapper;
import com.ceams.module.iam.service.UserService;
import com.ceams.module.iam.vo.LoginResponse;
import com.ceams.module.iam.vo.UserVO;
import com.ceams.module.integration.service.WeComLoginResult;
import com.ceams.module.integration.service.WeComLoginService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/** {@link AuthServiceImpl#loginByWeCom} 扫码登录链路单元测试。 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplWeComLoginTest {

    @Mock private UserMapper userMapper;
    @Mock private UserRoleMapper userRoleMapper;
    @Mock private RoleMapper roleMapper;
    @Mock private RefreshTokenMapper refreshTokenMapper;
    @Mock private UserDeviceMapper userDeviceMapper;
    @Mock private OperationLogMapper operationLogMapper;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserConverter userConverter;
    @Mock private UserService userService;
    @Mock private WeComLoginService weComLoginService;

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new AuthServiceImpl(
                        userMapper,
                        userRoleMapper,
                        roleMapper,
                        refreshTokenMapper,
                        userDeviceMapper,
                        operationLogMapper,
                        jwtUtil,
                        passwordEncoder,
                        userConverter,
                        userService,
                        weComLoginService);
        ReflectionTestUtils.setField(service, "accessTtlSeconds", 900L);
        ReflectionTestUtils.setField(service, "refreshTtlSeconds", 604800L);
    }

    @Test
    void 扫码登录_复用令牌签发与设备绑定() {
        SysUser user = new SysUser();
        user.setId(1001L);
        user.setUsername("zhangsan");
        user.setStatus(UserStatus.ACTIVE);
        when(weComLoginService.resolveUser("code-1", "st-1"))
                .thenReturn(new WeComLoginResult(user, "dev-1"));
        when(jwtUtil.generateAccessToken(eq(1001L), eq("zhangsan"), anyList()))
                .thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(eq(1001L), anyString())).thenReturn("refresh-token");
        when(userConverter.toVO(user)).thenReturn(new UserVO());
        when(userService.listRolesByUser(1001L)).thenReturn(List.of());
        when(refreshTokenMapper.insert(any(SysRefreshToken.class))).thenReturn(1);
        when(userDeviceMapper.selectOne(any())).thenReturn(null);
        when(userDeviceMapper.insert(any(SysUserDevice.class))).thenReturn(1);

        LoginResponse response = service.loginByWeCom("code-1", "st-1", null, null, null);

        assertThat(response.getTokenPair().getAccessToken()).isEqualTo("access-token");
        assertThat(response.getTokenPair().getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getTokenPair().getExpiresIn()).isEqualTo(900L);
        assertThat(response.getUser()).isNotNull();

        ArgumentCaptor<SysUserDevice> deviceCaptor = ArgumentCaptor.forClass(SysUserDevice.class);
        verify(userDeviceMapper).insert(deviceCaptor.capture());
        SysUserDevice device = deviceCaptor.getValue();
        assertThat(device.getDeviceId()).isEqualTo("dev-1");
        assertThat(device.getDeviceName()).isEqualTo("企业微信扫码");
        assertThat(device.getPlatform()).isEqualTo(Platform.WEB);

        ArgumentCaptor<SysRefreshToken> tokenCaptor =
                ArgumentCaptor.forClass(SysRefreshToken.class);
        verify(refreshTokenMapper).insert(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getDeviceId()).isEqualTo("dev-1");

        ArgumentCaptor<OperationLog> logCaptor = ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getOperationType()).isEqualTo(OperationType.LOGIN);
        assertThat(logCaptor.getValue().getOperationDescription()).isEqualTo("企业微信扫码登录");
        verify(userMapper).updateById(user);
    }
}
