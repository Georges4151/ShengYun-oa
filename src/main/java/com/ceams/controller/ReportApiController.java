package com.ceams.controller;

import com.ceams.service.AccountService;
import com.ceams.service.MaterialService;
import com.ceams.service.ProjectService;
import com.ceams.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportApiController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private MaterialService materialService;

    /**
     * 获取财务报表数据（JSON格式）
     */
    @GetMapping("/financial")
    public ResponseEntity<?> getFinancialReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer projectId) {
        try {
            Map<String, Object> result = new HashMap<>();

            // 获取账目汇总
            Map<String, Object> summary = accountService.getAccountSummary(projectId, null, null);
            result.put("summary", summary);

            // 获取账目列表
            List<?> accounts;
            if (projectId != null) {
                accounts = accountService.getByProjectId(projectId);
            } else {
                accounts = accountService.getAllAccounts();
            }
            result.put("accounts", accounts);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("获取财务报表失败: " + e.getMessage());
        }
    }

    /**
     * 获取项目成本报表数据（JSON格式）
     */
    @GetMapping("/project-cost/{projectId}")
    public ResponseEntity<?> getProjectCostReport(@PathVariable Integer projectId) {
        try {
            Map<String, Object> result = new HashMap<>();

            // 获取项目成本分析
            result.put("costAnalysis", projectService.getProjectCostAnalysisDTO());

            // 获取项目详情
            result.put("project", projectService.getProjectDetail(projectId));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("获取项目成本报表失败: " + e.getMessage());
        }
    }

    /**
     * 获取材料报表数据（JSON格式）
     */
    @GetMapping("/material")
    public ResponseEntity<?> getMaterialReport() {
        try {
            Map<String, Object> result = new HashMap<>();

            // 获取材料统计
            Map<String, Object> stats = materialService.getMaterialStatistics();
            result.put("statistics", stats);

            // 获取材料列表
            result.put("materials", materialService.getAllMaterials());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("获取材料报表失败: " + e.getMessage());
        }
    }

    /**
     * 获取合同支付报表
     */
    @GetMapping("/contract-payments")
    public ResponseEntity<?> getContractPaymentReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            Map<String, Object> report = reportService.getContractPaymentReport(startDate, endDate);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("获取合同支付报表失败: " + e.getMessage());
        }
    }

    /**
     * 获取现金流预测
     */
    @GetMapping("/cash-flow")
    public ResponseEntity<?> getCashFlowForecast(@RequestParam(defaultValue = "6") Integer months) {
        try {
            Map<String, Object> forecast = reportService.getCashFlowForecast(months);
            return ResponseEntity.ok(forecast);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("获取现金流预测失败: " + e.getMessage());
        }
    }

    /**
     * 获取成本分析报表
     */
    @GetMapping("/cost-analysis")
    public ResponseEntity<?> getCostAnalysisReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            Map<String, Object> report = reportService.getCostAnalysisReport(startDate, endDate);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("获取成本分析报表失败: " + e.getMessage());
        }
    }

    /**
     * 导出财务报表（PDF格式）
     */
    @GetMapping("/export/financial/pdf")
    public ResponseEntity<?> exportFinancialReportPdf(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer projectId) {
        try {
            byte[] reportBytes = reportService.generateFinancialReport(startDate, endDate, projectId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "financial_report.pdf");
            headers.setContentLength(reportBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(reportBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("导出财务报表失败: " + e.getMessage());
        }
    }

    /**
     * 导出财务报表（Excel格式）
     */
    @GetMapping("/export/financial/excel")
    public ResponseEntity<?> exportFinancialReportExcel(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer projectId) {
        try {
            byte[] reportBytes = reportService.generateFinancialReportExcel(startDate, endDate, projectId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "financial_report.xlsx");
            headers.setContentLength(reportBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(reportBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("导出财务报表失败: " + e.getMessage());
        }
    }

    /**
     * 导出项目成本报表（PDF格式）
     */
    @GetMapping("/export/project-cost/{projectId}/pdf")
    public ResponseEntity<?> exportProjectCostReportPdf(@PathVariable Integer projectId) {
        try {
            byte[] reportBytes = reportService.generateProjectCostReport(projectId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "project_cost_report.pdf");
            headers.setContentLength(reportBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(reportBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("导出项目成本报表失败: " + e.getMessage());
        }
    }

    /**
     * 导出项目成本报表（Excel格式）
     */
    @GetMapping("/export/project-cost/{projectId}/excel")
    public ResponseEntity<?> exportProjectCostReportExcel(@PathVariable Integer projectId) {
        try {
            byte[] reportBytes = reportService.generateProjectCostReportExcel(projectId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "project_cost_report.xlsx");
            headers.setContentLength(reportBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(reportBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("导出项目成本报表失败: " + e.getMessage());
        }
    }

    /**
     * 导出材料报表（PDF格式）
     */
    @GetMapping("/export/material/pdf")
    public ResponseEntity<?> exportMaterialReportPdf() {
        try {
            byte[] reportBytes = reportService.generateMaterialReport();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "material_report.pdf");
            headers.setContentLength(reportBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(reportBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("导出材料报表失败: " + e.getMessage());
        }
    }

    /**
     * 导出材料报表（Excel格式）
     */
    @GetMapping("/export/material/excel")
    public ResponseEntity<?> exportMaterialReportExcel() {
        try {
            byte[] reportBytes = reportService.generateMaterialReportExcel();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "material_report.xlsx");
            headers.setContentLength(reportBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(reportBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("导出材料报表失败: " + e.getMessage());
        }
    }
}
