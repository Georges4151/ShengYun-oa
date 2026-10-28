package com.ceams.module.payment.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.payment.dto.PaymentTransactionCreateRequest;
import com.ceams.module.payment.dto.PaymentTransactionUpdateRequest;
import com.ceams.module.payment.enums.AccountType;
import com.ceams.module.payment.service.PaymentTransactionService;
import com.ceams.module.payment.vo.PaymentTransactionVO;
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
 * 付款交易管理接口。
 */
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class PaymentTransactionController {

    private final PaymentTransactionService paymentTransactionService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<PaymentTransactionVO>> page(@RequestParam(defaultValue = "1") long page,
                                                              @RequestParam(defaultValue = "20") long size,
                                                              @RequestParam(required = false) Long accountId,
                                                              @RequestParam(required = false) AccountType type) {
        return ApiResponse.success(paymentTransactionService.page(page, size, accountId, type));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<String> create(@Valid @RequestBody PaymentTransactionCreateRequest request) {
        return ApiResponse.success(paymentTransactionService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PaymentTransactionVO> detail(@PathVariable Long id) {
        return ApiResponse.success(paymentTransactionService.detail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @Valid @RequestBody PaymentTransactionUpdateRequest request) {
        paymentTransactionService.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        paymentTransactionService.delete(id);
        return ApiResponse.success();
    }
}
