package com.ceams.module.doc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.bpm.enums.AuditAction;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.doc.config.DocProperties;
import com.ceams.module.doc.dto.UploadInitRequest;
import com.ceams.module.doc.entity.DocDirectory;
import com.ceams.module.doc.entity.DocFile;
import com.ceams.module.doc.entity.DocFileVersion;
import com.ceams.module.doc.enums.DocAction;
import com.ceams.module.doc.enums.DocFileStatus;
import com.ceams.module.doc.enums.DocVersionSource;
import com.ceams.module.doc.mapper.DocDirectoryMapper;
import com.ceams.module.doc.mapper.DocFileMapper;
import com.ceams.module.doc.mapper.DocFileVersionMapper;
import com.ceams.module.doc.service.DocUploadService;
import com.ceams.module.doc.support.DocAccessResolver;
import com.ceams.module.doc.support.DocConverter;
import com.ceams.module.doc.support.DocStorageService;
import com.ceams.module.doc.vo.DocFileVO;
import com.ceams.module.doc.vo.UploadInitVO;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档分片上传服务实现。
 *
 * <p>要点：
 *
 * <ol>
 *   <li>限制：单文件 ≤ {@code ceams.doc.max-file-size}，分片 {@code chunk-size}（默认 5MB），分片数 ≤ 1000，
 *       扩展名白名单；文件名只保留基础名并过滤非法字符。
 *   <li>秒传：init 传入的 checksum 命中既有 doc_file（size 相同）时直接建元数据复用 storage_key，物理文件不复制。
 *   <li>分片 meta 存 Redis hash {@code doc:upload:{uploadId}}，TTL {@code upload-temp-ttl-hours}；
 *       分片本体落 {@code {root}/tmp/chunks/{uploadId}/}。
 *   <li>合并后计算 SHA-256 与 init checksum 比对，不一致删除目标文件与临时目录并报 400。
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocUploadServiceImpl implements DocUploadService {

    /** 审计目标类型。 */
    private static final String AUDIT_TARGET = "DOC_FILE";

    /** 分片 meta Redis key 前缀。 */
    private static final String UPLOAD_KEY_PREFIX = "doc:upload:";

    /** 分片数量上限。 */
    private static final int MAX_CHUNKS = 1000;

    /** 文件名最大长度。 */
    private static final int MAX_NAME_LENGTH = 255;

    /** 文件名非法字符（含路径分隔符）。 */
    private static final String ILLEGAL_NAME_CHARS = "/\\:*?\"<>|";

    /** 允许的扩展名白名单（小写）。 */
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of(
                    "doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf", "txt", "md", "csv", "zip",
                    "png", "jpg", "jpeg", "webp");

    /** SHA-256 hex 格式。 */
    private static final Pattern CHECKSUM_PATTERN = Pattern.compile("^[0-9a-fA-F]{64}$");

    private final DocDirectoryMapper dirMapper;
    private final DocFileMapper fileMapper;
    private final DocFileVersionMapper versionMapper;
    private final DocAccessResolver accessResolver;
    private final DocStorageService storageService;
    private final StringRedisTemplate stringRedisTemplate;
    private final BpmAuditService bpmAuditService;
    private final DocProperties docProperties;

    @Override
    public UploadInitVO init(UploadInitRequest request) {
        Long userId = requireCurrentUserId();
        DocDirectory dir = requireDir(request.getDirId());
        accessResolver.require(DocAction.EDIT, dir, null);

        String name = normalizeFileName(request.getFileName());
        String ext = extOf(name);
        if (ext == null || !ALLOWED_EXTENSIONS.contains(ext)) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "不支持的文件类型：" + (ext == null ? "无扩展名" : ext));
        }
        long size = request.getSize() == null ? 0L : request.getSize();
        if (size <= 0) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "文件大小必须大于 0");
        }
        long maxFileSize = docProperties.getMaxFileSize();
        if (size > maxFileSize) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT,
                    "文件大小不能超过 " + (maxFileSize / 1024 / 1024) + "MB");
        }
        int chunkSize = resolveChunkSize();
        int totalChunks = (int) ((size + chunkSize - 1) / chunkSize);
        if (totalChunks > MAX_CHUNKS) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "分片数超过 " + MAX_CHUNKS + "，请减少文件大小");
        }
        String checksum = normalizeChecksum(request.getChecksum());

        ensureNameUnique(name, dir.getId());

        // 秒传：checksum + size 命中既有文件，直接建元数据复用物理文件
        if (checksum != null) {
            DocFile existing =
                    fileMapper.selectOne(
                            new LambdaQueryWrapper<DocFile>()
                                    .eq(DocFile::getChecksum, checksum)
                                    .eq(DocFile::getSize, size)
                                    .last("LIMIT 1"));
            if (existing != null) {
                DocFile file =
                        createFile(
                                dir.getId(),
                                name,
                                ext,
                                size,
                                existing.getStorageKey(),
                                existing.getChecksum(),
                                userId);
                UploadInitVO vo = new UploadInitVO();
                vo.setInstant(true);
                vo.setFileId(file.getId());
                vo.setFileName(name);
                vo.setChunkSize(chunkSize);
                log.info("文档秒传命中：fileId={}，storageKey={}", file.getId(), existing.getStorageKey());
                return vo;
            }
        }

        String uploadId = UUID.randomUUID().toString();
        storeMeta(uploadId, dir.getId(), name, size, checksum, totalChunks, userId);
        UploadInitVO vo = new UploadInitVO();
        vo.setUploadId(uploadId);
        vo.setChunkSize(chunkSize);
        vo.setUploaded(storageService.listUploadedChunks(uploadId));
        vo.setInstant(false);
        vo.setFileName(name);
        log.info(
                "文档分片上传初始化：uploadId={}，dirId={}，name={}，size={}，totalChunks={}",
                uploadId,
                dir.getId(),
                name,
                size,
                totalChunks);
        return vo;
    }

    @Override
    public List<Integer> saveChunk(String uploadId, int index, MultipartFile file) {
        UploadMeta meta = requireMeta(uploadId);
        if (index < 0 || index >= meta.totalChunks()) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT,
                    "分片序号越界，应在 0 ~ " + (meta.totalChunks() - 1) + " 之间");
        }
        long expected = expectedChunkSize(meta, index);
        if (file == null || file.getSize() != expected) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT,
                    "分片大小不正确，第 " + index + " 片应为 " + expected + " 字节");
        }
        storageService.saveChunk(uploadId, index, file);
        return storageService.listUploadedChunks(uploadId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocFileVO complete(String uploadId) {
        UploadMeta meta = requireMeta(uploadId);
        List<Integer> uploaded = storageService.listUploadedChunks(uploadId);
        List<Integer> missing = new ArrayList<>();
        for (int i = 0; i < meta.totalChunks(); i++) {
            if (!uploaded.contains(i)) {
                missing.add(i);
            }
        }
        if (!missing.isEmpty()) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "分片缺失，请重新上传分片序号：" + missing);
        }

        String ext = extOf(meta.fileName());
        String storageKey = storageService.merge(uploadId, meta.totalChunks(), ext);
        String actualChecksum = storageService.sha256(storageService.resolve(storageKey));
        if (StringUtils.isNotBlank(meta.checksum())
                && !meta.checksum().equalsIgnoreCase(actualChecksum)) {
            storageService.deleteQuietly(storageKey);
            stringRedisTemplate.delete(UPLOAD_KEY_PREFIX + uploadId);
            log.warn(
                    "文档分片合并校验失败：uploadId={}，期望 {}，实际 {}",
                    uploadId,
                    meta.checksum(),
                    actualChecksum);
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "文件校验失败（SHA-256 不一致），请重新上传");
        }

        DocFile file =
                createFile(
                        meta.dirId(),
                        meta.fileName(),
                        ext,
                        meta.size(),
                        storageKey,
                        actualChecksum,
                        meta.userId());
        stringRedisTemplate.delete(UPLOAD_KEY_PREFIX + uploadId);
        log.info("文档上传完成：fileId={}，name={}，key={}", file.getId(), file.getName(), storageKey);
        return DocConverter.toFileVO(file);
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    /** 创建 doc_file 与 v1 版本记录，并写 DOC_UPLOAD 审计。 */
    private DocFile createFile(
            Long dirId,
            String name,
            String ext,
            long size,
            String storageKey,
            String checksum,
            Long userId) {
        DocFile file = new DocFile();
        file.setDirId(dirId);
        file.setName(name);
        file.setExt(ext);
        file.setSize(size);
        file.setStorageKey(storageKey);
        file.setChecksum(checksum);
        file.setCurrentVersionNo(1);
        file.setStatus(DocFileStatus.NORMAL);
        file.setUploadedBy(userId);
        file.setDownloadCount(0);
        fileMapper.insert(file);

        DocFileVersion version = new DocFileVersion();
        version.setFileId(file.getId());
        version.setVersionNo(1);
        version.setStorageKey(storageKey);
        version.setSize(size);
        version.setChecksum(checksum);
        version.setUploadedBy(userId);
        version.setChangeNote("首次上传");
        version.setSource(DocVersionSource.UPLOAD);
        versionMapper.insert(version);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("name", name);
        after.put("size", size);
        after.put("checksum", checksum);
        after.put("currentVersionNo", 1);
        bpmAuditService.record(
                AUDIT_TARGET,
                file.getId(),
                AuditAction.DOC_UPLOAD,
                null,
                after,
                null,
                userId == null ? 0L : userId);
        return file;
    }

    /** 单用户上传校验（未登录直接 401）。 */
    private Long requireCurrentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "未登录不可上传文档");
        }
        return userId;
    }

    private DocDirectory requireDir(Long dirId) {
        DocDirectory dir = dirId == null ? null : dirMapper.selectById(dirId);
        if (dir == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "目录不存在");
        }
        return dir;
    }

    /** 分片大小配置兜底：非法值回退 5MB，避免除零。 */
    private int resolveChunkSize() {
        int chunkSize = docProperties.getChunkSize();
        return chunkSize > 0 ? chunkSize : 5242880;
    }

    /** 第 index 片期望大小：非末片必须等于分片大小，末片为剩余字节数。 */
    private long expectedChunkSize(UploadMeta meta, int index) {
        int chunkSize = resolveChunkSize();
        if (index < meta.totalChunks() - 1) {
            return chunkSize;
        }
        return meta.size() - (long) chunkSize * (meta.totalChunks() - 1);
    }

    /** 同名文件校验（同目录 deleted=0 范围内）。 */
    private void ensureNameUnique(String name, Long dirId) {
        Long count =
                fileMapper.selectCount(
                        new LambdaQueryWrapper<DocFile>()
                                .eq(DocFile::getDirId, dirId)
                                .eq(DocFile::getName, name));
        if (count != null && count > 0) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "同名文件已存在：" + name);
        }
    }

    /** 文件名归一化：只保留基础名，过滤控制字符与 Windows 非法字符。 */
    private String normalizeFileName(String raw) {
        String value = StringUtils.trimToNull(raw);
        if (value == null) {
            throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "文件名不能为空");
        }
        int slash = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
        if (slash >= 0) {
            value = value.substring(slash + 1).trim();
        }
        if (value.isEmpty()) {
            throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "文件名不能为空");
        }
        if (value.length() > MAX_NAME_LENGTH) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "文件名不能超过 " + MAX_NAME_LENGTH + " 个字符");
        }
        if (value.contains("..")) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "文件名不能包含 ..");
        }
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch < 0x20 || ch == 0x7F || ILLEGAL_NAME_CHARS.indexOf(ch) >= 0) {
                throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "文件名包含非法字符：" + value);
            }
        }
        return value;
    }

    /** 取小写扩展名，无扩展名返回 null。 */
    private String extOf(String name) {
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) {
            return null;
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** checksum 归一化：空值表示不走秒传；非空必须是 64 位 hex。 */
    private String normalizeChecksum(String raw) {
        String value = StringUtils.trimToNull(raw);
        if (value == null) {
            return null;
        }
        if (!CHECKSUM_PATTERN.matcher(value).matches()) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "checksum 必须为 64 位十六进制字符串");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    /** 写入分片 meta（Redis hash + TTL）。 */
    private void storeMeta(
            String uploadId,
            Long dirId,
            String fileName,
            long size,
            String checksum,
            int totalChunks,
            Long userId) {
        Map<String, String> meta = new HashMap<>();
        meta.put("dirId", String.valueOf(dirId));
        meta.put("fileName", fileName);
        meta.put("size", String.valueOf(size));
        meta.put("checksum", checksum == null ? "" : checksum);
        meta.put("totalChunks", String.valueOf(totalChunks));
        meta.put("userId", String.valueOf(userId));
        String key = UPLOAD_KEY_PREFIX + uploadId;
        stringRedisTemplate.opsForHash().putAll(key, meta);
        stringRedisTemplate.expire(key, Duration.ofHours(Math.max(1, docProperties.getUploadTempTtlHours())));
    }

    /** 读取分片 meta，缺失（过期 / 已完成）返回 400。 */
    private UploadMeta requireMeta(String uploadId) {
        if (StringUtils.isBlank(uploadId)) {
            throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "上传批次 ID 不能为空");
        }
        Map<Object, Object> raw =
                stringRedisTemplate.opsForHash().entries(UPLOAD_KEY_PREFIX + uploadId);
        if (raw == null || raw.isEmpty()) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "上传任务不存在或已过期，请重新初始化");
        }
        Long dirId = parseLong(raw.get("dirId"));
        String fileName = raw.get("fileName") == null ? null : String.valueOf(raw.get("fileName"));
        Long size = parseLong(raw.get("size"));
        String checksum = raw.get("checksum") == null ? null : String.valueOf(raw.get("checksum"));
        Long totalChunks = parseLong(raw.get("totalChunks"));
        Long userId = parseLong(raw.get("userId"));
        if (dirId == null || StringUtils.isBlank(fileName) || size == null || totalChunks == null) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "上传任务元数据损坏，请重新初始化");
        }
        return new UploadMeta(
                dirId, fileName, size, checksum, totalChunks.intValue(), userId);
    }

    private Long parseLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.isNotBlank(text)) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /** 上传批次元数据。 */
    private record UploadMeta(
            Long dirId, String fileName, long size, String checksum, int totalChunks, Long userId) {}
}
