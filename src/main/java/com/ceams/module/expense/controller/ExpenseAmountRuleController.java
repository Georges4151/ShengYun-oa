package com.ceams.module.expense.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.expense.dto.AmountRuleSaveRequest;
import com.ceams.module.expense.service.ExpenseAmountRuleService;
import com.ceams.module.expense.vo.AmountRuleVO;
import jakarta.validation.Valid;
import java.util.List;
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
 * 报销额度分级规则维护接口。
 *
 * <p>统一由服务层校验 expense:amount-rule:manage 权限点（管理员默认放行），
 * 控制器仅要求登录，避免权限码与角色双重判定口径不一致。
 */
@RestController
@RequestMapping("/api/v1/expense/amount-rules")
@RequiredArgsConstructor
public class ExpenseAmountRuleController {

    private final ExpenseAmountRuleService amountRuleService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<AmountRuleVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(amountRuleService.page(page, size, keyword));
    }

    @GetMapping("/options")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<AmountRuleVO>> options() {
        return ApiResponse.success(amountRuleService.options());
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> create(@Valid @RequestBody AmountRuleSaveRequest request) {
        return ApiResponse.success(amountRuleService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> update(
            @PathVariable Long id, @Valid @RequestBody AmountRuleSaveRequest request) {
        amountRuleService.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        amountRuleService.delete(id);
        return ApiResponse.success();
    }
}
