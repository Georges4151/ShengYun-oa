package com.ceams.module.integration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.enums.UserStatus;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.integration.client.WeComApiClient;
import com.ceams.module.integration.client.WeComApiException;
import com.ceams.module.integration.client.WeComUser;
import com.ceams.module.integration.config.WeComAppProperties;
import com.ceams.module.integration.service.WeComLoginResult;
import com.ceams.module.integration.service.WeComLoginService;
import com.ceams.module.integration.support.WeComTokenCache;
import com.ceams.module.integration.vo.WeComQrCodeVO;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 企业微信扫码登录服务实现。
 *
 * <p>口径：
 *
 * <ul>
 *   <li>state 为 32 位十六进制随机串，Redis {@code wecom:login:state:{state}} 存 deviceId，TTL 5 分钟；
 *   <li>state 在 {@link #resolveUser} 中「读到即删」，无论后续授权成功与否都不可复用；
 *   <li>成员匹配顺序：本地 {@code wecom_userid} 精确命中 → 企业微信手机号唯一命中未绑定账号并回填；
 *   <li>手机号命中 0 条或多条均视为未绑定（不猜测），提示人工处理。
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeComLoginServiceImpl implements WeComLoginService {

    /** 扫码 state 缓存 key 前缀。 */
    static final String STATE_KEY_PREFIX = "wecom:login:state:";

    /** state 有效期。 */
    private static final Duration STATE_TTL = Duration.ofMinutes(5);

    /** state 随机字节数（16 字节 → 32 位十六进制）。 */
    private static final int STATE_BYTES = 16;

    /** 企微登录默认基地址。 */
    private static final String DEFAULT_LOGIN_BASE_URL = "https://login.work.weixin.qq.com";

    /** 随机源（线程安全）。 */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final WeComAppProperties properties;
    private final WeComTokenCache tokenCache;
    private final WeComApiClient weComApiClient;
    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public WeComQrCodeVO qrCode(String deviceId) {
        requireConfigured();
        String state = newState();
        stringRedisTemplate
                .opsForValue()
                .set(STATE_KEY_PREFIX + state, deviceId == null ? "" : deviceId.trim(), STATE_TTL);
        WeComQrCodeVO vo = new WeComQrCodeVO();
        vo.setState(state);
        vo.setLoginUrl(buildLoginUrl(state));
        return vo;
    }

    @Override
    public WeComLoginResult resolveUser(String code, String state) {
        requireConfigured();
        if (!StringUtils.hasText(state)) {
            throw expiredState();
        }
        String stateKey = STATE_KEY_PREFIX + state.trim();
        String deviceId = stringRedisTemplate.opsForValue().get(stateKey);
        if (deviceId == null) {
            throw expiredState();
        }
        // 一次性消费：无论后续授权成功与否都不允许复用
        stringRedisTemplate.delete(stateKey);
        if (!StringUtils.hasText(code)) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "企业微信授权码不能为空");
        }
        try {
            String token = tokenCache.getToken();
            String wecomUserid = weComApiClient.getUserIdByCode(token, code.trim());
            SysUser user = findBoundUser(wecomUserid);
            if (user == null) {
                user = bindByMobile(token, wecomUserid);
            }
            if (!UserStatus.ACTIVE.equals(user.getStatus())) {
                throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "账号已禁用");
            }
            return new WeComLoginResult(user, StringUtils.hasText(deviceId) ? deviceId : null);
        } catch (WeComApiException e) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, toAuthFailureMessage(e));
        }
    }

    // ------------------------------------------------------------------
    // 内部流程
    // ------------------------------------------------------------------

    private void requireConfigured() {
        if (!properties.isConfigured()) {
            throw BusinessException.of(ErrorCode.SYSTEM_SERVICE_UNAVAILABLE, "企业微信登录未启用");
        }
    }

    private BusinessException expiredState() {
        return BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "二维码已过期，请重新扫码");
    }

    /** 按本地已绑定的 wecom_userid 精确匹配（逻辑删除由 @TableLogic 自动排除）。 */
    private SysUser findBoundUser(String wecomUserid) {
        List<SysUser> users =
                userMapper.selectList(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(SysUser::getWecomUserid, wecomUserid)
                                .last("LIMIT 1"));
        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * 手机号兜底绑定：仅匹配 {@code wecom_userid IS NULL} 的账号，唯一命中才回填。
     *
     * <p>回填必须使用实体 {@code updateById}，保证手机号等敏感列走 {@code SensitiveTypeHandler}。
     */
    private SysUser bindByMobile(String token, String wecomUserid) {
        WeComUser detail = weComApiClient.getUser(token, wecomUserid);
        String mobile = normalizeMobile(detail == null ? null : detail.mobile());
        if (!StringUtils.hasText(mobile)) {
            throw notBound();
        }
        List<SysUser> candidates =
                userMapper.selectList(
                        new LambdaQueryWrapper<SysUser>().isNull(SysUser::getWecomUserid));
        List<SysUser> matched = new ArrayList<>();
        for (SysUser candidate : candidates) {
            if (mobile.equals(normalizeMobile(candidate.getPhone()))) {
                matched.add(candidate);
            }
        }
        if (matched.size() != 1) {
            if (matched.size() > 1) {
                log.warn(
                        "企业微信扫码登录手机号命中多个未绑定账号，跳过自动绑定：wecomUserid={}，命中数={}",
                        wecomUserid,
                        matched.size());
            }
            throw notBound();
        }
        SysUser user = matched.get(0);
        user.setWecomUserid(wecomUserid);
        if (!StringUtils.hasText(user.getName()) && detail != null) {
            user.setName(detail.name());
        }
        if (!StringUtils.hasText(user.getEmail()) && detail != null) {
            user.setEmail(detail.email());
        }
        userMapper.updateById(user);
        log.info(
                "企业微信扫码登录按手机号回填绑定：userId={}，wecomUserid={}",
                user.getId(),
                wecomUserid);
        return user;
    }

    private BusinessException notBound() {
        return BusinessException.of(
                ErrorCode.VALID_ILLEGAL_ARGUMENT, "账号未绑定，请联系管理员绑定企业微信");
    }

    /** 企微异常统一转换为 400 中文提示，避免前端暴露 errcode 细节。 */
    private String toAuthFailureMessage(WeComApiException e) {
        String errmsg = StringUtils.hasText(e.getErrmsg()) ? e.getErrmsg().trim() : "未知错误";
        return errmsg.startsWith("企业微信授权失败") ? errmsg : "企业微信授权失败：" + errmsg;
    }

    private String normalizeMobile(String mobile) {
        return mobile == null ? null : mobile.replaceAll("\\s+", "");
    }

    private String newState() {
        byte[] bytes = new byte[STATE_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String buildLoginUrl(String state) {
        String redirectUri =
                properties.getRedirectUri() == null ? "" : properties.getRedirectUri().trim();
        return loginBaseUrl()
                + "/wwlogin/sso/login?login_type=CorpApp&appid="
                + encode(properties.getCorpId())
                + "&agentid="
                + encode(properties.getAgentId())
                + "&redirect_uri="
                + encode(redirectUri)
                + "&state="
                + encode(state);
    }

    private String loginBaseUrl() {
        String base = properties.getLoginBaseUrl();
        if (!StringUtils.hasText(base)) {
            return DEFAULT_LOGIN_BASE_URL;
        }
        String trimmed = base.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
