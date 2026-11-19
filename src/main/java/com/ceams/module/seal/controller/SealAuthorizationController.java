package com.ceams.module.seal.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.module.seal.dto.SealAuthorizationCreateRequest;
import com.ceams.module.seal.service.SealAuthorizationService;
import com.ceams.module.seal.vo.SealAuthorizationVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 印章授权管理接口。 */
@RestController
@RequestMapping("/api/v1/seal/authorizations")
@RequiredArgsConstructor
public class SealAuthorizationController {

    private final SealAuthorizationService sealAuthorizationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<List<SealAuthorizationVO>> list(@RequestParam Long sealId) {
        return ApiResponse.success(sealAuthorizationService.listBySeal(sealId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<String> create(@Valid @RequestBody SealAuthorizationCreateRequest request) {
        return ApiResponse.success(sealAuthorizationService.create(request));
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> revoke(@PathVariable Long id) {
        sealAuthorizationService.revoke(id);
        return ApiResponse.success();
    }
}
