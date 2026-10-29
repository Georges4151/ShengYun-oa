package com.ceams.module.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.common.security.JwtUtil;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.iam.converter.UserConverter;
import com.ceams.module.iam.dto.auth.LoginRequest;
import com.ceams.module.iam.dto.auth.RefreshRequest;
import com.ceams.module.iam.entity.OperationLog;
import com.ceams.module.iam.entity.SysRefreshToken;
import com.ceams.module.iam.entity.SysRole;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.entity.SysUserDevice;
import com.ceams.module.iam.entity.UserRole;
import com.ceams.module.iam.enums.DeviceStatus;
import com.ceams.module.iam.enums.OperationType;
import com.ceams.module.iam.enums.UserStatus;
import com.ceams.module.iam.mapper.OperationLogMapper;
import com.ceams.module.iam.mapper.RefreshTokenMapper;
import com.ceams.module.iam.mapper.RoleMapper;
import com.ceams.module.iam.mapper.UserDeviceMapper;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.iam.mapper.UserRoleMapper;
import com.ceams.module.iam.service.AuthService;
import com.ceams.module.iam.service.UserService;
import com.ceams.module.iam.util.PageUtil;
import com.ceams.module.iam.vo.LoginResponse;
import com.ceams.module.iam.vo.RoleVO;
import com.ceams.module.iam.vo.SignSecretVO;
import com.ceams.module.iam.vo.TokenPair;
import com.ceams.module.iam.vo.UserSessionVO;
import com.ceams.module.iam.vo.UserVO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 认证服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String SYSTEM_ADMIN = "SYS_ADMIN";

    /** AppSecret 生成用安全随机源（线程安全）。 */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final UserDeviceMapper userDeviceMapper;
    private final OperationLogMapper operationLogMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final UserConverter userConverter;
    private final UserService userService;

    @Value("${ceams.jwt.access-ttl-seconds:900}")
    private long accessTtlSeconds;

    @Value("${ceams.jwt.refresh-ttl-seconds:604800}")
    private long refreshTtlSeconds;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        SysUser user =
                userMapper.selectOne(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(SysUser::getUsername, request.getUsername()));
        if (user == null
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw BusinessException.of(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "账号当前不可用");
        }

        LocalDateTime now = LocalDateTime.now();
        // 更新登录痕迹
        user.setLastLoginTime(now);
        user.setLastLoginIp(resolveIp(httpRequest));
        userMapper.updateById(user);

        // 设备绑定
        if (StringUtils.isNotBlank(request.getDeviceId())) {
            bindDevice(
                    user.getId(),
                    request.getDeviceId(),
                    request.getDeviceName(),
                    request.getPlatform(),
                    user.getLastLoginIp(),
                    now);
        }

        List<RoleVO> roles = userService.listRolesByUser(user.getId());
        LoginResponse response = issueTokens(user, roles, request.getDeviceId(), newJti());
        logOperation(
                user.getId(),
                user.getUsername(),
                OperationType.LOGIN,
                "用户登录",
                null,
                null,
                httpRequest);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse refresh(RefreshRequest request) {
        Claims claims;
        try {
            claims = jwtUtil.parse(request.getRefreshToken());
        } catch (ExpiredJwtException ex) {
            throw BusinessException.of(ErrorCode.AUTH_TOKEN_EXPIRED, "refresh token 已过期");
        } catch (JwtException | IllegalArgumentException ex) {
            throw BusinessException.of(ErrorCode.AUTH_TOKEN_INVALID, "refresh token 无效");
        }
        if (!jwtUtil.isRefreshToken(claims)) {
            throw BusinessException.of(ErrorCode.AUTH_TOKEN_INVALID, "非 refresh token");
        }
        String jti = claims.getId();
        Long userId = jwtUtil.getUserId(claims);

        SysRefreshToken existing =
                refreshTokenMapper.selectOne(
                        new LambdaQueryWrapper<SysRefreshToken>().eq(SysRefreshToken::getJti, jti));
        if (existing == null || existing.getRevoked() != null && existing.getRevoked() == 1) {
            throw BusinessException.of(ErrorCode.AUTH_REFRESH_REVOKED);
        }
        if (!sha256Hex(request.getRefreshToken()).equals(existing.getTokenHash())) {
            throw BusinessException.of(ErrorCode.AUTH_TOKEN_INVALID, "refresh token 哈希不匹配");
        }
        if (existing.getExpiresAt() != null
                && existing.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw BusinessException.of(ErrorCode.AUTH_REFRESH_REVOKED, "refresh token 已过期");
        }

        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.of(ErrorCode.AUTH_TOKEN_INVALID, "用户不存在");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "账号当前不可用");
        }

        List<RoleVO> roles = userService.listRolesByUser(user.getId());
        String newJti = newJti();
        LoginResponse response = issueTokens(user, roles, existing.getDeviceId(), newJti);

        // 吊销旧令牌，记录轮换关系
        existing.setRevoked(1);
        existing.setReplacedByJti(newJti);
        existing.setRevokedAt(LocalDateTime.now());
        refreshTokenMapper.updateById(existing);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logout(HttpServletRequest httpRequest) {
        Long userId = SecurityUtils.getCurrentUserIdOrDefault();
        List<SysRefreshToken> tokens =
                refreshTokenMapper.selectList(
                        new LambdaQueryWrapper<SysRefreshToken>()
                                .eq(SysRefreshToken::getUserId, userId)
                                .eq(SysRefreshToken::getRevoked, 0));
        LocalDateTime now = LocalDateTime.now();
        for (SysRefreshToken token : tokens) {
            token.setRevoked(1);
            token.setRevokedAt(now);
            refreshTokenMapper.updateById(token);
        }
        SysUser user = userMapper.selectById(userId);
        logOperation(
                userId,
                user == null ? null : user.getUsername(),
                OperationType.LOGOUT,
                "用户登出",
                userId,
                "USER",
                httpRequest);
    }

    @Override
    public PageResult<UserSessionVO> sessions(long page, long size) {
        Long userId = SecurityUtils.getCurrentUserIdOrDefault();
        Page<SysRefreshToken> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<SysRefreshToken> wrapper =
                new LambdaQueryWrapper<SysRefreshToken>()
                        .eq(SysRefreshToken::getUserId, userId)
                        .eq(SysRefreshToken::getRevoked, 0)
                        .orderByDesc(SysRefreshToken::getCreateTime);
        IPage<SysRefreshToken> result = refreshTokenMapper.selectPage(p, wrapper);

        List<SysRefreshToken> records = result.getRecords();
        Map<String, SysUserDevice> deviceMap = loadDevices(userId, records);
        List<UserSessionVO> vos = new ArrayList<>();
        for (SysRefreshToken token : records) {
            UserSessionVO vo = new UserSessionVO();
            vo.setSessionId(token.getJti());
            vo.setUserId(token.getUserId());
            vo.setLoginTime(token.getCreateTime());
            vo.setLastActiveTime(token.getUpdateTime());
            SysUserDevice device =
                    token.getDeviceId() == null ? null : deviceMap.get(token.getDeviceId());
            if (device != null) {
                vo.setPlatform(device.getPlatform());
                vo.setIpAddress(device.getLastLoginIp());
            }
            vos.add(vo);
        }
        return new PageResult<>(vos, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeSession(String sessionId) {
        SysRefreshToken token =
                refreshTokenMapper.selectOne(
                        new LambdaQueryWrapper<SysRefreshToken>()
                                .eq(SysRefreshToken::getJti, sessionId));
        if (token == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "会话不存在");
        }
        Long currentUserId = SecurityUtils.getCurrentUserIdOrDefault();
        boolean owner = token.getUserId().equals(currentUserId);
        boolean admin = isSystemAdmin(currentUserId);
        if (!owner && !admin) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "无权吊销他人会话");
        }
        token.setRevoked(1);
        token.setRevokedAt(LocalDateTime.now());
        refreshTokenMapper.updateById(token);
    }

    @Override
    public SignSecretVO signSecret(String deviceId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "未登录");
        }
        if (StringUtils.isBlank(deviceId)) {
            throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "缺少设备标识 deviceId");
        }
        SysUserDevice device =
                userDeviceMapper.selectOne(
                        new LambdaQueryWrapper<SysUserDevice>()
                                .eq(SysUserDevice::getUserId, userId)
                                .eq(SysUserDevice::getDeviceId, deviceId)
                                .last("LIMIT 1"));
        if (device == null || StringUtils.isBlank(device.getAppSecret())) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "设备未绑定");
        }
        // 缓存过期时间对齐 access token 生命周期，客户端据此定时刷新
        long expiresAt = System.currentTimeMillis() + accessTtlSeconds * 1000L;
        return new SignSecretVO(device.getAppSecret(), expiresAt);
    }

    // ==================== 内部辅助 ====================

    private LoginResponse issueTokens(
            SysUser user, List<RoleVO> roles, String deviceId, String jti) {
        List<String> roleCodes =
                roles.stream().map(RoleVO::getCode).filter(StringUtils::isNotBlank).toList();
        String accessToken =
                jwtUtil.generateAccessToken(user.getId(), user.getUsername(), roleCodes);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), jti);

        SysRefreshToken entity = new SysRefreshToken();
        entity.setUserId(user.getId());
        entity.setJti(jti);
        entity.setTokenHash(sha256Hex(refreshToken));
        entity.setDeviceId(deviceId);
        entity.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTtlSeconds));
        entity.setRevoked(0);
        refreshTokenMapper.insert(entity);

        UserVO userVO = userConverter.toVO(user);
        userVO.setRoles(roles);

        TokenPair tokenPair = new TokenPair(accessToken, refreshToken, accessTtlSeconds);
        return new LoginResponse(tokenPair, userVO);
    }

    private String newJti() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** 生成 32 字节随机 AppSecret（Base64 URL-safe，无填充）。 */
    private String newAppSecret() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void bindDevice(
            Long userId,
            String deviceId,
            String deviceName,
            com.ceams.module.iam.enums.Platform platform,
            String ip,
            LocalDateTime now) {
        SysUserDevice device =
                userDeviceMapper.selectOne(
                        new LambdaQueryWrapper<SysUserDevice>()
                                .eq(SysUserDevice::getUserId, userId)
                                .eq(SysUserDevice::getDeviceId, deviceId)
                                .last("LIMIT 1"));
        if (device == null) {
            device = new SysUserDevice();
            device.setUserId(userId);
            device.setDeviceId(deviceId);
            device.setBindTime(now);
            device.setCreateBy(userId);
            device.setStatus(DeviceStatus.ACTIVE);
            device.setLastLoginIp(ip);
            device.setLastLoginTime(now);
            device.setDeviceName(deviceName);
            device.setPlatform(platform);
            // 首次绑定时生成独立 AppSecret（库内经 SensitiveTypeHandler 加密落库）
            device.setAppSecret(newAppSecret());
            userDeviceMapper.insert(device);
        } else {
            // 再次登录命中已有设备：保留原 AppSecret 不重新生成，避免客户端存量密钥失效。
            // 密钥轮换通过「解绑后重新绑定」自然触发。
            device.setStatus(DeviceStatus.ACTIVE);
            device.setLastLoginIp(ip);
            device.setLastLoginTime(now);
            if (StringUtils.isNotBlank(deviceName)) {
                device.setDeviceName(deviceName);
            }
            if (platform != null) {
                device.setPlatform(platform);
            }
            userDeviceMapper.updateById(device);
        }
    }

    private Map<String, SysUserDevice> loadDevices(Long userId, List<SysRefreshToken> records) {
        List<String> deviceIds =
                records.stream()
                        .map(SysRefreshToken::getDeviceId)
                        .filter(StringUtils::isNotBlank)
                        .distinct()
                        .toList();
        if (deviceIds.isEmpty()) {
            return new HashMap<>();
        }
        List<SysUserDevice> devices =
                userDeviceMapper.selectList(
                        new LambdaQueryWrapper<SysUserDevice>()
                                .eq(SysUserDevice::getUserId, userId)
                                .in(SysUserDevice::getDeviceId, deviceIds));
        return devices.stream()
                .collect(
                        Collectors.toMap(
                                SysUserDevice::getDeviceId, Function.identity(), (a, b) -> a));
    }

    private boolean isSystemAdmin(Long userId) {
        if (userId == null || userId == 0L) {
            return false;
        }
        List<Long> roleIds =
                userRoleMapper
                        .selectList(
                                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId))
                        .stream()
                        .map(UserRole::getRoleId)
                        .distinct()
                        .toList();
        if (roleIds.isEmpty()) {
            return false;
        }
        List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
        return roles.stream().anyMatch(r -> SYSTEM_ADMIN.equals(r.getCode()));
    }

    private void logOperation(
            Long userId,
            String username,
            OperationType type,
            String description,
            Long targetId,
            String targetType,
            HttpServletRequest request) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setOperationType(type);
        log.setOperationDescription(description);
        log.setTargetId(targetId);
        log.setTargetType(targetType);
        log.setIpAddress(resolveIp(request));
        log.setUserAgent(request == null ? null : request.getHeader("User-Agent"));
        log.setRequestMethod(request == null ? null : request.getMethod());
        log.setRequestUri(request == null ? null : request.getRequestURI());
        log.setStatus("SUCCESS");
        operationLogMapper.insert(log);
    }

    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }
}
