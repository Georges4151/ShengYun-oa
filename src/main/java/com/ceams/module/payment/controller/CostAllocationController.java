package com.ceams.module.payment.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.payment.dto.CostAllocationCreateRequest;
import com.ceams.module.payment.dto.CostAllocationUpdateRequest;
import com.ceams.module.payment.service.CostAllocationService;
import com.ceams.module.payment.vo.CostAllocationVO;
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

/** 成本分摊管理接口。 */
@RestController
@RequestMapping("/api/v1/cost-allocations")
@RequiredArgsConstructor
public class CostAllocationController {

    private final CostAllocationService costAllocationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<CostAllocationVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long accountRecordId) {
        return ApiResponse.success(costAllocationService.page(page, size, accountRecordId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<String> create(@Valid @RequestBody CostAllocationCreateRequest request) {
        return ApiResponse.success(costAllocationService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CostAllocationVO> detail(@PathVariable Long id) {
        return ApiResponse.success(costAllocationService.detail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> update(
            @PathVariable Long id, @Valid @RequestBody CostAllocationUpdateRequest request) {
        costAllocationService.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        costAllocationService.delete(id);
        return ApiResponse.success();
    }
}
