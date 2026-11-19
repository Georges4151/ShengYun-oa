package com.ceams.module.doc.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.doc.entity.DocFile;
import com.ceams.module.doc.mapper.DocFileMapper;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档存储服务：分片落盘、按序合并、SHA-256 校验、越界解析与引用计数。
 *
 * <p>落盘约定：
 *
 * <ul>
 *   <li>根目录 {@code ceams.file.upload-dir}（默认 {@code ./data/files}）。
 *   <li>分片临时目录 {@code {root}/tmp/chunks/{uploadId}/}，文件名 {@code 00000.part} 形式。
 *   <li>正式文件 {@code {root}/doc/{yyyyMM}/{雪花ID}.{ext}}，返回相对 key 使用正斜杠。
 * </ul>
 *
 * <p>落盘命名一律用雪花 ID，不使用用户文件名做路径，规避路径穿越；{@link #resolve(String)}
 * 额外做规范化越界校验作为兜底。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocStorageService {

    /** 上传根目录配置项。 */
    @Value("${ceams.file.upload-dir:./data/files}")
    private String uploadDir;

    /** 分片文件名格式：5 位补零序号 + .part。 */
    private static final String PART_SUFFIX = ".part";

    /** 分片文件名匹配。 */
    private static final Pattern PART_PATTERN = Pattern.compile("^(\\d{1,6})\\.part$");

    /** 正式文件月份目录格式。 */
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    /** 复制缓冲区大小（64KB）。 */
    private static final int BUFFER_SIZE = 64 * 1024;

    private final DocFileMapper docFileMapper;

    /** 上传根目录（包级可见 setter，供单元测试注入临时目录）。 */
    void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    /** 根目录（绝对路径、已规范化）。 */
    public Path root() {
        String dir = StringUtils.isBlank(uploadDir) ? "./data/files" : uploadDir;
        return Path.of(dir).toAbsolutePath().normalize();
    }

    /** 分片临时目录：{@code {root}/tmp/chunks/{uploadId}/}。 */
    public Path chunkDir(String uploadId) {
        String id = requireUploadId(uploadId);
        return root().resolve("tmp").resolve("chunks").resolve(id);
    }

    /** 单个分片路径。 */
    public Path partPath(String uploadId, int index) {
        if (index < 0) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "分片序号不能为负数");
        }
        return chunkDir(uploadId).resolve(String.format("%05d", index) + PART_SUFFIX);
    }

    /** 保存分片（覆盖写，自动建目录）。 */
    public void saveChunk(String uploadId, int index, MultipartFile file) {
        if (file == null) {
            throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "分片文件不能为空");
        }
        Path target = partPath(uploadId, index);
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("分片落盘失败：uploadId={}，index={}", uploadId, index, e);
            throw new BusinessException(ErrorCode.BIZ_UNPROCESSABLE, "分片落盘失败，请重试", e);
        }
    }

    /** 已上传分片序号（升序）；目录不存在返回空列表。 */
    public List<Integer> listUploadedChunks(String uploadId) {
        Path dir = chunkDir(uploadId);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        List<Integer> indexes = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            files.forEach(
                    path -> {
                        Matcher matcher = PART_PATTERN.matcher(path.getFileName().toString());
                        if (matcher.matches()) {
                            indexes.add(Integer.parseInt(matcher.group(1)));
                        }
                    });
        } catch (IOException e) {
            log.warn("读取分片临时目录失败：{}", dir, e);
            return List.of();
        }
        indexes.sort(Integer::compareTo);
        return indexes;
    }

    /**
     * 按序合并分片为正式文件，合并成功后删除临时目录。
     *
     * @param uploadId 上传批次 ID
     * @param totalChunks 分片总数
     * @param ext 扩展名（可空，空则生成无后缀文件）
     * @return 相对存储 key（正斜杠），形如 {@code doc/202611/123456.docx}
     */
    public String merge(String uploadId, int totalChunks, String ext) {
        if (totalChunks <= 0) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "分片总数必须大于 0");
        }
        List<Integer> uploaded = listUploadedChunks(uploadId);
        List<Integer> missing = new ArrayList<>();
        for (int i = 0; i < totalChunks; i++) {
            if (!uploaded.contains(i)) {
                missing.add(i);
            }
        }
        if (!missing.isEmpty()) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "分片缺失，请重新上传分片序号：" + missing);
        }

        String month = LocalDate.now().format(MONTH_FORMATTER);
        String fileName = String.valueOf(IdWorker.getId());
        if (StringUtils.isNotBlank(ext)) {
            fileName = fileName + "." + ext.toLowerCase();
        }
        String storageKey = "doc/" + month + "/" + fileName;
        Path target = resolve(storageKey);
        try {
            Files.createDirectories(target.getParent());
            try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(target), BUFFER_SIZE)) {
                for (int i = 0; i < totalChunks; i++) {
                    Path part = partPath(uploadId, i);
                    try (InputStream in = Files.newInputStream(part)) {
                        in.transferTo(out);
                    }
                }
            }
        } catch (IOException e) {
            log.error("分片合并失败：uploadId={}，totalChunks={}", uploadId, totalChunks, e);
            throw new BusinessException(ErrorCode.BIZ_UNPROCESSABLE, "分片合并失败，请重试", e);
        } finally {
            deleteChunkDir(uploadId);
        }
        log.info("分片合并完成：uploadId={}，totalChunks={}，key={}", uploadId, totalChunks, storageKey);
        return storageKey;
    }

    /** 直接写入字节内容（用于版本回滚复制），返回相对存储 key。 */
    public String write(byte[] content, String ext) {
        if (content == null) {
            throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "写入内容不能为空");
        }
        String month = LocalDate.now().format(MONTH_FORMATTER);
        String fileName = String.valueOf(IdWorker.getId());
        if (StringUtils.isNotBlank(ext)) {
            fileName = fileName + "." + ext.toLowerCase();
        }
        String storageKey = "doc/" + month + "/" + fileName;
        Path target = resolve(storageKey);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            log.error("文档内容写入失败：key={}", storageKey, e);
            throw new BusinessException(ErrorCode.BIZ_UNPROCESSABLE, "文档内容写入失败", e);
        }
        return storageKey;
    }

    /**
     * 解析相对 key 为绝对路径。
     *
     * <p>规范化后必须仍在根目录下，否则视为越界并抛 400；绝对路径同样拒绝。
     */
    public Path resolve(String storageKey) {
        if (StringUtils.isBlank(storageKey)) {
            throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "存储键不能为空");
        }
        Path relative = Path.of(storageKey);
        if (relative.isAbsolute()) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "非法存储键：" + storageKey);
        }
        Path rootPath = root();
        Path target = rootPath.resolve(relative).normalize();
        if (!target.startsWith(rootPath)) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "非法存储键：" + storageKey);
        }
        return target;
    }

    /** 计算文件 SHA-256（小写 hex）；文件不存在抛 404。 */
    public String sha256(Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "待校验文件不存在");
        }
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException e) {
            log.error("计算文件 SHA-256 失败：{}", file, e);
            throw new BusinessException(ErrorCode.BIZ_UNPROCESSABLE, "计算文件校验和失败", e);
        } catch (NoSuchAlgorithmException e) {
            // JDK 必带 SHA-256，正常不会发生
            throw new BusinessException(ErrorCode.SYSTEM_INTERNAL_ERROR, "SHA-256 算法不可用", e);
        }
    }

    /** 静默删除正式文件（不存在或非法 key 仅记日志，不抛异常）。 */
    public void deleteQuietly(String storageKey) {
        if (StringUtils.isBlank(storageKey)) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (BusinessException e) {
            log.warn("拒绝删除非法存储键：{}", storageKey);
        } catch (IOException e) {
            log.warn("删除物理文件失败：{}", storageKey, e);
        }
    }

    /** 物理文件引用计数：未删除且 storage_key 相同的 doc_file 条数（供 P6 删除策略使用）。 */
    public long countReferences(String storageKey) {
        if (StringUtils.isBlank(storageKey)) {
            return 0L;
        }
        Long count =
                docFileMapper.selectCount(
                        new LambdaQueryWrapper<DocFile>()
                                .eq(DocFile::getStorageKey, storageKey));
        return count == null ? 0L : count;
    }

    /** 删除分片临时目录（递归，失败仅记日志）。 */
    private void deleteChunkDir(String uploadId) {
        Path dir;
        try {
            dir = chunkDir(uploadId);
        } catch (BusinessException e) {
            return;
        }
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted((left, right) -> right.getNameCount() - left.getNameCount())
                    .forEach(
                            path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException e) {
                                    log.warn("删除分片临时文件失败：{}", path, e);
                                }
                            });
        } catch (IOException e) {
            log.warn("清理分片临时目录失败：{}", dir, e);
        }
    }

    private String requireUploadId(String uploadId) {
        if (StringUtils.isBlank(uploadId)) {
            throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "上传批次 ID 不能为空");
        }
        for (int i = 0; i < uploadId.length(); i++) {
            char ch = uploadId.charAt(i);
            boolean allowed =
                    (ch >= '0' && ch <= '9')
                            || (ch >= 'a' && ch <= 'z')
                            || (ch >= 'A' && ch <= 'Z')
                            || ch == '-'
                            || ch == '_';
            if (!allowed) {
                throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "非法的上传批次 ID");
            }
        }
        return uploadId;
    }
}
