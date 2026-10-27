package com.ceams.controller;

import com.ceams.dto.PaymentTransactionDTO;
import com.ceams.model.entity.PaymentAccount;
import com.ceams.model.entity.PaymentTransaction;
import com.ceams.security.CustomUserDetails;
import com.ceams.service.PaymentAccountService;
import com.ceams.service.PaymentTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FX客户端支付相关API控制器
 * 处理FX客户端调用的 /api/payment-accounts 和 /api/payment-transactions 端点
 */
@RestController
@RequestMapping("/api")
public class PaymentFXApiController {

    @Autowired
    private PaymentAccountService paymentAccountService;

    @Autowired
    private PaymentTransactionService paymentTransactionService;

    /**
     * 从SecurityContext获取当前登录用户ID
     */
    private Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            return userDetails.getUserId();
        }
        return null;
    }

    /**
     * FX客户端调用: apiClient.get("/api/payment-accounts", PaymentAccountListResponse.class)
     * 返回支付账户列表，包装为 {accounts: [...]} 格式
     */
    @GetMapping("/payment-accounts")
    public ResponseEntity<?> getPaymentAccounts(
            @RequestParam(required = false) String type) {
        List<PaymentAccount> accounts;
        if (type != null && !type.isEmpty()) {
            accounts = paymentAccountService.getAccountsByType(type);
        } else {
            accounts = paymentAccountService.getAllAccounts();
        }
        Map<String, Object> response = new HashMap<>();
        response.put("accounts", accounts);
        return ResponseEntity.ok(response);
    }

    /**
     * FX客户端调用: apiClient.post("/api/payment-transactions", transaction, PaymentTransaction.class)
     * 创建支付交易记录
     */
    @PostMapping("/payment-transactions")
    public ResponseEntity<?> createPaymentTransaction(@RequestBody PaymentTransactionDTO dto) {
        Integer currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(401).body("用户未登录");
        }
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setAccountId(dto.getAccountId());
        transaction.setReceiptId(dto.getReceiptId());
        transaction.setType(dto.getType());
        transaction.setAmount(dto.getAmount());
        transaction.setDescription(dto.getDescription());
        transaction.setTransactionDate(dto.getTransactionDate() != null ? dto.getTransactionDate() : new Date());
        transaction.setCreateTime(new Date());
        boolean success = paymentTransactionService.addTransaction(transaction);
        if (success) {
            return ResponseEntity.ok(transaction);
        }
        return ResponseEntity.badRequest().body("创建支付交易记录失败");
    }
}
