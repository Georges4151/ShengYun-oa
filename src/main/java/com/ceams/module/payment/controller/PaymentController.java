package com.ceams.module.payment.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.sign.HmacSigned;
import com.ceams.module.payment.dto.PaymentExecuteRequest;
import com.ceams.module.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 付款执行接口（高危，HMAC 签名）。 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @HmacSigned
    @PostMapping("/execute")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<String> execute(@Valid @RequestBody PaymentExecuteRequest request) {
        return ApiResponse.success(paymentService.execute(request));
    }
}
