package com.ceams.controller;

import com.ceams.model.Pagination;
import com.ceams.model.entity.Receipt;
import com.ceams.security.CustomUserDetails;
import com.ceams.service.ReceiptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptApiController {

    @Autowired
    private ReceiptService receiptService;

    private Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            return userDetails.getUserId();
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<?> getAllReceipts(
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) Integer reporterId,
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) String verificationStatus,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        
        Pagination pagination = new Pagination(pageNum, pageSize);
        
        List<Receipt> receipts;
        long total;
        
        if (verificationStatus != null) {
            receipts = receiptService.getByVerificationStatus(verificationStatus);
            total = receipts.size();
        } else if (type != null) {
            receipts = receiptService.getByType(type);
            total = receipts.size();
        } else if (startDate != null && endDate != null) {
            receipts = receiptService.getByDateRange(startDate, endDate);
            total = receipts.size();
        } else if (userId != null) {
            receipts = receiptService.getByUserId(userId);
            total = receipts.size();
        } else if (reporterId != null) {
            receipts = receiptService.getByReporterId(reporterId);
            total = receipts.size();
        } else if (projectId != null) {
            receipts = receiptService.getByProjectId(projectId);
            total = receipts.size();
        } else {
            receipts = receiptService.getReceiptsWithPagination(pagination);
            total = receiptService.count();
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", receipts);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getReceiptById(@PathVariable Integer id) {
        Receipt receipt = receiptService.getReceiptDetail(id);
        if (receipt == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(receipt);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadReceipt(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) Integer reporterId,
            @RequestParam(required = false) String reporterName,
            @RequestParam(required = false) Long receiptDate,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String receiptNumber) {
        Integer userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body("用户未登录");
        }
        try {
            Date date = receiptDate != null ? new Date(receiptDate) : new Date();
            
            Receipt receipt;
            if (file != null && !file.isEmpty()) {
                receipt = receiptService.uploadReceipt(
                    file.getBytes(), userId, reporterId, projectId, type, date, amount, reporterName, description, receiptNumber);
            } else {
                return ResponseEntity.badRequest().body("请上传票据文件");
            }
            return ResponseEntity.ok(receipt);
        } catch (IOException | NoSuchAlgorithmException e) {
            return ResponseEntity.internalServerError().body("票据上传失败: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createReceipt(@RequestBody Receipt receipt) {
        // 使用字节数组方式上传（无文件）
        Integer currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(401).body("用户未登录");
        }
        try {
            Receipt result = receiptService.uploadReceipt(
                new byte[0], currentUserId, receipt.getReporterId(), receipt.getProjectId(),
                receipt.getType(), receipt.getReceiptDate(), receipt.getAmount(),
                receipt.getReporterName(), receipt.getDescription(), receipt.getReceiptNumber());
            return ResponseEntity.ok(result);
        } catch (IOException | NoSuchAlgorithmException e) {
            return ResponseEntity.internalServerError().body("创建票据失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/verify")
    public ResponseEntity<?> verifyReceipt(
            @PathVariable Integer id,
            @RequestParam Boolean isVerified,
            @RequestParam(required = false) Integer verifiedBy) {
        Integer verifierId = getCurrentUserId();
        if (verifierId == null) {
            return ResponseEntity.status(401).body("用户未登录");
        }
        String status = isVerified ? "verified" : "rejected";
        boolean success = receiptService.verifyReceipt(id, verifierId, status);
        if (success) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("验证票据失败");
    }

    @PutMapping("/{id}/payment")
    public ResponseEntity<?> updatePaymentStatus(
            @PathVariable Integer id,
            @RequestParam String paymentStatus,
            @RequestParam(required = false) BigDecimal paidAmount,
            @RequestParam(required = false) Integer paidBy,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) Integer accountId) {
        boolean success = receiptService.updatePaymentStatus(
            id, paymentStatus, paidAmount, paidBy, new Date(), paymentMethod, accountId);
        if (success) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("更新支付状态失败");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReceipt(@PathVariable Integer id) {
        boolean success = receiptService.deleteReceipt(id);
        if (success) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("删除票据失败");
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchReceipts(@RequestParam String keyword) {
        List<Receipt> receipts = receiptService.searchReceipts(keyword);
        return ResponseEntity.ok(receipts);
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingReceipts() {
        List<Receipt> receipts = receiptService.getByVerificationStatus("pending");
        return ResponseEntity.ok(receipts);
    }

    @GetMapping("/pending-approvals")
    public ResponseEntity<?> getPendingApprovals(
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String role) {
        List<Receipt> receipts = receiptService.getByVerificationStatus("pending");
        return ResponseEntity.ok(receipts);
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<?> verifyReceiptPost(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> request) {
        Integer verifierId = getCurrentUserId();
        if (verifierId == null) {
            return ResponseEntity.status(401).body("用户未登录");
        }
        Boolean isVerified = (Boolean) request.get("isVerified");
        boolean success = receiptService.verifyReceipt(id, verifierId, isVerified ? "verified" : "rejected");
        if (success) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("验证票据失败");
    }

    @PostMapping("/{id}/approval")
    public ResponseEntity<?> submitApproval(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> request) {
        Integer approverId = getCurrentUserId();
        if (approverId == null) {
            return ResponseEntity.status(401).body("用户未登录");
        }
        String action = (String) request.get("action");
        String comment = (String) request.get("comment");
        boolean success = receiptService.verifyReceipt(id, approverId, "approved".equals(action) ? "verified" : "rejected");
        if (success) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("审批处理失败");
    }

    @GetMapping("/check-duplicate")
    public ResponseEntity<?> checkDuplicate(@RequestParam String receiptNumber) {
        boolean exists = receiptService.isReceiptNumberDuplicate(receiptNumber);
        Map<String, Object> result = new HashMap<>();
        result.put("exists", exists);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/approval-history")
    public ResponseEntity<?> getApprovalHistory(@PathVariable Integer id) {
        Receipt receipt = receiptService.getReceiptDetail(id);
        if (receipt == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new java.util.ArrayList<>());
    }
}
