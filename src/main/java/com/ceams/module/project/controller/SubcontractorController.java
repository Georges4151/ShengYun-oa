package com.ceams.module.project.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.project.dto.subcontractor.SubcontractorCreateRequest;
import com.ceams.module.project.dto.subcontractor.SubcontractorUpdateRequest;
import com.ceams.module.project.service.SubcontractorService;
import com.ceams.module.project.vo.SubcontractorVO;
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

/** 分包商管理接口。 */
@RestController
@RequestMapping("/api/v1/subcontractors")
@RequiredArgsConstructor
public class SubcontractorController {

    private final SubcontractorService subcontractorService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<SubcontractorVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(subcontractorService.page(page, size, keyword));
    }

    @PostMapping
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ApiResponse<String> create(@Valid @RequestBody SubcontractorCreateRequest request) {
        return ApiResponse.success(subcontractorService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<SubcontractorVO> detail(@PathVariable Long id) {
        return ApiResponse.success(subcontractorService.detail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ApiResponse<Void> update(
            @PathVariable Long id, @Valid @RequestBody SubcontractorUpdateRequest request) {
        subcontractorService.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        subcontractorService.delete(id);
        return ApiResponse.success();
    }
}
