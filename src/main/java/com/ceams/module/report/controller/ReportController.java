package com.ceams.module.report.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.module.report.dto.FinancialReportQuery;
import com.ceams.module.report.dto.MaterialReportQuery;
import com.ceams.module.report.dto.ProjectCostReportQuery;
import com.ceams.module.report.service.ReportService;
import com.ceams.module.report.vo.DashboardVO;
import com.ceams.module.report.vo.FinancialReportVO;
import com.ceams.module.report.vo.MaterialReportVO;
import com.ceams.module.report.vo.ProjectCostReportVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 跨域只读报表接口（report 模块，只读、不挂 @HmacSigned）。
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/financial")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<FinancialReportVO> financial(@Valid FinancialReportQuery query) {
        return ApiResponse.success(reportService.financial(query));
    }

    @GetMapping("/project-cost")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ProjectCostReportVO> projectCost(@Valid ProjectCostReportQuery query) {
        return ApiResponse.success(reportService.projectCost(query));
    }

    @GetMapping("/material")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<MaterialReportVO> material(@Valid MaterialReportQuery query) {
        return ApiResponse.success(reportService.material(query));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<DashboardVO> dashboard() {
        return ApiResponse.success(reportService.dashboard());
    }
}
