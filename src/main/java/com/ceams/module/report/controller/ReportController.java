package com.ceams.module.report.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.module.report.dto.AdminAssetReportQuery;
import com.ceams.module.report.dto.BpmEfficiencyQuery;
import com.ceams.module.report.dto.FinancialReportQuery;
import com.ceams.module.report.dto.HrCostQuery;
import com.ceams.module.report.dto.HrPerformanceQuery;
import com.ceams.module.report.dto.MaterialReportQuery;
import com.ceams.module.report.dto.ProjectCostReportQuery;
import com.ceams.module.report.service.ReportService;
import com.ceams.module.report.support.ReportExcelExporter;
import com.ceams.module.report.vo.AdminAssetReportVO;
import com.ceams.module.report.vo.BpmEfficiencyReportVO;
import com.ceams.module.report.vo.DashboardVO;
import com.ceams.module.report.vo.FinancialReportVO;
import com.ceams.module.report.vo.HrCostReportVO;
import com.ceams.module.report.vo.HrPerformanceReportVO;
import com.ceams.module.report.vo.MaterialReportVO;
import com.ceams.module.report.vo.ProjectCostReportVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 跨域只读报表接口（report 模块，只读、不挂 @HmacSigned）。 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    private final ReportExcelExporter reportExcelExporter;

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

    @GetMapping("/hr-cost")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<HrCostReportVO> hrCost(@Valid HrCostQuery query) {
        return ApiResponse.success(reportService.hrCost(query));
    }

    @GetMapping("/hr-performance")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<HrPerformanceReportVO> hrPerformance(@Valid HrPerformanceQuery query) {
        return ApiResponse.success(reportService.hrPerformance(query));
    }

    @GetMapping("/admin-assets")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public ApiResponse<AdminAssetReportVO> adminAssets(@Valid AdminAssetReportQuery query) {
        return ApiResponse.success(reportService.adminAssets(query));
    }

    @GetMapping("/bpm-efficiency")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public ApiResponse<BpmEfficiencyReportVO> bpmEfficiency(@Valid BpmEfficiencyQuery query) {
        return ApiResponse.success(reportService.bpmEfficiency(query));
    }

    // ------------------------------------------------------------------
    // 导出：与页面查询参数、权限保持一致，返回 xlsx 附件
    // ------------------------------------------------------------------

    @GetMapping("/financial/export")
    @PreAuthorize("isAuthenticated()")
    public void exportFinancial(@Valid FinancialReportQuery query, HttpServletResponse response)
            throws IOException {
        prepareXlsx(response, "财务报表.xlsx");
        reportExcelExporter.exportFinancial(
                reportService.financial(query), response.getOutputStream());
    }

    @GetMapping("/project-cost/export")
    @PreAuthorize("isAuthenticated()")
    public void exportProjectCost(
            @Valid ProjectCostReportQuery query, HttpServletResponse response) throws IOException {
        prepareXlsx(response, "项目成本报表.xlsx");
        reportExcelExporter.exportProjectCost(
                reportService.projectCost(query), response.getOutputStream());
    }

    @GetMapping("/material/export")
    @PreAuthorize("isAuthenticated()")
    public void exportMaterial(@Valid MaterialReportQuery query, HttpServletResponse response)
            throws IOException {
        prepareXlsx(response, "材料报表.xlsx");
        reportExcelExporter.exportMaterial(
                reportService.material(query), response.getOutputStream());
    }

    @GetMapping("/hr-cost/export")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public void exportHrCost(@Valid HrCostQuery query, HttpServletResponse response)
            throws IOException {
        prepareXlsx(response, "人力成本报表.xlsx");
        reportExcelExporter.exportHrCost(reportService.hrCost(query), response.getOutputStream());
    }

    @GetMapping("/hr-performance/export")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public void exportHrPerformance(
            @Valid HrPerformanceQuery query, HttpServletResponse response) throws IOException {
        prepareXlsx(response, "绩效考核报表.xlsx");
        reportExcelExporter.exportHrPerformance(
                reportService.hrPerformance(query), response.getOutputStream());
    }

    @GetMapping("/admin-assets/export")
    @PreAuthorize("hasAnyRole('SYS_ADMIN')")
    public void exportAdminAssets(
            @Valid AdminAssetReportQuery query, HttpServletResponse response) throws IOException {
        prepareXlsx(response, "行政资产报表.xlsx");
        reportExcelExporter.exportAdminAssets(
                reportService.adminAssets(query), response.getOutputStream());
    }

    @GetMapping("/bpm-efficiency/export")
    @PreAuthorize("hasAnyRole('SYS_ADMIN','FINANCE')")
    public void exportBpmEfficiency(
            @Valid BpmEfficiencyQuery query, HttpServletResponse response) throws IOException {
        prepareXlsx(response, "审批效率报表.xlsx");
        reportExcelExporter.exportBpmEfficiency(
                reportService.bpmEfficiency(query), response.getOutputStream());
    }

    /** 统一设置 xlsx 响应头；中文文件名按 RFC 5987 编码，兼容各浏览器。 */
    private void prepareXlsx(HttpServletResponse response, String filename) {
        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader(
                "Content-Disposition", "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
    }
}
