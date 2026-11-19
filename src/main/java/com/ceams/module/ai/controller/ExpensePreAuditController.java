package com.ceams.module.ai.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.module.ai.service.ExpensePreAuditService;
import com.ceams.module.ai.vo.ReceiptPreAuditVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 报销预审接口（只读提示，仅 SYS_ADMIN / FINANCE）。 */
@RestController
@RequestMapping("/api/v1/ai/preaudit/receipts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
public class ExpensePreAuditController {

    private final ExpensePreAuditService expensePreAuditService;

    /** 待审批票据预审清单（默认 20 条，创建时间倒序）。 */
    @GetMapping
    public ApiResponse<List<ReceiptPreAuditVO>> pendingList(
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(expensePreAuditService.pendingList(limit));
    }

    /** 单张票据预审。 */
    @GetMapping("/{id}")
    public ApiResponse<ReceiptPreAuditVO> audit(@PathVariable Long id) {
        return ApiResponse.success(expensePreAuditService.audit(id));
    }
}
