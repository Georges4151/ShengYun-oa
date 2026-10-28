package com.ceams.module.project.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.project.dto.changerequest.ApprovalActionRequest;
import com.ceams.module.project.dto.changerequest.ChangeRequestCreateRequest;
import com.ceams.module.project.dto.changerequest.ChangeRequestUpdateRequest;
import com.ceams.module.project.enums.ApprovalStatus;
import com.ceams.module.project.service.ChangeRequestService;
import com.ceams.module.project.vo.ChangeRequestVO;
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
 * 变更请求管理接口。
 */
@RestController
@RequestMapping("/api/v1/change-requests")
@RequiredArgsConstructor
public class ChangeRequestController {

    private final ChangeRequestService changeRequestService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<ChangeRequestVO>> page(@RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "20") long size,
                                                         @RequestParam(required = false) Long projectId,
                                                         @RequestParam(required = false) ApprovalStatus status) {
        return ApiResponse.success(changeRequestService.page(page, size, projectId, status));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<String> create(@Valid @RequestBody ChangeRequestCreateRequest request) {
        return ApiResponse.success(changeRequestService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ChangeRequestVO> detail(@PathVariable Long id) {
        return ApiResponse.success(changeRequestService.detail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody ChangeRequestUpdateRequest request) {
        changeRequestService.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        changeRequestService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> approve(@PathVariable Long id,
                                     @RequestBody(required = false) ApprovalActionRequest request) {
        changeRequestService.approve(id, request);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> reject(@PathVariable Long id,
                                    @RequestBody(required = false) ApprovalActionRequest request) {
        changeRequestService.reject(id, request);
        return ApiResponse.success();
    }
}