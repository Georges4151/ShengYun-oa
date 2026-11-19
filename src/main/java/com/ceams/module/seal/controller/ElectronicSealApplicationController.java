package com.ceams.module.seal.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.seal.dto.ElectronicSealApplicationCreateRequest;
import com.ceams.module.seal.dto.ElectronicSealApplicationUpdateRequest;
import com.ceams.module.seal.enums.ApplicationStatus;
import com.ceams.module.seal.service.ElectronicSealApplicationService;
import com.ceams.module.seal.vo.ElectronicSealApplicationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 用印申请管理接口。 */
@RestController
@RequestMapping("/api/v1/seal/applications")
@RequiredArgsConstructor
public class ElectronicSealApplicationController {

    private final ElectronicSealApplicationService electronicSealApplicationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<ElectronicSealApplicationVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long sealId,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) Long applicantId) {
        return ApiResponse.success(
                electronicSealApplicationService.page(page, size, sealId, status, applicantId));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> create(
            @Valid @RequestBody ElectronicSealApplicationCreateRequest request) {
        return ApiResponse.success(electronicSealApplicationService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ElectronicSealApplicationVO> detail(@PathVariable Long id) {
        return ApiResponse.success(electronicSealApplicationService.detail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> update(
            @PathVariable Long id,
            @Valid @RequestBody ElectronicSealApplicationUpdateRequest request) {
        electronicSealApplicationService.update(id, request);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        electronicSealApplicationService.cancel(id);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        electronicSealApplicationService.delete(id);
        return ApiResponse.success();
    }
}
