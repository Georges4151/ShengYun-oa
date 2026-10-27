package com.ceams.controller;

import com.ceams.dto.AccountDTO;
import com.ceams.model.PageResult;
import com.ceams.model.Pagination;
import com.ceams.model.entity.Account;
import com.ceams.security.CustomUserDetails;
import com.ceams.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountApiController {

    @Autowired
    private AccountService accountService;

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
     * 将DTO转换为Account实体
     */
    private Account convertDtoToEntity(AccountDTO dto) {
        Account account = new Account();
        account.setProjectId(dto.getProjectId());
        account.setCategoryId(dto.getCategoryId());
        account.setContactId(dto.getContactId());
        account.setMaterialId(dto.getMaterialId());
        account.setSubcontractorId(dto.getSubcontractorId());
        account.setReceiptId(dto.getReceiptId());
        account.setPaymentAccountId(dto.getPaymentAccountId());
        account.setContractId(dto.getContractId());
        account.setTransactionDate(dto.getTransactionDate());
        account.setAmount(dto.getAmount());
        account.setType(dto.getType());
        account.setPaymentType(dto.getPaymentType());
        account.setDescription(dto.getDescription());
        account.setStatus(dto.getStatus());
        return account;
    }

    @GetMapping
    public ResponseEntity<?> getAllAccounts(
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        
        Pagination pagination = new Pagination(pageNum, pageSize);
        
        if (projectId != null) {
            List<Account> accounts = accountService.getByProjectId(projectId);
            return ResponseEntity.ok(accounts);
        } else if (type != null) {
            List<Account> accounts = accountService.getByType(type);
            return ResponseEntity.ok(accounts);
        } else if (startDate != null && endDate != null) {
            List<Account> accounts = accountService.getByDateRange(startDate, endDate);
            return ResponseEntity.ok(accounts);
        } else {
            List<Account> accounts = accountService.getAccountsWithPagination(pagination);
            long total = accountService.count();
            Map<String, Object> result = new HashMap<>();
            result.put("list", accounts);
            result.put("total", total);
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);
            return ResponseEntity.ok(result);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAccountById(@PathVariable Integer id) {
        Account account = accountService.getAccountDetail(id);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(account);
    }

    @PostMapping
    public ResponseEntity<?> createAccount(@RequestBody AccountDTO dto) {
        try {
            Account account = convertDtoToEntity(dto);
            // 服务端设置敏感字段
            Integer currentUserId = getCurrentUserId();
            if (currentUserId != null) {
                account.setUserId(currentUserId);
            }
            account.setCreateTime(new Date());
            account.setUpdateTime(new Date());
            boolean success = accountService.addAccount(account);
            if (success) {
                return ResponseEntity.ok(account);
            }
            return ResponseEntity.badRequest().body("创建账目失败");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAccount(@PathVariable Integer id, @RequestBody AccountDTO dto) {
        Account account = convertDtoToEntity(dto);
        account.setId(id);
        // 服务端设置更新时间
        account.setUpdateTime(new Date());
        boolean success = accountService.updateAccount(account);
        if (success) {
            return ResponseEntity.ok(account);
        }
        return ResponseEntity.badRequest().body("更新账目失败");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAccount(@PathVariable Integer id) {
        boolean success = accountService.deleteAccount(id);
        if (success) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("删除账目失败");
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        Map<String, Object> summary = accountService.getAccountSummary(projectId, startDate, endDate);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/monthly-stats/{year}")
    public ResponseEntity<?> getMonthlyStats(@PathVariable Integer year) {
        List<Map<String, Object>> stats = accountService.getMonthlyStats(year);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchAccounts(@RequestParam String keyword) {
        List<Account> accounts = accountService.searchAccounts(keyword);
        return ResponseEntity.ok(accounts);
    }

    @PostMapping("/from-contract")
    public ResponseEntity<?> createFromContract(@RequestBody AccountDTO dto) {
        try {
            Account account = convertDtoToEntity(dto);
            // 服务端设置敏感字段
            Integer currentUserId = getCurrentUserId();
            if (currentUserId != null) {
                account.setUserId(currentUserId);
            }
            account.setCreateTime(new Date());
            account.setUpdateTime(new Date());
            boolean success = accountService.addAccountFromContract(account);
            if (success) {
                return ResponseEntity.ok(account);
            }
            return ResponseEntity.badRequest().body("从合同创建账目失败");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/contract/{contractId}")
    public ResponseEntity<?> getByContractId(@PathVariable Integer contractId) {
        List<Account> accounts = accountService.getByContractId(contractId);
        return ResponseEntity.ok(accounts);
    }
}
