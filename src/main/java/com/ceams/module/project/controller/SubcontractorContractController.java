package com.ceams.module.project.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.project.dto.subcontractorcontract.SubcontractorContractCreateRequest;
import com.ceams.module.project.dto.subcontractorcontract.SubcontractorContractUpdateRequest;
import com.ceams.module.project.service.SubcontractorContractService;
import com.ceams.module.project.vo.SubcontractorContractVO;
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

/** 分包合同管理接口。 */
@RestController
@RequestMapping("/api/v1/subcontractor-contracts")
@RequiredArgsConstructor
public class SubcontractorContractController {

    private final SubcontractorContractService subcontractorContractService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<SubcontractorContractVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long subcontractorId,
            @RequestParam(required = false) Long projectId) {
        return ApiResponse.success(
                subcontractorContractService.page(page, size, subcontractorId, projectId));
    }

    @PostMapping
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ApiResponse<String> create(
            @Valid @RequestBody SubcontractorContractCreateRequest request) {
        return ApiResponse.success(subcontractorContractService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<SubcontractorContractVO> detail(@PathVariable Long id) {
        return ApiResponse.success(subcontractorContractService.detail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ApiResponse<Void> update(
            @PathVariable Long id, @Valid @RequestBody SubcontractorContractUpdateRequest request) {
        subcontractorContractService.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        subcontractorContractService.delete(id);
        return ApiResponse.success();
    }
}
