package com.ceams.module.bank.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.bank.dto.GeneralLedgerCreateRequest;
import com.ceams.module.bank.dto.GeneralLedgerUpdateRequest;
import com.ceams.module.bank.enums.TransactionType;
import com.ceams.module.bank.service.GeneralLedgerService;
import com.ceams.module.bank.vo.GeneralLedgerVO;
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

/** 总账管理接口。 */
@RestController
@RequestMapping("/api/v1/bank/ledgers")
@RequiredArgsConstructor
public class GeneralLedgerController {

    private final GeneralLedgerService generalLedgerService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<GeneralLedgerVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String accountName,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(
                generalLedgerService.page(page, size, accountName, transactionType, status));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<String> create(@Valid @RequestBody GeneralLedgerCreateRequest request) {
        return ApiResponse.success(generalLedgerService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<GeneralLedgerVO> detail(@PathVariable Long id) {
        return ApiResponse.success(generalLedgerService.detail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> update(
            @PathVariable Long id, @Valid @RequestBody GeneralLedgerUpdateRequest request) {
        generalLedgerService.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        generalLedgerService.delete(id);
        return ApiResponse.success();
    }
}
