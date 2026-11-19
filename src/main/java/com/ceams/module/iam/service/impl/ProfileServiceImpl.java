package com.ceams.module.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.iam.service.ProfileService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 个人头像服务实现。
 *
 * <p>参考 {@code FileServiceImpl} 的「本地落盘 + 相对地址返回」既定做法：目录由 {@code
 * ceams.file.upload-dir} 注入（头像固定落在其下 {@code avatars} 子目录），文件名使用雪花 ID +
 * 白名单扩展名，规避原始文件名带来的路径穿越；库内仅保存访问地址。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    /** 头像大小上限：2MB。 */
    private static final long MAX_AVATAR_SIZE = 2L * 1024 * 1024;

    /** 允许的头像扩展名白名单（取原文件名后缀转小写比对）。 */
    private static final Set<String> ALLOWED_AVATAR_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    /** 头像访问地址前缀（与 {@code sys_user.avatar} 存储格式一致）。 */
    private static final String AVATAR_URL_PREFIX = "/api/v1/profile/avatars/";

    /** 头像子目录名（相对 {@code ceams.file.upload-dir}）。 */
    private static final String AVATAR_SUB_DIR = "avatars";

    /** 服务端文件名格式：雪花 ID + 白名单扩展名，防路径穿越。 */
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[A-Za-z0-9_-]+\\.[a-zA-Z0-9]{2,5}$");

    /** webp 媒体类型（Spring 未提供对应常量）。 */
    private static final MediaType IMAGE_WEBP = MediaType.parseMediaType("image/webp");

    private final UserMapper userMapper;

    @Value("${ceams.file.upload-dir:./data/files}")
    private String uploadDir;

    @Override
    public String uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "上传文件不能为空");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "头像大小不能超过 2MB");
        }
        String extension = resolveExtension(file.getOriginalFilename());
        String name = IdWorker.getId() + "." + extension;

        Path dir = avatarDir();
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(name));
        } catch (IOException e) {
            log.error("头像落盘失败", e);
            throw new BusinessException(ErrorCode.BIZ_UNPROCESSABLE, "头像保存失败", e);
        }

        Long userId = requireCurrentUserId();
        SysUser current = userMapper.selectById(userId);
        if (current == null) {
            deleteQuietly(name);
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "当前用户不存在");
        }

        String avatarUrl = AVATAR_URL_PREFIX + name;
        int rows =
                userMapper.update(
                        null,
                        new UpdateWrapper<SysUser>()
                                .set("avatar", avatarUrl)
                                .eq("id", userId)
                                .eq("deleted", 0));
        if (rows == 0) {
            deleteQuietly(name);
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "当前用户不存在");
        }
        // 回写成功后清理旧头像文件，失败不影响主流程
        deleteOldAvatar(current.getAvatar());
        return avatarUrl;
    }

    @Override
    public void clearAvatar() {
        Long userId = requireCurrentUserId();
        SysUser current = userMapper.selectById(userId);
        if (current == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "当前用户不存在");
        }
        userMapper.update(
                null,
                new UpdateWrapper<SysUser>()
                        .set("avatar", null)
                        .eq("id", userId)
                        .eq("deleted", 0));
        deleteOldAvatar(current.getAvatar());
    }

    @Override
    public AvatarFile loadAvatar(String name) {
        if (name == null || !NAME_PATTERN.matcher(name).matches()) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "非法文件名");
        }
        Path target = avatarDir().resolve(name);
        if (!Files.isRegularFile(target)) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "头像不存在");
        }
        try {
            return new AvatarFile(Files.readAllBytes(target), resolveMediaType(name));
        } catch (IOException e) {
            log.error("读取头像失败", e);
            throw new BusinessException(ErrorCode.BIZ_UNPROCESSABLE, "读取头像失败", e);
        }
    }

    /** 头像所在目录：{@code ${ceams.file.upload-dir}/avatars}。 */
    private Path avatarDir() {
        return Path.of(uploadDir, AVATAR_SUB_DIR);
    }

    /** 解析并校验原文件扩展名（统一转小写）。 */
    private String resolveExtension(String originalFilename) {
        if (originalFilename == null) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "文件类型不支持");
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "文件类型不支持");
        }
        String extension = originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_AVATAR_EXTENSIONS.contains(extension)) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "仅支持 jpg/jpeg/png/webp 格式的头像");
        }
        return extension;
    }

    /** 按扩展名映射媒体类型。 */
    private MediaType resolveMediaType(String name) {
        int dot = name.lastIndexOf('.');
        String extension = dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        return switch (extension) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "webp" -> IMAGE_WEBP;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    /** 当前登录用户 ID（未认证时抛未授权类异常）。 */
    private Long requireCurrentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.of(ErrorCode.AUTH_TOKEN_INVALID, "当前未登录或登录已失效");
        }
        return userId;
    }

    /**
     * 尽力删除旧头像文件：仅处理本站头像地址且文件名校验通过的场景，任何失败只告警不抛出。
     *
     * @param avatarUrl 旧头像访问地址（可能为空或非本站地址）
     */
    private void deleteOldAvatar(String avatarUrl) {
        if (avatarUrl == null || !avatarUrl.startsWith(AVATAR_URL_PREFIX)) {
            return;
        }
        String oldName = avatarUrl.substring(AVATAR_URL_PREFIX.length());
        if (!NAME_PATTERN.matcher(oldName).matches()) {
            return;
        }
        deleteQuietly(oldName);
    }

    /** 删除头像文件，失败仅告警。 */
    private void deleteQuietly(String name) {
        try {
            Files.deleteIfExists(avatarDir().resolve(name));
        } catch (IOException e) {
            log.warn("删除旧头像文件失败：{}", name, e);
        }
    }
}
