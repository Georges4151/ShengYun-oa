package com.ceams.module.seal.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.sign.HmacSigned;
import com.ceams.module.seal.dto.SealStampRequest;
import com.ceams.module.seal.service.SealStampService;
import com.ceams.module.seal.vo.SealStampResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** PDF 盖章接口（HMAC 高危接口）。 */
@RestController
@RequestMapping("/api/v1/seals")
@RequiredArgsConstructor
public class SealStampController {

    private final SealStampService sealStampService;

    @HmacSigned
    @PostMapping("/stamp")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<SealStampResultVO> stamp(@Valid @RequestBody SealStampRequest request) {
        return ApiResponse.success(sealStampService.stamp(request));
    }
}
