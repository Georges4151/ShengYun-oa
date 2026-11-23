package com.ceams.module.bpm.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.common.sign.HmacSigned;
import com.ceams.module.bpm.dto.FlowPreviewRequest;
import com.ceams.module.bpm.dto.InstanceActionRequest;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.bpm.vo.BizDetailVO;
import com.ceams.module.bpm.vo.FlowPreviewVO;
import com.ceams.module.bpm.vo.InstanceDetailVO;
import com.ceams.module.bpm.vo.InstanceVO;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 流程实例接口（/api/v1/bpm/instances）。 */
@RestController
@RequestMapping("/api/v1/bpm/instances")
@RequiredArgsConstructor
public class BpmInstanceController {

    private final BpmInstanceService instanceService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> start(@Valid @RequestBody InstanceStartRequest request) {
        return ApiResponse.success(instanceService.start(request));
    }

    @PostMapping("/preview")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<FlowPreviewVO> preview(@Valid @RequestBody FlowPreviewRequest request) {
        return ApiResponse.success(instanceService.preview(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<InstanceDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(instanceService.detail(id));
    }

    @GetMapping("/{id}/biz-detail")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<BizDetailVO> bizDetail(@PathVariable Long id) {
        return ApiResponse.success(instanceService.bizDetail(id));
    }

    /**
     * 审批附件受控下载：沿用实例可见性校验，只读取文件模块已存在的存储名，不做任何目录拼接。
     *
     * <p>与通用 {@code GET /api/v1/files/{name}} 并存，原有下载行为保持不变。
     */
    @GetMapping("/{id}/files/{name}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> attachment(
            @PathVariable Long id, @PathVariable String name) {
        byte[] bytes = instanceService.loadAttachment(id, name);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(resolveAttachmentMediaType(name));
        headers.setContentDisposition(
                ContentDisposition.inline().filename(name, StandardCharsets.UTF_8).build());
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<InstanceVO>> mine(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) BpmInstanceStatus status,
            @RequestParam(required = false) String bizType) {
        return ApiResponse.success(instanceService.pageByInitiator(page, size, status, bizType));
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> withdraw(
            @PathVariable Long id, @Valid @RequestBody InstanceActionRequest request) {
        instanceService.withdraw(id, request);
        return ApiResponse.success();
    }

    @HmacSigned
    @PostMapping("/{id}/terminate")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ApiResponse<Void> terminate(
            @PathVariable Long id, @Valid @RequestBody InstanceActionRequest request) {
        instanceService.terminate(id, request);
        return ApiResponse.success();
    }

    /** 按扩展名解析附件类型，未知类型统一按二进制流返回。 */
    private MediaType resolveAttachmentMediaType(String name) {
        int dot = name.lastIndexOf('.');
        String extension = dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        return switch (extension) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "doc" -> MediaType.parseMediaType("application/msword");
            case "docx" ->
                    MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case "xls" -> MediaType.parseMediaType("application/vnd.ms-excel");
            case "xlsx" ->
                    MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
