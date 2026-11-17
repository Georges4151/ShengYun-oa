package com.ceams.module.hr.controller;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.hr.dto.PayrollAdjustRequest;
import com.ceams.module.hr.dto.PayrollCalcRequest;
import com.ceams.module.hr.dto.PayrollConfirmRequest;
import com.ceams.module.hr.dto.SalarySlipPublishRequest;
import com.ceams.module.hr.enums.PayrollStatus;
import com.ceams.module.hr.enums.SalarySlipReadStatus;
import com.ceams.module.hr.service.HrPayrollService;
import com.ceams.module.hr.service.HrSalarySlipService;
import com.ceams.module.hr.vo.PayrollCalcResultVO;
import com.ceams.module.hr.vo.SalaryCalcVO;
import com.ceams.module.hr.vo.SalarySlipPublishResultVO;
import com.ceams.module.hr.vo.SalarySlipVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HR 月度工资核算接口。
 *
 * <p>权限口径与薪资 / 绩效模块一致：JWT 仅携带 ROLE_*，故使用等价的角色表达式；
 * 核算初始化、调整与确认仅 SYS_ADMIN，明细分页额外放行 FINANCE。
 *
 * <p>工资条接口：发布与导出仅 SYS_ADMIN（导出额外放行 FINANCE），本人查看与已读回写仅要求登录。
 */
@RestController
@RequestMapping("/api/v1/hr/payroll")
@RequiredArgsConstructor
public class HrPayrollController {

    /** xlsx 响应内容类型。 */
    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final HrPayrollService payrollService;
    private final HrSalarySlipService salarySlipService;

    @PostMapping("/calc")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<PayrollCalcResultVO> calculate(
            @Valid @RequestBody PayrollCalcRequest request) {
        return ApiResponse.success(payrollService.calculate(request));
    }

    @GetMapping("/calc")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<PageResult<SalaryCalcVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String calcMonth,
            @RequestParam(required = false) PayrollStatus status,
            @RequestParam(required = false) Long employeeId) {
        return ApiResponse.success(payrollService.page(page, size, calcMonth, status, employeeId));
    }

    @PutMapping("/calc/{id}")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<SalaryCalcVO> adjust(
            @PathVariable Long id, @Valid @RequestBody PayrollAdjustRequest request) {
        return ApiResponse.success(payrollService.adjust(id, request));
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<Integer> confirm(@Valid @RequestBody PayrollConfirmRequest request) {
        return ApiResponse.success(payrollService.confirm(request));
    }

    // ------------------------------------------------------------------
    // 工资条
    // ------------------------------------------------------------------

    @PostMapping("/slips/publish")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<SalarySlipPublishResultVO> publishSlips(
            @Valid @RequestBody SalarySlipPublishRequest request) {
        return ApiResponse.success(salarySlipService.publish(request));
    }

    @GetMapping("/slips")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<PageResult<SalarySlipVO>> pageSlips(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String calcMonth,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) SalarySlipReadStatus readStatus) {
        return ApiResponse.success(
                salarySlipService.page(page, size, calcMonth, employeeId, readStatus));
    }

    @GetMapping("/slips/my")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<SalarySlipVO>> mySlips() {
        return ApiResponse.success(salarySlipService.listMine());
    }

    @PutMapping("/slips/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<SalarySlipVO> markSlipRead(@PathVariable Long id) {
        return ApiResponse.success(salarySlipService.markRead(id));
    }

    @GetMapping("/slips/export")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public void exportSlips(
            @RequestParam String calcMonth, HttpServletResponse response) throws IOException {
        String month = normalizeMonth(calcMonth);
        response.setContentType(XLSX_CONTENT_TYPE);
        response.setHeader(
                "Content-Disposition", "attachment; filename=\"salary-slip-" + month + ".xlsx\"");
        salarySlipService.export(month, response.getOutputStream());
    }

    /** 导出前先校验并规范化核算月，避免非法参数在响应头写出后才失败。 */
    private static String normalizeMonth(String calcMonth) {
        try {
            return YearMonth.parse(calcMonth == null ? "" : calcMonth.trim()).toString();
        } catch (DateTimeParseException e) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "核算月格式应为 yyyy-MM：" + calcMonth);
        }
    }
}
