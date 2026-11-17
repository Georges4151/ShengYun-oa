package com.ceams.module.doc.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.doc.config.DocProperties;
import com.ceams.module.doc.entity.DocDirectory;
import com.ceams.module.doc.entity.DocFile;
import com.ceams.module.doc.enums.DocAction;
import com.ceams.module.doc.mapper.DocDirectoryMapper;
import com.ceams.module.doc.mapper.DocFileMapper;
import com.ceams.module.doc.service.DocDownloadService;
import com.ceams.module.doc.support.DocAccessResolver;
import com.ceams.module.doc.support.DocStorageService;
import com.ceams.module.doc.support.DocTokenSupport;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 文档下载与预览服务实现。
 *
 * <p>下载走一次性短时 token：签名 / 过期由 {@link DocTokenSupport} 校验，{@code jti} 由 Redis
 * {@code doc:token:used:{jti}}（TTL 同 token）原子占用保证只能用一次；预览走登录态 + PREVIEW 数据权限。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocDownloadServiceImpl implements DocDownloadService {

    /** 一次性 token 已用标记前缀。 */
    private static final String USED_KEY_PREFIX = "doc:token:used:";

    /** Office 扩展名集合。 */
    private static final java.util.Set<String> OFFICE_EXTENSIONS =
            java.util.Set.of("doc", "docx", "xls", "xlsx", "ppt", "pptx");

    private final DocFileMapper fileMapper;
    private final DocDirectoryMapper dirMapper;
    private final DocAccessResolver accessResolver;
    private final DocStorageService storageService;
    private final DocTokenSupport tokenSupport;
    private final StringRedisTemplate stringRedisTemplate;
    private final DocProperties docProperties;

    @Override
    public String issueToken(Long fileId) {
        Long userId = requireCurrentUserId();
        DocFile file = requireFile(fileId);
        DocDirectory dir = requireDir(file.getDirId());

        EnumSet<DocAction> actions = accessResolver.resolve(userId, dir, file);
        if (!actions.contains(DocAction.DOWNLOAD) && !actions.contains(DocAction.PREVIEW)) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "无权下载该文档");
        }
        int versionNo = file.getCurrentVersionNo() == null ? 1 : file.getCurrentVersionNo();
        return tokenSupport.signDownload(fileId, userId, versionNo);
    }

    @Override
    public void download(Long fileId, String token, HttpServletResponse response) {
        DocFile file = requireFile(fileId);
        Claims claims = tokenSupport.parse(token);
        if (!tokenSupport.isDocToken(claims)
                || !tokenSupport.hasScope(claims, DocTokenSupport.SCOPE_DOWNLOAD)) {
            throw BusinessException.of(ErrorCode.AUTH_TOKEN_INVALID, "无效的下载令牌");
        }
        if (!Objects.equals(tokenSupport.fileIdOf(claims), fileId)) {
            throw BusinessException.of(ErrorCode.AUTH_TOKEN_INVALID, "下载令牌与文件不匹配");
        }
        String jti = claims.getId();
        if (StringUtils.isBlank(jti)) {
            throw BusinessException.of(ErrorCode.AUTH_TOKEN_INVALID, "下载令牌缺少唯一标识");
        }
        Path path = storageService.resolve(file.getStorageKey());
        if (!Files.isRegularFile(path)) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "物理文件不存在");
        }

        // 一次性占用：SET NX EX，失败即视为已使用
        int ttlSeconds = Math.max(1, docProperties.getDownloadTokenTtlSeconds());
        Boolean firstUse =
                stringRedisTemplate
                        .opsForValue()
                        .setIfAbsent(USED_KEY_PREFIX + jti, "1", Duration.ofSeconds(ttlSeconds));
        if (!Boolean.TRUE.equals(firstUse)) {
            throw BusinessException.of(ErrorCode.AUTH_TOKEN_INVALID, "下载链接已使用，请重新获取");
        }

        long size;
        try {
            size = Files.size(path);
        } catch (IOException e) {
            log.error("读取文件大小失败：{}", path, e);
            throw new BusinessException(ErrorCode.BIZ_UNPROCESSABLE, "读取文件失败", e);
        }
        writeStream(response, path, "application/octet-stream", "attachment", file.getName(), size);
        incrementDownloadCount(fileId);
        log.info("文档下载：fileId={}，name={}，size={}", fileId, file.getName(), size);
    }

    @Override
    public void preview(Long fileId, HttpServletResponse response) {
        DocFile file = requireFile(fileId);
        DocDirectory dir = requireDir(file.getDirId());
        accessResolver.require(DocAction.PREVIEW, dir, file);

        String ext = resolveExt(file);
        if (OFFICE_EXTENSIONS.contains(ext)) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "Office 文档请使用在线编辑");
        }
        String contentType = previewContentType(ext);
        if (contentType == null) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "该类型不支持预览");
        }
        Path path = storageService.resolve(file.getStorageKey());
        if (!Files.isRegularFile(path)) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "物理文件不存在");
        }
        long size;
        try {
            size = Files.size(path);
        } catch (IOException e) {
            log.error("读取文件大小失败：{}", path, e);
            throw new BusinessException(ErrorCode.BIZ_UNPROCESSABLE, "读取文件失败", e);
        }
        writeStream(response, path, contentType, "inline", file.getName(), size);
        log.info("文档预览：fileId={}，name={}，contentType={}", fileId, file.getName(), contentType);
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    private Long requireCurrentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "未登录不可获取下载链接");
        }
        return userId;
    }

    private DocFile requireFile(Long fileId) {
        DocFile file = fileId == null ? null : fileMapper.selectById(fileId);
        if (file == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "文件不存在");
        }
        return file;
    }

    private DocDirectory requireDir(Long dirId) {
        DocDirectory dir = dirId == null ? null : dirMapper.selectById(dirId);
        if (dir == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "目录不存在");
        }
        return dir;
    }

    /** 文件扩展名（优先取实体 ext，缺失时从文件名推导）。 */
    private String resolveExt(DocFile file) {
        if (StringUtils.isNotBlank(file.getExt())) {
            return file.getExt().toLowerCase();
        }
        String name = file.getName();
        if (StringUtils.isBlank(name)) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        return dot < 0 || dot == name.length() - 1 ? "" : name.substring(dot + 1).toLowerCase();
    }

    /** 预览 Content-Type；不支持的类型返回 null。 */
    private String previewContentType(String ext) {
        return switch (ext) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            case "txt", "md" -> "text/plain;charset=UTF-8";
            case "csv" -> "text/csv;charset=UTF-8";
            default -> null;
        };
    }

    /** 流式输出：Content-Disposition 使用 RFC 5987 的 {@code filename*} 形式支持中文名。 */
    private void writeStream(
            HttpServletResponse response,
            Path path,
            String contentType,
            String disposition,
            String fileName,
            long size) {
        response.setContentType(contentType);
        response.setHeader(
                "Content-Disposition",
                disposition
                        + "; filename*=UTF-8''"
                        + URLEncoder.encode(fileName == null ? "download" : fileName, StandardCharsets.UTF_8)
                                .replace("+", "%20"));
        response.setContentLengthLong(size);
        try (InputStream in = Files.newInputStream(path)) {
            in.transferTo(response.getOutputStream());
            response.flushBuffer();
        } catch (IOException e) {
            log.error("文档流式输出失败：{}", path, e);
            throw new BusinessException(ErrorCode.BIZ_UNPROCESSABLE, "文档输出失败", e);
        }
    }

    /** 下载次数 +1（SQL 自增，避免并发覆盖）。 */
    private void incrementDownloadCount(Long fileId) {
        UpdateWrapper<DocFile> wrapper =
                new UpdateWrapper<DocFile>()
                        .eq("id", fileId)
                        .setSql("download_count = download_count + 1");
        fileMapper.update(null, wrapper);
    }
}
