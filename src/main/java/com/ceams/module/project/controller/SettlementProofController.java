package com.ceams.module.project.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.project.dto.settlementproof.SettlementProofCreateRequest;
import com.ceams.module.project.dto.settlementproof.SettlementProofUpdateRequest;
import com.ceams.module.project.service.SettlementProofService;
import com.ceams.module.project.vo.SettlementProofVO;
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

/** 结算凭证管理接口。 */
@RestController
@RequestMapping("/api/v1/settlement-proofs")
@RequiredArgsConstructor
public class SettlementProofController {

    private final SettlementProofService settlementProofService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<SettlementProofVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long contractId) {
        return ApiResponse.success(settlementProofService.page(page, size, contractId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<String> create(@Valid @RequestBody SettlementProofCreateRequest request) {
        return ApiResponse.success(settlementProofService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<SettlementProofVO> detail(@PathVariable Long id) {
        return ApiResponse.success(settlementProofService.detail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> update(
            @PathVariable Long id, @Valid @RequestBody SettlementProofUpdateRequest request) {
        settlementProofService.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        settlementProofService.delete(id);
        return ApiResponse.success();
    }
}
