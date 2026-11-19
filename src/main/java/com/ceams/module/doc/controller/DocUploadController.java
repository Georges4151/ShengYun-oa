package com.ceams.module.doc.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.module.doc.dto.UploadInitRequest;
import com.ceams.module.doc.service.DocUploadService;
import com.ceams.module.doc.vo.DocFileVO;
import com.ceams.module.doc.vo.UploadInitVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档分片上传接口（统一前缀 /api/v1/doc/files/upload）。
 *
 * <p>初始化时服务层校验目录 EDIT 权、扩展名白名单、大小与分片数上限，并尝试 SHA-256 秒传；
 * 分片逐片上传（支持断点续传），合并时校验 SHA-256 后落库并生成 v1 版本。
 */
@RestController
@RequestMapping("/api/v1/doc/files/upload")
@RequiredArgsConstructor
public class DocUploadController {

    private final DocUploadService uploadService;

    /** 初始化上传（返回 uploadId / 分片大小 / 已上传分片 / 是否秒传）。 */
    @PostMapping("/init")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UploadInitVO> init(@Valid @RequestBody UploadInitRequest request) {
        return ApiResponse.success(uploadService.init(request));
    }

    /** 上传单个分片（multipart，字段名 file）。 */
    @PutMapping("/chunk")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Integer>> chunk(
            @RequestParam String uploadId,
            @RequestParam int index,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(uploadService.saveChunk(uploadId, index, file));
    }

    /** 合并分片并落库。 */
    @PostMapping("/complete")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<DocFileVO> complete(@RequestParam String uploadId) {
        return ApiResponse.success(uploadService.complete(uploadId));
    }
}
