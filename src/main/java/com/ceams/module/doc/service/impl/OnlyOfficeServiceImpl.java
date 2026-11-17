package com.ceams.module.doc.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.bpm.enums.AuditAction;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.doc.config.OnlyOfficeProperties;
import com.ceams.module.doc.entity.DocDirectory;
import com.ceams.module.doc.entity.DocFile;
import com.ceams.module.doc.entity.DocFileVersion;
import com.ceams.module.doc.enums.DocAction;
import com.ceams.module.doc.enums.DocVersionSource;
import com.ceams.module.doc.mapper.DocDirectoryMapper;
import com.ceams.module.doc.mapper.DocFileMapper;
import com.ceams.module.doc.mapper.DocFileVersionMapper;
import com.ceams.module.doc.service.DocLockService;
import com.ceams.module.doc.service.OnlyOfficeService;
import com.ceams.module.doc.support.DocAccessResolver;
import com.ceams.module.doc.support.DocStorageService;
import com.ceams.module.doc.support.DocTokenSupport;
import com.ceams.module.doc.support.OnlyOfficeJwtSupport;
import com.ceams.module.doc.vo.DocLockVO;
import com.ceams.module.doc.vo.EditorConfigVO;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * OnlyOffice 在线编辑服务实现。
 *
 * <p>口径：
 *
 * <ol>
 *   <li>降级：{@code enabled=false}、{@code jwt-secret} 为空或 {@code /healthcheck} 探活失败时，
 *       editor-config 返回 503「在线编辑暂不可用」；上传/下载/版本链路不经过本类，不受影响。
 *   <li>取件：{@code /api/v1/doc/onlyoffice/file?token=} 只校验 token 的 {@code typ/scope/fid}，
 *       <b>不消耗</b>一次性 nonce（Document Server 可能多次拉取同一原文）。
 *   <li>回调：JWT 校验 → key 格式 → status ∈ {2,6} → 回调域名可信 → Redis 幂等 → 下载重试 →
 *       checksum 相同仅释放锁；不同则落盘生成新版本、更新 {@code doc_file}、释放锁并审计 {@code DOC_EDIT}。
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnlyOfficeServiceImpl implements OnlyOfficeService {

    /** 可在线编辑的扩展名。 */
    private static final Set<String> EDITABLE_EXTENSIONS =
            Set.of("doc", "docx", "xls", "xlsx", "ppt", "pptx");

    /** 需要落库保存的回调状态（2=文档已关闭，6=强制保存）。 */
    private static final Set<Integer> SAVE_STATUSES = Set.of(2, 6);

    /** 回调幂等键前缀。 */
    private static final String CALLBACK_IDEMPOTENT_PREFIX = "doc:oo:cb:";

    /** 回调幂等有效期（分钟）。 */
    private static final int CALLBACK_IDEMPOTENT_MINUTES = 10;

    /** 保存回调下载失败后的退避时间（毫秒），共重试 3 次。 */
    private static final long[] DOWNLOAD_BACKOFF_MILLIS = {1000L, 2000L, 4000L};

    /** 版本号冲突最大重试次数。 */
    private static final int MAX_VERSION_RETRY = 3;

    /** 回调 key 格式：{fileId}_{versionNo}。 */
    private static final Pattern CALLBACK_KEY_PATTERN = Pattern.compile("^(\\d+)_(\\d+)$");

    /** 审计目标类型。 */
    private static final String AUDIT_TARGET = "DOC_FILE";

    /** 在线编辑保存版本说明。 */
    private static final String CHANGE_NOTE_ONLINE_EDIT = "在线编辑保存";

    /** 回调无登录上下文时，审计操作人的系统用户兜底 ID。 */
    private static final long SYSTEM_USER_ID = 1L;

    private final DocFileMapper fileMapper;
    private final DocDirectoryMapper dirMapper;
    private final DocFileVersionMapper versionMapper;
    private final DocAccessResolver accessResolver;
    private final DocLockService lockService;
    private final DocStorageService storageService;
    private final DocTokenSupport tokenSupport;
    private final OnlyOfficeJwtSupport jwtSupport;
    private final OnlyOfficeProperties properties;
    private final RestTemplate restTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final BpmAuditService bpmAuditService;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    public EditorConfigVO buildEditorConfig(Long fileId) {
        requireAvailable();

        DocFile file = requireFile(fileId);
        String ext = resolveExt(file);
        if (!EDITABLE_EXTENSIONS.contains(ext)) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT,
                    "该文件类型不支持在线编辑：" + (ext.isEmpty() ? "无扩展名" : ext));
        }
        DocDirectory dir = requireDir(file.getDirId());
        Long userId = requireCurrentUserId();

        accessResolver.require(DocAction.PREVIEW, dir, file);
        EnumSet<DocAction> actions = accessResolver.resolve(userId, dir, file);
        boolean canEdit = actions != null && actions.contains(DocAction.EDIT);
        if (canEdit) {
            DocLockVO lock = lockService.status(fileId);
            if (lock == null || !lock.isLocked() || !Objects.equals(lock.getOwnerId(), userId)) {
                throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "请先获取编辑锁");
            }
        }
        String mode = canEdit ? "edit" : "view";
        int versionNo = file.getCurrentVersionNo() == null ? 1 : file.getCurrentVersionNo();
        String fileToken = tokenSupport.signOnlyOfficeFile(fileId, versionNo);
        String documentUrl =
                normalizeBase(properties.getPublicBaseUrl())
                        + "/api/v1/doc/onlyoffice/file?token="
                        + fileToken;

        Map<String, Object> config = buildConfig(file, ext, mode, canEdit, versionNo, documentUrl, userId);
        String token = jwtSupport.signConfig(config);

        EditorConfigVO vo = new EditorConfigVO();
        vo.setDocumentServerUrl(normalizeBase(properties.getDocumentServerUrl()));
        // 服务端地址为容器内地址，浏览器无法加载 api.js，故单独返回浏览器可达地址
        vo.setBrowserDocumentServerUrl(normalizeBase(properties.getBrowserDocumentServerUrl()));
        vo.setConfig(config);
        vo.setToken(token);
        vo.setMode(mode);
        vo.setFileId(fileId);
        log.info("OnlyOffice 编辑器配置已生成：fileId={}，mode={}，version={}", fileId, mode, versionNo);
        return vo;
    }

    @Override
    public void serveDocument(Long fileId, String token, HttpServletResponse response) {
        Claims claims = tokenSupport.parse(token);
        if (!tokenSupport.isDocToken(claims)
                || !tokenSupport.hasScope(claims, DocTokenSupport.SCOPE_ONLYOFFICE_FILE)) {
            throw BusinessException.of(ErrorCode.AUTH_TOKEN_INVALID, "无效的取件令牌");
        }
        Long tokenFileId = tokenSupport.fileIdOf(claims);
        if (tokenFileId == null) {
            throw BusinessException.of(ErrorCode.AUTH_TOKEN_INVALID, "取件令牌缺少文件 ID");
        }
        if (fileId != null && !Objects.equals(tokenFileId, fileId)) {
            throw BusinessException.of(ErrorCode.AUTH_TOKEN_INVALID, "取件令牌与文件不匹配");
        }
        DocFile file = fileMapper.selectById(tokenFileId);
        if (file == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "文件不存在");
        }
        Path path = storageService.resolve(file.getStorageKey());
        if (!Files.isRegularFile(path)) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "物理文件不存在");
        }
        long size;
        try {
            size = Files.size(path);
        } catch (IOException e) {
            log.error("读取文档大小失败：{}", path, e);
            throw new BusinessException(ErrorCode.BIZ_UNPROCESSABLE, "读取文件失败", e);
        }
        writeInline(response, path, contentTypeOf(resolveExt(file)), file.getName(), size);
        log.info("OnlyOffice 取件：fileId={}，name={}，size={}", tokenFileId, file.getName(), size);
    }

    @Override
    public Map<String, Integer> callback(String authorizationHeader, String body) {
        JsonNode root = parseBody(body);
        if (root == null) {
            log.warn("OnlyOffice 回调请求体非法或为空");
            return error();
        }
        String token = extractToken(authorizationHeader, root);
        try {
            jwtSupport.verify(token);
        } catch (RuntimeException e) {
            log.warn("OnlyOffice 回调 JWT 校验失败：{}", e.getMessage());
            return error();
        }

        String key = root.path("key").asText(null);
        int status = root.path("status").asInt(-1);
        String url = root.path("url").asText(null);
        Matcher matcher = key == null ? null : CALLBACK_KEY_PATTERN.matcher(key);
        if (matcher == null || !matcher.matches()) {
            log.warn("OnlyOffice 回调 key 格式非法：{}", key);
            return error();
        }
        Long fileId = parseLong(matcher.group(1));
        if (fileId == null) {
            log.warn("OnlyOffice 回调 key 中的文件 ID 非法：{}", key);
            return error();
        }
        int userCount = root.path("users").isArray() ? root.path("users").size() : 0;
        log.info("OnlyOffice 回调：key={}，status={}，users={}", key, status, userCount);
        if (!SAVE_STATUSES.contains(status)) {
            log.info("OnlyOffice 回调非保存状态，忽略：fileId={}，status={}", fileId, status);
            return ok();
        }
        if (!isTrustedCallbackUrl(url)) {
            log.warn("OnlyOffice 回调 url 域名不可信：{}", url);
            return error();
        }

        String idempotentKey =
                CALLBACK_IDEMPOTENT_PREFIX + md5Hex(key + "|" + status + "|" + url);
        Boolean first =
                stringRedisTemplate
                        .opsForValue()
                        .setIfAbsent(
                                idempotentKey,
                                "1",
                                Duration.ofMinutes(CALLBACK_IDEMPOTENT_MINUTES));
        if (!Boolean.TRUE.equals(first)) {
            log.info("OnlyOffice 回调重复，按幂等成功处理：fileId={}，status={}", fileId, status);
            return ok();
        }

        DocFile file = fileMapper.selectById(fileId);
        if (file == null) {
            log.warn("OnlyOffice 回调目标文件不存在：fileId={}", fileId);
            return error();
        }
        byte[] content;
        try {
            content = downloadWithRetry(url);
        } catch (RuntimeException e) {
            // 保留幂等键 10 分钟，由人工排查后重试，避免 Document Server 高频重试打爆存储
            log.error("OnlyOffice 回调下载文档失败：fileId={}，url={}", fileId, url, e);
            return error();
        }

        String checksum = sha256Hex(content);
        if (StringUtils.equalsIgnoreCase(checksum, file.getChecksum())) {
            lockService.forceUnlock(fileId);
            log.info("OnlyOffice 回调内容未变更，仅释放编辑锁：fileId={}", fileId);
            return ok();
        }
        try {
            saveNewVersion(file, content, checksum);
        } catch (RuntimeException e) {
            log.error("OnlyOffice 回调保存新版本失败：fileId={}", fileId, e);
            return error();
        }
        return ok();
    }

    // ------------------------------------------------------------------
    // 可用性 / 探活
    // ------------------------------------------------------------------

    /** 可用性前置校验：开关、密钥、探活任一不满足即 503（降级为下载）。 */
    private void requireAvailable() {
        if (!properties.isEnabled()) {
            throw unavailable();
        }
        if (StringUtils.isBlank(properties.getJwtSecret())) {
            throw unavailable();
        }
        if (!healthCheck()) {
            throw unavailable();
        }
    }

    private BusinessException unavailable() {
        return BusinessException.of(ErrorCode.SYSTEM_SERVICE_UNAVAILABLE, "在线编辑暂不可用");
    }

    /** 探活 {@code GET {documentServerUrl}/healthcheck}，异常或非 true 均视为不可用。 */
    private boolean healthCheck() {
        String base = normalizeBase(properties.getDocumentServerUrl());
        if (StringUtils.isBlank(base)) {
            return false;
        }
        try {
            String body = restTemplate.getForObject(base + "/healthcheck", String.class);
            return body != null && body.toLowerCase(Locale.ROOT).contains("true");
        } catch (RuntimeException e) {
            log.warn("OnlyOffice 探活失败：{}（{}）", base, e.getClass().getSimpleName());
            return false;
        }
    }

    // ------------------------------------------------------------------
    // 编辑器配置
    // ------------------------------------------------------------------

    /** 组装 OnlyOffice config：document / editorConfig / type。 */
    private Map<String, Object> buildConfig(
            DocFile file,
            String ext,
            String mode,
            boolean canEdit,
            int versionNo,
            String documentUrl,
            Long userId) {
        Map<String, Object> permissions = new LinkedHashMap<>();
        permissions.put("edit", canEdit);
        permissions.put("download", true);
        permissions.put("print", true);
        permissions.put("comment", canEdit);

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("fileType", ext);
        document.put("key", file.getId() + "_" + versionNo);
        document.put("title", file.getName());
        document.put("url", documentUrl);
        document.put("permissions", permissions);

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", String.valueOf(userId));
        user.put("name", userName(userId));

        Map<String, Object> editorConfig = new LinkedHashMap<>();
        editorConfig.put("mode", mode);
        editorConfig.put(
                "callbackUrl",
                normalizeBase(properties.getPublicBaseUrl())
                        + normalizePath(properties.getCallbackPath()));
        editorConfig.put("lang", "zh-CN");
        editorConfig.put("user", user);

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("document", document);
        config.put("editorConfig", editorConfig);
        config.put("type", "desktop");
        return config;
    }

    /** 用户名：取不到时回退为「用户{ID}」，保证 config 结构完整。 */
    private String userName(Long userId) {
        if (userId == null) {
            return "未知用户";
        }
        SysUser user = userMapper.selectById(userId);
        if (user != null && StringUtils.isNotBlank(user.getName())) {
            return user.getName();
        }
        return "用户" + userId;
    }

    // ------------------------------------------------------------------
    // 回调处理
    // ------------------------------------------------------------------

    /** 解析回调 JSON，非法返回 null。 */
    private JsonNode parseBody(String body) {
        if (StringUtils.isBlank(body)) {
            return null;
        }
        try {
            return objectMapper.readTree(body);
        } catch (IOException e) {
            log.warn("OnlyOffice 回调 JSON 解析失败：{}", e.getMessage());
            return null;
        }
    }

    /** token 优先取 Authorization（去 Bearer 前缀），其次取 body.token。 */
    private String extractToken(String authorizationHeader, JsonNode root) {
        String header = StringUtils.trimToNull(authorizationHeader);
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            header = StringUtils.trimToNull(header.substring(7));
        }
        if (StringUtils.isNotBlank(header)) {
            return header;
        }
        return root.path("token").asText(null);
    }

    /**
     * 回调 url 可信校验：host:port 必须属于 Document Server 或 CEAMS 自身（public-base-url）。
     *
     * <p>Document Server 正常回调 url 指向其自身；端到端联调时也可配置为 CEAMS 自身地址，
     * 两者都在受信清单内，避免 SSRF 到任意地址。
     */
    private boolean isTrustedCallbackUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }
        return sameHostAndPort(url, properties.getDocumentServerUrl())
                || sameHostAndPort(url, properties.getPublicBaseUrl());
    }

    private boolean sameHostAndPort(String url, String base) {
        if (StringUtils.isBlank(base)) {
            return false;
        }
        try {
            URI target = URI.create(url);
            URI trust = URI.create(base);
            if (target.getHost() == null || trust.getHost() == null) {
                return false;
            }
            return target.getHost().equalsIgnoreCase(trust.getHost())
                    && effectivePort(target) == effectivePort(trust);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private int effectivePort(URI uri) {
        if (uri.getPort() > 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    /** 下载保存内容：失败重试 3 次（退避 1s / 2s / 4s）。 */
    private byte[] downloadWithRetry(String url) {
        RuntimeException last = null;
        for (int attempt = 0; attempt <= DOWNLOAD_BACKOFF_MILLIS.length; attempt++) {
            try {
                return download(url);
            } catch (RuntimeException e) {
                last = e;
                if (attempt == DOWNLOAD_BACKOFF_MILLIS.length) {
                    break;
                }
                log.warn(
                        "OnlyOffice 回调下载失败，第 {} 次重试：url={}（{}）",
                        attempt + 1,
                        url,
                        e.getMessage());
                sleep(DOWNLOAD_BACKOFF_MILLIS[attempt]);
            }
        }
        throw last == null ? new IllegalStateException("下载回调文档失败") : last;
    }

    /** 单次下载（包级可见，便于单元测试 mock）。 */
    byte[] download(String url) {
        ResponseEntity<byte[]> response =
                restTemplate.exchange(
                        URI.create(url), HttpMethod.GET, HttpEntity.EMPTY, byte[].class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("下载回调文档失败：HTTP " + response.getStatusCode());
        }
        return response.getBody();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("下载重试被中断", e);
        }
    }

    /** 落盘并生成新版本（唯一键冲突重试 3 次），随后更新文件元数据、释放锁并审计。 */
    private void saveNewVersion(DocFile file, byte[] content, String checksum) {
        String newKey = storageService.write(content, file.getExt());
        long size = content.length;
        int previousVersionNo = file.getCurrentVersionNo() == null ? 0 : file.getCurrentVersionNo();
        int nextVersionNo = previousVersionNo + 1;
        Long uploadedBy = file.getUploadedBy() == null ? SYSTEM_USER_ID : file.getUploadedBy();

        int attempt = 0;
        while (true) {
            attempt++;
            DocFileVersion version = new DocFileVersion();
            version.setFileId(file.getId());
            version.setVersionNo(nextVersionNo);
            version.setStorageKey(newKey);
            version.setSize(size);
            version.setChecksum(checksum);
            version.setUploadedBy(uploadedBy);
            version.setChangeNote(CHANGE_NOTE_ONLINE_EDIT);
            version.setSource(DocVersionSource.ONLINE_EDIT);
            try {
                versionMapper.insert(version);
                break;
            } catch (DuplicateKeyException e) {
                if (attempt >= MAX_VERSION_RETRY) {
                    // 最终失败时清理本次落盘文件，避免产生孤儿文件
                    storageService.deleteQuietly(newKey);
                    log.warn(
                            "版本号冲突重试 {} 次仍失败：fileId={}，versionNo={}",
                            attempt,
                            file.getId(),
                            nextVersionNo);
                    throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "版本号冲突，请稍后重试");
                }
                nextVersionNo++;
            }
        }

        UpdateWrapper<DocFile> wrapper =
                new UpdateWrapper<DocFile>()
                        .eq("id", file.getId())
                        .set("size", size)
                        .set("checksum", checksum)
                        .set("current_version_no", nextVersionNo)
                        .set("update_time", LocalDateTime.now());
        fileMapper.update(null, wrapper);

        lockService.forceUnlock(file.getId());

        Map<String, Object> before = new LinkedHashMap<>();
        before.put("currentVersionNo", previousVersionNo);
        before.put("size", file.getSize());
        before.put("checksum", file.getChecksum());
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("currentVersionNo", nextVersionNo);
        after.put("size", size);
        after.put("checksum", checksum);
        // 回调无登录上下文：操作人用文件上传人兜底
        bpmAuditService.record(
                AUDIT_TARGET, file.getId(), AuditAction.DOC_EDIT, before, after, null, uploadedBy);

        log.info(
                "OnlyOffice 在线保存成功：fileId={}，v{} -> v{}，key={}",
                file.getId(),
                previousVersionNo,
                nextVersionNo,
                newKey);
    }

    // ------------------------------------------------------------------
    // 通用工具
    // ------------------------------------------------------------------

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

    private Long requireCurrentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "未登录不可获取编辑器配置");
        }
        return userId;
    }

    /** 扩展名：优先实体 ext，缺失时从文件名推导。 */
    private String resolveExt(DocFile file) {
        if (StringUtils.isNotBlank(file.getExt())) {
            return file.getExt().toLowerCase(Locale.ROOT);
        }
        String name = file.getName();
        if (StringUtils.isBlank(name)) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        return dot < 0 || dot == name.length() - 1
                ? ""
                : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** 取件的 Content-Type（Office 类型给精确 MIME，未知回退二进制流）。 */
    private String contentTypeOf(String ext) {
        return switch (ext) {
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "doc" -> "application/msword";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "xls" -> "application/vnd.ms-excel";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "ppt" -> "application/vnd.ms-powerpoint";
            default -> "application/octet-stream";
        };
    }

    /** 内联输出：Content-Disposition 使用 RFC 5987 的 filename* 形式支持中文名。 */
    private void writeInline(
            HttpServletResponse response,
            Path path,
            String contentType,
            String fileName,
            long size) {
        response.setContentType(contentType);
        response.setHeader(
                "Content-Disposition",
                "inline; filename*=UTF-8''"
                        + URLEncoder.encode(
                                        fileName == null ? "document" : fileName,
                                        StandardCharsets.UTF_8)
                                .replace("+", "%20"));
        response.setContentLengthLong(size);
        try (InputStream in = Files.newInputStream(path)) {
            in.transferTo(response.getOutputStream());
            response.flushBuffer();
        } catch (IOException e) {
            log.error("OnlyOffice 取件输出失败：{}", path, e);
            throw new BusinessException(ErrorCode.BIZ_UNPROCESSABLE, "文档输出失败", e);
        }
    }

    /** 去掉 base 末尾斜杠。 */
    private String normalizeBase(String base) {
        String value = StringUtils.trimToEmpty(base);
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    /** 回调路径统一以 / 开头。 */
    private String normalizePath(String path) {
        String value = StringUtils.trimToEmpty(path);
        if (value.isEmpty()) {
            return "/";
        }
        return value.startsWith("/") ? value : "/" + value;
    }

    private Map<String, Integer> ok() {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("error", 0);
        return result;
    }

    private Map<String, Integer> error() {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("error", 1);
        return result;
    }

    private Long parseLong(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** SHA-256（小写 hex）。 */
    private String sha256Hex(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ErrorCode.SYSTEM_INTERNAL_ERROR, "SHA-256 算法不可用", e);
        }
    }

    /** MD5（小写 hex），仅用于回调幂等键，不作安全用途。 */
    private String md5Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ErrorCode.SYSTEM_INTERNAL_ERROR, "MD5 算法不可用", e);
        }
    }
}
