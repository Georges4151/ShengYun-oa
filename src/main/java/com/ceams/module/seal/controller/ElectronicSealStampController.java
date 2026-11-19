package com.ceams.module.seal.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.seal.service.ElectronicSealStampService;
import com.ceams.module.seal.vo.DownloadUrlVO;
import com.ceams.module.seal.vo.ElectronicSealStampVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 用印盖章记录管理接口。 */
@RestController
@RequestMapping("/api/v1/seal/stamps")
@RequiredArgsConstructor
public class ElectronicSealStampController {

    private final ElectronicSealStampService electronicSealStampService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<ElectronicSealStampVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long sealId,
            @RequestParam(required = false) Long projectId) {
        return ApiResponse.success(electronicSealStampService.page(page, size, sealId, projectId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ElectronicSealStampVO> detail(@PathVariable Long id) {
        return ApiResponse.success(electronicSealStampService.detail(id));
    }

    @GetMapping("/{id}/download-url")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<DownloadUrlVO> downloadUrl(@PathVariable Long id) {
        return ApiResponse.success(electronicSealStampService.downloadUrl(id));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> download(
            @PathVariable Long id,
            @RequestParam(value = "expires", required = false) Long expires,
            @RequestParam(value = "sign", required = false) String sign,
            HttpServletRequest request) {
        byte[] bytes = electronicSealStampService.download(id, expires, sign, request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", id + ".pdf");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
}
