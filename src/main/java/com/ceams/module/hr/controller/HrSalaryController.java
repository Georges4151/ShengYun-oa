package com.ceams.module.hr.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.hr.dto.SalaryChangeApplyRequest;
import com.ceams.module.hr.dto.SalaryCreateRequest;
import com.ceams.module.hr.enums.SalaryChangeStatus;
import com.ceams.module.hr.enums.SalaryChangeType;
import com.ceams.module.hr.enums.SalaryStatus;
import com.ceams.module.hr.service.HrSalaryChangeService;
import com.ceams.module.hr.service.HrSalaryService;
import com.ceams.module.hr.vo.SalaryChangeVO;
import com.ceams.module.hr.vo.SalaryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HR 薪资台账与薪资变动接口。
 *
 * <p>权限口径：JWT 仅携带 ROLE_*，权限点 {@code hr:salary:view / hr:salary:change} 在 V30 种子中
 * 分别授予 SYS_ADMIN、FINANCE 与 SYS_ADMIN，故此处使用等价的角色表达式做方法级防护。
 */
@RestController
@RequestMapping("/api/v1/hr")
@RequiredArgsConstructor
public class HrSalaryController {

    private final HrSalaryService salaryService;
    private final HrSalaryChangeService salaryChangeService;

    // ------------------------------------------------------------------
    // 薪资台账
    // ------------------------------------------------------------------

    @GetMapping("/salaries/my")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<SalaryVO> mySalary() {
        return ApiResponse.success(salaryService.mySalary());
    }

    @GetMapping("/salaries")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<PageResult<SalaryVO>> pageSalaries(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) SalaryStatus status) {
        return ApiResponse.success(salaryService.page(page, size, employeeId, status));
    }

    @PostMapping("/salaries")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<String> createSalary(@Valid @RequestBody SalaryCreateRequest request) {
        return ApiResponse.success(salaryService.createSalary(request));
    }

    // ------------------------------------------------------------------
    // 薪资变动申请
    // ------------------------------------------------------------------

    @PostMapping("/salary-changes")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<String> applyChange(@Valid @RequestBody SalaryChangeApplyRequest request) {
        return ApiResponse.success(salaryChangeService.applyChange(request));
    }

    @GetMapping("/salary-changes")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<PageResult<SalaryChangeVO>> pageChanges(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) SalaryChangeType changeType,
            @RequestParam(required = false) SalaryChangeStatus status) {
        return ApiResponse.success(
                salaryChangeService.pageChanges(page, size, employeeId, changeType, status));
    }

    @GetMapping("/salary-changes/my")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<SalaryChangeVO>> myChanges(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.success(salaryChangeService.myChanges(page, size));
    }
}
