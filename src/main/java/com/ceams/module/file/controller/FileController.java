package com.ceams.module.file.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.module.file.service.FileService;
import com.ceams.module.file.vo.FileStorageInfoVO;
import com.ceams.module.file.vo.FileVO;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 轻量文件上传/下载接口（file 模块，票据拍照上传 + 预览）。 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<FileVO> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(fileService.upload(file));
    }

    /**
     * 文件存储目录只读信息（路径、占用统计、所在分区空间）。
     *
     * <p>仅系统管理员可查看；路径改动需调整 FILE_UPLOAD_DIR（{@code ceams.file.upload-dir}）或
     * 容器卷挂载后重启服务生效，本接口不修改任何配置与文件。
     *
     * <p>必须声明在 {@code /{name}} 之前，避免被路径变量匹配。
     */
    @GetMapping("/storage-info")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ApiResponse<FileStorageInfoVO> storageInfo() {
        return ApiResponse.success(fileService.storageInfo());
    }

    @GetMapping("/{name}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> download(@PathVariable String name) {
        byte[] bytes = fileService.load(name);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(resolveMediaType(name));
        headers.setContentDisposition(ContentDisposition.inline().filename(name).build());
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @DeleteMapping("/{name}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> delete(@PathVariable String name) {
        fileService.delete(name);
        return ApiResponse.success();
    }

    private MediaType resolveMediaType(String name) {
        int dot = name.lastIndexOf('.');
        String extension = dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        return switch (extension) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "pdf" -> MediaType.APPLICATION_PDF;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}