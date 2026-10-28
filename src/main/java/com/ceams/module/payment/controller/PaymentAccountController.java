package com.ceams.module.payment.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.payment.dto.PaymentAccountCreateRequest;
import com.ceams.module.payment.dto.PaymentAccountUpdateRequest;
import com.ceams.module.payment.service.PaymentAccountService;
import com.ceams.module.payment.vo.PaymentAccountVO;
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
 * 付款账户管理接口。
 */
@RestController
@RequestMapping("/api/v1/payment-accounts")
@RequiredArgsConstructor
public class PaymentAccountController {

    private final PaymentAccountService paymentAccountService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<PaymentAccountVO>> page(@RequestParam(defaultValue = "1") long page,
                                                          @RequestParam(defaultValue = "20") long size,
                                                          @RequestParam(required = false) String name,
                                                          @RequestParam(required = false) String type) {
        return ApiResponse.success(paymentAccountService.page(page, size, name, type));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<String> create(@Valid @RequestBody PaymentAccountCreateRequest request) {
        return ApiResponse.success(paymentAccountService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PaymentAccountVO> detail(@PathVariable Long id) {
        return ApiResponse.success(paymentAccountService.detail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody PaymentAccountUpdateRequest request) {
        paymentAccountService.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        paymentAccountService.delete(id);
        return ApiResponse.success();
    }
}
