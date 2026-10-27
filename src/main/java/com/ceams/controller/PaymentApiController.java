package com.ceams.controller;

import com.ceams.dto.PaymentAccountDTO;
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
import java.util.List;

@RestController
@RequestMapping("/api/payment")
public class PaymentApiController {

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

    // 账户管理
    @GetMapping("/accounts")
    public ResponseEntity<?> getAllAccounts(
            @RequestParam(required = false) String type) {
        List<PaymentAccount> accounts;
        if (type != null && !type.isEmpty()) {
            accounts = paymentAccountService.getAccountsByType(type);
        } else {
            accounts = paymentAccountService.getAllAccounts();
        }
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<?> getAccountById(@PathVariable Integer id) {
        PaymentAccount account = paymentAccountService.getAccountById(id);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(account);
    }

    @PostMapping("/accounts")
    public ResponseEntity<?> createAccount(@RequestBody PaymentAccountDTO dto) {
        Integer currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(401).body("用户未登录");
        }
        // 将DTO转换为实体，敏感字段由服务端设置
        PaymentAccount account = new PaymentAccount();
        account.setName(dto.getName());
        account.setType(dto.getType());
        account.setBalance(dto.getBalance());
        account.setDescription(dto.getDescription());
        account.setCreateTime(new Date());
        account.setUpdateTime(new Date());
        boolean success = paymentAccountService.addAccount(account);
        if (success) {
            return ResponseEntity.ok(account);
        }
        return ResponseEntity.badRequest().body("创建账户失败");
    }

    @PutMapping("/accounts/{id}")
    public ResponseEntity<?> updateAccount(@PathVariable Integer id, @RequestBody PaymentAccount account) {
        account.setId(id);
        boolean success = paymentAccountService.updateAccount(account);
        if (success) {
            return ResponseEntity.ok(account);
        }
        return ResponseEntity.badRequest().body("更新账户失败");
    }

    @DeleteMapping("/accounts/{id}")
    public ResponseEntity<?> deleteAccount(@PathVariable Integer id) {
        boolean success = paymentAccountService.deleteAccount(id);
        if (success) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("删除账户失败");
    }

    // 交易记录管理
    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(
            @RequestParam(required = false) Integer accountId,
            @RequestParam(required = false) String type) {
        List<PaymentTransaction> transactions;
        if (accountId != null && type != null && !type.isEmpty()) {
            transactions = paymentTransactionService.getTransactionsByConditions(accountId, null, type, null, null, null);
        } else if (accountId != null) {
            transactions = paymentTransactionService.getTransactionsByAccountId(accountId);
        } else if (type != null && !type.isEmpty()) {
            transactions = paymentTransactionService.getTransactionsByConditions(null, null, type, null, null, null);
        } else {
            transactions = paymentTransactionService.getAllTransactions();
        }
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<?> getTransactionById(@PathVariable Integer id) {
        PaymentTransaction transaction = paymentTransactionService.getTransactionById(id);
        if (transaction == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(transaction);
    }

    @PostMapping("/transactions")
    public ResponseEntity<?> createTransaction(@RequestBody PaymentTransactionDTO dto) {
        Integer currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(401).body("用户未登录");
        }
        // 将DTO转换为实体，敏感字段由服务端设置
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setAccountId(dto.getAccountId());
        transaction.setType(dto.getType());
        transaction.setAmount(dto.getAmount());
        transaction.setDescription(dto.getDescription());
        transaction.setTransactionDate(dto.getTransactionDate() != null ? dto.getTransactionDate() : new Date());
        transaction.setCreateTime(new Date());
        boolean success = paymentTransactionService.addTransaction(transaction);
        if (success) {
            return ResponseEntity.ok(transaction);
        }
        return ResponseEntity.badRequest().body("创建交易记录失败");
    }

    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<?> deleteTransaction(@PathVariable Integer id) {
        boolean success = paymentTransactionService.deleteTransaction(id);
        if (success) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("删除交易记录失败");
    }
}
