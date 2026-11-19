package com.ceams.module.file.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.file.service.FileService;
import com.ceams.module.file.vo.DirectoryStatVO;
import com.ceams.module.file.vo.FileStorageInfoVO;
import com.ceams.module.file.vo.FileVO;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 轻量文件上传/下载服务实现。
 *
 * <p>参考 {@code SealStampServiceImpl} 的「本地落盘 + 相对路径返回」既定做法： 目录通过 {@code
 * ceams.file.upload-dir} 注入，文件名使用雪花 ID + 白名单扩展名，规避原始文件名带来的路径穿越。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    /** 上传大小上限：20MB（与 {@code spring.servlet.multipart.max-file-size} 对齐）。 */
    private static final long MAX_SIZE = 20L * 1024 * 1024;

    /** 允许的扩展名白名单（取原文件名后缀转小写比对）。 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "pdf");

    /** 服务端文件名格式：雪花 ID + 白名单扩展名，防路径穿越。 */
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+\\.[a-zA-Z0-9]{2,5}$");

    /** 根目录直接文件在存储统计中的展示名称。 */
    private static final String ROOT_FILES_LABEL = "业务附件";

    @Value("${ceams.file.upload-dir:./data/files}")
    private String uploadDir;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public FileVO upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "上传文件不能为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "文件大小不能超过 20MB");
        }
        String extension = resolveExtension(file.getOriginalFilename());
        String name = IdWorker.getId() + "." + extension;

        Path target = null;
        try {
            Path dir = Path.of(uploadDir);
            Files.createDirectories(dir);
            target = dir.resolve(name);
            file.transferTo(target);
        } catch (IOException e) {
            cleanupQuietly(target);
            log.error("文件落盘失败", e);
            throw new BusinessException(ErrorCode.BIZ_UNPROCESSABLE, "文件落盘失败", e);
        } catch (RuntimeException e) {
            cleanupQuietly(target);
            log.error("文件落盘失败", e);
            throw e;
        }

        FileVO vo = new FileVO();
        vo.setName(name);
        vo.setUrl("/api/v1/files/" + name);
        vo.setSize(file.getSize());
        vo.setContentType(file.getContentType());
        return vo;
    }

    @Override
    public byte[] load(String name) {
        if (name == null || !NAME_PATTERN.matcher(name).matches()) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "非法文件名");
        }
        Path target = Path.of(uploadDir).resolve(name);
        if (!Files.isRegularFile(target)) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "文件不存在");
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            log.error("读取文件失败", e);
            throw new BusinessException(ErrorCode.BIZ_UNPROCESSABLE, "读取文件失败", e);
        }
    }

    @Override
    public byte[] readBytes(String name) {
        // 与下载共用同一套文件名校验与读取逻辑，避免两处规则漂移。
        return load(name);
    }

    @Override
    public void delete(String name) {
        if (name == null || !NAME_PATTERN.matcher(name).matches()) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "非法文件名");
        }
        Integer referenced =
                jdbcTemplate.queryForObject(
                        "SELECT (SELECT COUNT(1) FROM biz_subcontractor_contract WHERE file_path = ? AND deleted = 0)"
                                + " + (SELECT COUNT(1) FROM biz_settlement_proof WHERE file_path = ? AND deleted = 0)",
                        Integer.class,
                        name,
                        name);
        if (referenced != null && referenced > 0) {
            throw BusinessException.of(ErrorCode.BIZ_UNPROCESSABLE, "文件已被业务数据引用，不能删除");
        }
        try {
            Files.deleteIfExists(Path.of(uploadDir).resolve(name));
        } catch (IOException e) {
            log.error("删除文件失败", e);
            throw new BusinessException(ErrorCode.BIZ_UNPROCESSABLE, "删除文件失败", e);
        }
    }

    @Override
    public FileStorageInfoVO storageInfo() {
        FileStorageInfoVO vo = new FileStorageInfoVO();
        vo.setConfiguredPath(uploadDir);

        Path root;
        try {
            root = Path.of(uploadDir).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            // 配置值本身非法（如含当前系统不允许的字符）：不抛异常，返回空统计，避免接口 500。
            log.warn("上传目录配置非法，无法解析: {}", uploadDir, e);
            return vo;
        }
        vo.setAbsolutePath(root.toString());

        boolean exists = Files.isDirectory(root);
        vo.setExists(exists);
        vo.setWritable(exists && Files.isWritable(root));
        if (!exists) {
            // 目录尚未创建：其余统计保持默认 0 / 空列表，磁盘信息按「获取失败」处理为 null。
            fillDiskSpace(root, vo);
            return vo;
        }

        StorageTotals totals = new StorageTotals();
        accumulate(totals, root);
        vo.setFileCount(totals.fileCount);
        vo.setTotalSizeBytes(totals.sizeBytes);
        vo.setDirectories(collectDirectories(root));
        fillDiskSpace(root, vo);
        return vo;
    }

    /**
     * 统计根目录直接文件与各一级子目录的占用。
     *
     * <p>只枚举一层：根目录直接文件归入「业务附件」，每个一级子目录单独递归统计， 避免子目录内容被重复计入「业务附件」。
     */
    private List<DirectoryStatVO> collectDirectories(Path root) {
        List<DirectoryStatVO> directories = new ArrayList<>();

        StorageTotals rootFiles = new StorageTotals();
        try (Stream<Path> children = Files.list(root)) {
            children.filter(path -> !Files.isSymbolicLink(path))
                    .filter(Files::isRegularFile)
                    .forEach(path -> addFileQuietly(rootFiles, path));
        } catch (IOException | UncheckedIOException e) {
            // 目录不可读等情况：降级为已统计部分，不让接口失败。
            log.warn("统计根目录直接文件失败，返回已统计部分: {}", root, e);
        }
        directories.add(
                new DirectoryStatVO(ROOT_FILES_LABEL, rootFiles.fileCount, rootFiles.sizeBytes));

        try (Stream<Path> children = Files.list(root)) {
            List<Path> subDirectories =
                    children.filter(path -> !Files.isSymbolicLink(path))
                            .filter(Files::isDirectory)
                            .sorted()
                            .toList();
            for (Path subDirectory : subDirectories) {
                StorageTotals totals = new StorageTotals();
                accumulate(totals, subDirectory);
                directories.add(
                        new DirectoryStatVO(
                                subDirectory.getFileName().toString(),
                                totals.fileCount,
                                totals.sizeBytes));
            }
        } catch (IOException | UncheckedIOException e) {
            log.warn("统计一级子目录失败，返回已统计部分: {}", root, e);
        }

        // 按占用字节数倒序；占用相同时按名称稳定排序，保证输出可预期。
        directories.sort(
                Comparator.comparingLong(DirectoryStatVO::getSizeBytes)
                        .reversed()
                        .thenComparing(DirectoryStatVO::getName));
        return directories;
    }

    /** 递归统计目录下的常规文件数量与字节数；忽略符号链接与不可读文件，异常时保留已统计部分。 */
    private void accumulate(StorageTotals totals, Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.filter(path -> !Files.isSymbolicLink(path))
                    .filter(Files::isRegularFile)
                    .forEach(path -> addFileQuietly(totals, path));
        } catch (IOException | UncheckedIOException e) {
            log.warn("遍历目录失败，返回已统计部分: {}", directory, e);
        }
    }

    /** 累加单个文件的大小；读取失败（权限不足、文件被占用等）时跳过该文件并记录告警。 */
    private void addFileQuietly(StorageTotals totals, Path file) {
        try {
            totals.sizeBytes += Files.size(file);
            totals.fileCount++;
        } catch (IOException e) {
            log.warn("读取文件大小失败，已跳过: {}", file, e);
        }
    }

    /** 读取目录所在分区的可用/总空间；目录不存在或读取失败时保持 null，不影响其他字段。 */
    private void fillDiskSpace(Path root, FileStorageInfoVO vo) {
        try {
            FileStore fileStore = Files.getFileStore(root);
            vo.setDiskUsableBytes(fileStore.getUsableSpace());
            vo.setDiskTotalBytes(fileStore.getTotalSpace());
        } catch (IOException | SecurityException e) {
            log.warn("获取磁盘空间信息失败，置为空: {}", root, e);
        }
    }

    /** 尽力删除上传中途残留的半截文件，清理失败只记录告警，不掩盖原始异常。 */
    private void cleanupQuietly(Path target) {
        if (target == null) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException cleanupError) {
            log.warn("上传失败后清理半截文件失败: {}", target, cleanupError);
        }
    }

    private String resolveExtension(String originalFilename) {
        if (originalFilename == null) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "文件类型不支持");
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "文件类型不支持");
        }
        String extension = originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "仅支持 jpg/jpeg/png/webp/pdf 格式");
        }
        return extension;
    }

    /** 目录统计累加器：遍历过程使用可变计数，异常中断时仍能保留已统计部分。 */
    private static final class StorageTotals {
        private long fileCount;
        private long sizeBytes;
    }
}