package com.ceams.module.seal.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.seal.dto.StampApprovalApproveRequest;
import com.ceams.module.seal.dto.StampApprovalCreateRequest;
import com.ceams.module.seal.dto.StampApprovalRejectRequest;
import com.ceams.module.seal.dto.StampApprovalUpdateRequest;
import com.ceams.module.seal.enums.ApprovalStatus;
import com.ceams.module.seal.service.StampApprovalService;
import com.ceams.module.seal.vo.StampApprovalVO;
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

/** 用印审批管理接口。 */
@RestController
@RequestMapping("/api/v1/seal/approvals")
@RequiredArgsConstructor
public class StampApprovalController {

    private final StampApprovalService stampApprovalService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<StampApprovalVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long stampId,
            @RequestParam(required = false) Long sealId,
            @RequestParam(required = false) ApprovalStatus status) {
        return ApiResponse.success(stampApprovalService.page(page, size, stampId, sealId, status));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<String> create(@Valid @RequestBody StampApprovalCreateRequest request) {
        return ApiResponse.success(stampApprovalService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<StampApprovalVO> detail(@PathVariable Long id) {
        return ApiResponse.success(stampApprovalService.detail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> update(
            @PathVariable Long id, @Valid @RequestBody StampApprovalUpdateRequest request) {
        stampApprovalService.update(id, request);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> approve(
            @PathVariable Long id, @Valid @RequestBody StampApprovalApproveRequest request) {
        stampApprovalService.approve(id, request);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> reject(
            @PathVariable Long id, @Valid @RequestBody StampApprovalRejectRequest request) {
        stampApprovalService.reject(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        stampApprovalService.delete(id);
        return ApiResponse.success();
    }
}
