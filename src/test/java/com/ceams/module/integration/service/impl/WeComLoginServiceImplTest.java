package com.ceams.module.integration.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.enums.UserStatus;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.integration.client.WeComApiClient;
import com.ceams.module.integration.client.WeComUser;
import com.ceams.module.integration.config.WeComAppProperties;
import com.ceams.module.integration.service.WeComLoginResult;
import com.ceams.module.integration.support.WeComTokenCache;
import com.ceams.module.integration.vo.WeComQrCodeVO;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/** 企业微信扫码登录服务单元测试：state 生命周期、成员匹配与手机号兜底绑定。 */
@ExtendWith(MockitoExtension.class)
class WeComLoginServiceImplTest {

    private static final String STATE_KEY_PREFIX = "wecom:login:state:";

    @Mock private WeComTokenCache tokenCache;

    @Mock private WeComApiClient weComApiClient;

    @Mock private UserMapper userMapper;

    @Mock private StringRedisTemplate stringRedisTemplate;

    @Mock private ValueOperations<String, String> valueOperations;

    private WeComAppProperties properties;

    private WeComLoginServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new WeComAppProperties();
        properties.setEnabled(true);
        properties.setCorpId("corp-001");
        properties.setAgentId("100001");
        properties.setSecret("secret-001");
        properties.setLoginBaseUrl("https://login.work.weixin.qq.com");
        properties.setRedirectUri("http://localhost:5173/login/wecom");
        service =
                new WeComLoginServiceImpl(
                        properties, tokenCache, weComApiClient, userMapper, stringRedisTemplate);
    }

    @Test
    void 生成二维码_拼装登录地址并写入五分钟state() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        WeComQrCodeVO vo = service.qrCode("dev-1");

        assertThat(vo.getState()).matches("[0-9a-f]{32}");
        assertThat(vo.getLoginUrl())
                .startsWith(
                        "https://login.work.weixin.qq.com/wwlogin/sso/login?login_type=CorpApp")
                .contains("appid=corp-001")
                .contains("agentid=100001")
                .contains("redirect_uri=http%3A%2F%2Flocalhost%3A5173%2Flogin%2Fwecom")
                .contains("state=" + vo.getState());
        verify(valueOperations)
                .set(
                        eq(STATE_KEY_PREFIX + vo.getState()),
                        eq("dev-1"),
                        eq(Duration.ofMinutes(5)));
    }

    @Test
    void 集成未启用_返回503() {
        properties.setEnabled(false);

        assertThatThrownBy(() -> service.qrCode(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("企业微信登录未启用")
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.SYSTEM_SERVICE_UNAVAILABLE);
        verifyNoInteractions(stringRedisTemplate, tokenCache, weComApiClient, userMapper);
    }

    @Test
    void state不存在_返回400且不调用企微() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(STATE_KEY_PREFIX + "missing")).thenReturn(null);

        assertThatThrownBy(() -> service.resolveUser("code-1", "missing"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("二维码已过期")
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALID_ILLEGAL_ARGUMENT);
        verify(tokenCache, never()).getToken();
        verify(userMapper, never()).selectList(any());
    }

    @Test
    void 已绑定成员_账号禁用_返回403() {
        mockState("st1", "dev-1");
        when(tokenCache.getToken()).thenReturn("tk");
        when(weComApiClient.getUserIdByCode("tk", "code-1")).thenReturn("wx1");
        when(userMapper.selectList(any()))
                .thenReturn(List.of(user(1001L, "zhangsan", "wx1", UserStatus.DISABLED, "13800000000")));

        assertThatThrownBy(() -> service.resolveUser("code-1", "st1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号已禁用")
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
        // state 一次性消费
        verify(stringRedisTemplate).delete(STATE_KEY_PREFIX + "st1");
        verify(userMapper, never()).updateById(any(SysUser.class));
    }

    @Test
    void 未绑定_手机号唯一命中_回填wecomUserid并返回该用户() {
        mockState("st2", "dev-2");
        when(tokenCache.getToken()).thenReturn("tk");
        when(weComApiClient.getUserIdByCode("tk", "code-2")).thenReturn("wx2");
        SysUser candidate = user(2002L, "lisi", null, UserStatus.ACTIVE, "13800000000");
        candidate.setName(null);
        candidate.setEmail(null);
        when(userMapper.selectList(any())).thenReturn(List.of(), List.of(candidate));
        when(weComApiClient.getUser("tk", "wx2"))
                .thenReturn(
                        new WeComUser(
                                "wx2", "李四", "138 0000 0000", "lisi@x.com", 1, List.of(10L)));

        WeComLoginResult result = service.resolveUser("code-2", "st2");

        assertThat(result.user()).isSameAs(candidate);
        assertThat(result.user().getWecomUserid()).isEqualTo("wx2");
        assertThat(result.deviceId()).isEqualTo("dev-2");
        assertThat(candidate.getName()).isEqualTo("李四");
        assertThat(candidate.getEmail()).isEqualTo("lisi@x.com");
        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).updateById(captor.capture());
        assertThat(captor.getValue().getWecomUserid()).isEqualTo("wx2");
    }

    @Test
    void 未绑定_手机号多命中_返回400且不绑定() {
        mockState("st3", null);
        when(tokenCache.getToken()).thenReturn("tk");
        when(weComApiClient.getUserIdByCode("tk", "code-3")).thenReturn("wx3");
        when(userMapper.selectList(any()))
                .thenReturn(
                        List.of(),
                        List.of(
                                user(3001L, "a", null, UserStatus.ACTIVE, "13800000000"),
                                user(3002L, "b", null, UserStatus.ACTIVE, "13800000000")));
        when(weComApiClient.getUser("tk", "wx3"))
                .thenReturn(new WeComUser("wx3", "王五", "13800000000", null, 1, List.of(10L)));

        assertThatThrownBy(() -> service.resolveUser("code-3", "st3"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号未绑定")
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALID_ILLEGAL_ARGUMENT);
        verify(userMapper, never()).updateById(any(SysUser.class));
    }

    @Test
    void 未绑定_手机号零命中_返回400() {
        mockState("st4", "dev-4");
        when(tokenCache.getToken()).thenReturn("tk");
        when(weComApiClient.getUserIdByCode("tk", "code-4")).thenReturn("wx4");
        when(userMapper.selectList(any())).thenReturn(List.of());
        when(weComApiClient.getUser("tk", "wx4"))
                .thenReturn(new WeComUser("wx4", "赵六", "13900000000", null, 1, List.of(10L)));

        assertThatThrownBy(() -> service.resolveUser("code-4", "st4"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号未绑定")
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALID_ILLEGAL_ARGUMENT);
        verify(userMapper, never()).updateById(any(SysUser.class));
    }

    // ------------------------------------------------------------------
    // 测试辅助
    // ------------------------------------------------------------------

    private void mockState(String state, String deviceId) {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        // 与生产一致：deviceId 为空时 Redis 存空串（不是 key 缺失）
        when(valueOperations.get(STATE_KEY_PREFIX + state))
                .thenReturn(deviceId == null ? "" : deviceId);
    }

    private SysUser user(
            Long id, String username, String wecomUserid, UserStatus status, String phone) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setWecomUserid(wecomUserid);
        user.setStatus(status);
        user.setPhone(phone);
        return user;
    }
}
