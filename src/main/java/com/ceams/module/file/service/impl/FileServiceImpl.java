package com.ceams.module.file.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.file.service.FileService;
import com.ceams.module.file.vo.FileVO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
public class FileServiceImpl implements FileService {

    /** 上传大小上限：20MB（与 {@code spring.servlet.multipart.max-file-size} 对齐）。 */
    private static final long MAX_SIZE = 20L * 1024 * 1024;

    /** 允许的扩展名白名单（取原文件名后缀转小写比对）。 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "pdf");

    /** 服务端文件名格式：雪花 ID + 白名单扩展名，防路径穿越。 */
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+\\.[a-zA-Z0-9]{2,5}$");

    @Value("${ceams.file.upload-dir:./data/files}")
    private String uploadDir;

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

        try {
            Path dir = Path.of(uploadDir);
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(name));
        } catch (IOException e) {
            log.error("文件落盘失败", e);
            throw new BusinessException(ErrorCode.BIZ_UNPROCESSABLE, "文件落盘失败", e);
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
}