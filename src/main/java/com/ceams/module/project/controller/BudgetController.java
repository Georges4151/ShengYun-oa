package com.ceams.module.project.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.project.dto.budget.BudgetCreateRequest;
import com.ceams.module.project.dto.budget.BudgetUpdateRequest;
import com.ceams.module.project.service.BudgetService;
import com.ceams.module.project.vo.BudgetVO;
import com.ceams.module.project.vo.IdResponse;
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
 * 预算管理接口。
 */
@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<BudgetVO>> page(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "20") long size,
                                                  @RequestParam(required = false) Long projectId) {
        return ApiResponse.success(budgetService.page(page, size, projectId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<IdResponse> create(@Valid @RequestBody BudgetCreateRequest request) {
        return ApiResponse.success(budgetService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<BudgetVO> detail(@PathVariable Long id) {
        return ApiResponse.success(budgetService.detail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody BudgetUpdateRequest request) {
        budgetService.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        budgetService.delete(id);
        return ApiResponse.success();
    }
}