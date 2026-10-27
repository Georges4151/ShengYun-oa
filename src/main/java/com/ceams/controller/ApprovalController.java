package com.ceams.controller;

import com.ceams.constant.RoleConstants;
import com.ceams.model.entity.ApprovalRecord;
import com.ceams.model.entity.Receipt;
import com.ceams.security.CustomUserDetails;
import com.ceams.service.ApprovalFlowService;
import com.ceams.service.ReceiptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/approval")
public class ApprovalController {

    @Autowired
    private ApprovalFlowService approvalFlowService;

    @Autowired
    private ReceiptService receiptService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadReceipt(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "projectId", required = false) Integer projectId,
            @RequestParam(value = "reporterId", required = false) Integer reporterId,
            @RequestParam(value = "reporterName", required = false) String reporterName,
            @RequestParam(value = "receiptDate", required = false) Long receiptDate,
            @RequestParam(value = "amount", required = false) BigDecimal amount,
            @RequestParam(value = "description", required = false) String description,
            Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer userId = userDetails.getUserId();

            Date receiptDateValue = receiptDate != null ? new Date(receiptDate) : new Date();

            Receipt receipt = receiptService.uploadReceipt(
                    file.getBytes(),
                    userId,
                    reporterId,
                    projectId,
                    type,
                    receiptDateValue,
                    amount,
                    reporterName,
                    description);

            return ResponseEntity.ok(receipt);
        } catch (IOException | NoSuchAlgorithmException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("票据上传失败: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("票据上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingApprovals(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String role = userDetails.getRole();

            Integer approvalLevel = getApprovalLevelByRole(role);
            if (approvalLevel == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无审批权限");
            }

            Map<String, Object> result = approvalFlowService.getPendingApprovals(approvalLevel, page, pageSize);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("获取待审批列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/pending/all")
    public ResponseEntity<?> getAllPendingApprovals(Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String role = userDetails.getRole();

            Integer approvalLevel = getApprovalLevelByRole(role);
            if (approvalLevel == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无审批权限");
            }

            List<Receipt> pendingReceipts = approvalFlowService.getPendingApprovals(approvalLevel);
            return ResponseEntity.ok(pendingReceipts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("获取待审批列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/history/{receiptId}")
    public ResponseEntity<?> getApprovalHistory(@PathVariable Integer receiptId) {
        try {
            List<ApprovalRecord> history = approvalFlowService.getApprovalHistory(receiptId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("获取审批历史失败: " + e.getMessage());
        }
    }

    @PostMapping("/process")
    public ResponseEntity<?> processApproval(@RequestBody ApprovalRequest approvalRequest,
            Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer approverId = userDetails.getUserId();
            String approverName = userDetails.getUsername();
            String role = userDetails.getRole();

            // 验证审批权限
            boolean canApprove = approvalFlowService.canApprove(approvalRequest.getReceiptId(), approverId, role);
            if (!canApprove) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无审批权限");
            }

            boolean success = approvalFlowService.processApproval(
                    approvalRequest.getReceiptId(),
                    approverId,
                    approverName,
                    approvalRequest.getStatus(),
                    approvalRequest.getComment());

            if (success) {
                return ResponseEntity.ok("审批处理成功");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("审批处理失败");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("审批处理失败: " + e.getMessage());
        }
    }

    @GetMapping("/status/{receiptId}")
    public ResponseEntity<?> getApprovalStatus(@PathVariable Integer receiptId) {
        try {
            Receipt receipt = approvalFlowService.getReceiptApprovalStatus(receiptId);
            return ResponseEntity.ok(receipt);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("获取审批状态失败: " + e.getMessage());
        }
    }

    @GetMapping("/receipt/{id}")
    public ResponseEntity<?> getReceiptDetail(@PathVariable Integer id) {
        try {
            Receipt receipt = approvalFlowService.getReceiptApprovalStatus(id);
            if (receipt == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("票据不存在");
            }
            return ResponseEntity.ok(receipt);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("获取票据详情失败: " + e.getMessage());
        }
    }

    // 根据角色获取审批级别
    private Integer getApprovalLevelByRole(String role) {
        switch (role) {
            case RoleConstants.FINANCE:
                return 0;
            case RoleConstants.PROJECT_MANAGER:
                return 1;
            case RoleConstants.MANAGER:
                return 2;
            case RoleConstants.ADMIN:
                return 2; // 管理员拥有最高审批权限
            default:
                return null;
        }
    }

    // 审批请求DTO
    public static class ApprovalRequest {
        private Integer receiptId;
        private String status; // approved, rejected
        private String comment;

        public Integer getReceiptId() {
            return receiptId;
        }

        public void setReceiptId(Integer receiptId) {
            this.receiptId = receiptId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }
}
