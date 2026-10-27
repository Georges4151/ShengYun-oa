package com.ceams.module.project.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.project.dto.progresspayment.ProgressPaymentCreateRequest;
import com.ceams.module.project.dto.progresspayment.ProgressPaymentUpdateRequest;
import com.ceams.module.project.enums.ApprovalStatus;
import com.ceams.module.project.service.ProgressPaymentService;
import com.ceams.module.project.vo.IdResponse;
import com.ceams.module.project.vo.ProgressPaymentVO;
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

/**
 * 进度款管理接口。
 */
@RestController
@RequestMapping("/api/v1/progress-payments")
@RequiredArgsConstructor
public class ProgressPaymentController {

    private final ProgressPaymentService progressPaymentService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<ProgressPaymentVO>> page(@RequestParam(defaultValue = "1") long page,
                                                           @RequestParam(defaultValue = "20") long size,
                                                           @RequestParam(required = false) Long projectId,
                                                           @RequestParam(required = false) ApprovalStatus status) {
        return ApiResponse.success(progressPaymentService.page(page, size, projectId, status));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<IdResponse> create(@Valid @RequestBody ProgressPaymentCreateRequest request) {
        return ApiResponse.success(progressPaymentService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ProgressPaymentVO> detail(@PathVariable Long id) {
        return ApiResponse.success(progressPaymentService.detail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody ProgressPaymentUpdateRequest request) {
        progressPaymentService.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        progressPaymentService.delete(id);
        return ApiResponse.success();
    }
}