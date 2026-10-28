package com.ceams.module.expense.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.common.sign.HmacSigned;
import com.ceams.module.expense.dto.ApprovalActionRequest;
import com.ceams.module.expense.dto.ReceiptCreateRequest;
import com.ceams.module.expense.dto.ReceiptUpdateRequest;
import com.ceams.module.expense.dto.ReceiptVerifyRequest;
import com.ceams.module.expense.enums.PaymentStatus;
import com.ceams.module.expense.enums.ReceiptApprovalStatus;
import com.ceams.module.expense.enums.VerificationStatus;
import com.ceams.module.expense.service.ReceiptService;
import com.ceams.module.expense.vo.ApprovalRecordVO;
import com.ceams.module.expense.vo.ReceiptVO;
import com.ceams.module.expense.vo.ReceiptVerificationVO;
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

import java.util.List;

/**
 * 票据（报销）管理接口。
 */
@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<ReceiptVO>> page(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "20") long size,
                                                   @RequestParam(required = false) String receiptNumber,
                                                   @RequestParam(required = false) Long userId,
                                                   @RequestParam(required = false) Long projectId,
                                                   @RequestParam(required = false) VerificationStatus verificationStatus,
                                                   @RequestParam(required = false) PaymentStatus paymentStatus,
                                                   @RequestParam(required = false) ReceiptApprovalStatus approvalStatus) {
        return ApiResponse.success(receiptService.page(page, size, receiptNumber, userId, projectId,
                verificationStatus, paymentStatus, approvalStatus));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> create(@Valid @RequestBody ReceiptCreateRequest request) {
        return ApiResponse.success(receiptService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ReceiptVO> detail(@PathVariable Long id) {
        return ApiResponse.success(receiptService.detail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody ReceiptUpdateRequest request) {
        receiptService.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        receiptService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> submit(@PathVariable Long id) {
        receiptService.submit(id);
        return ApiResponse.success();
    }

    @HmacSigned
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> approve(@PathVariable Long id,
                                     @RequestBody(required = false) ApprovalActionRequest request) {
        receiptService.approve(id, request);
        return ApiResponse.success();
    }

    @HmacSigned
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> reject(@PathVariable Long id,
                                    @RequestBody(required = false) ApprovalActionRequest request) {
        receiptService.reject(id, request);
        return ApiResponse.success();
    }

    @HmacSigned
    @PostMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> verify(@PathVariable Long id, @Valid @RequestBody ReceiptVerifyRequest request) {
        receiptService.verify(id, request);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/approvals")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ApprovalRecordVO>> approvals(@PathVariable Long id) {
        return ApiResponse.success(receiptService.listApprovals(id));
    }

    @GetMapping("/{id}/verifications")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ReceiptVerificationVO>> verifications(@PathVariable Long id) {
        return ApiResponse.success(receiptService.listVerifications(id));
    }
}
