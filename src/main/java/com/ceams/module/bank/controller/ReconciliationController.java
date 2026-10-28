package com.ceams.module.bank.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.bank.dto.ReconciliationMatchRequest;
import com.ceams.module.bank.dto.ReconciliationResolveRequest;
import com.ceams.module.bank.enums.ReconciliationStatus;
import com.ceams.module.bank.enums.ResolutionStatus;
import com.ceams.module.bank.service.ReconciliationService;
import com.ceams.module.bank.vo.ReconciliationMatchResultVO;
import com.ceams.module.bank.vo.ReconciliationResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对账结果管理接口。
 */
@RestController
@RequestMapping("/api/v1/bank/reconciliations")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<ReconciliationResultVO>> page(@RequestParam(defaultValue = "1") long page,
                                                                @RequestParam(defaultValue = "20") long size,
                                                                @RequestParam(required = false) Long accountId,
                                                                @RequestParam(required = false) ReconciliationStatus status,
                                                                @RequestParam(required = false) ResolutionStatus resolutionStatus) {
        return ApiResponse.success(reconciliationService.page(page, size, accountId, status, resolutionStatus));
    }

    @PostMapping("/match")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<ReconciliationMatchResultVO> match(@Valid @RequestBody ReconciliationMatchRequest request) {
        return ApiResponse.success(reconciliationService.match(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ReconciliationResultVO> detail(@PathVariable Long id) {
        return ApiResponse.success(reconciliationService.detail(id));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> resolve(@PathVariable Long id, @Valid @RequestBody ReconciliationResolveRequest request) {
        reconciliationService.resolve(id, request);
        return ApiResponse.success();
    }
}
