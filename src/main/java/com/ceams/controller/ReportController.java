package com.ceams.controller;

import com.ceams.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/report")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/list")
    public String list() {
        return "report/list";
    }

    @GetMapping("/generate/financial")
    public String financialReportForm(Model model) {
        return "report/financial_form";
    }

    @PostMapping("/generate/financial")
    public ResponseEntity<byte[]> generateFinancialReport(@RequestParam String startDate, @RequestParam String endDate,
            @RequestParam(required = false) Integer projectId, @RequestParam(defaultValue = "pdf") String format) {
        try {
            byte[] reportBytes;
            String fileName;
            MediaType contentType;

            if ("excel".equals(format)) {
                reportBytes = reportService.generateFinancialReportExcel(startDate, endDate, projectId);
                fileName = "financial_report.xlsx";
                contentType = MediaType
                        .parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            } else {
                reportBytes = reportService.generateFinancialReport(startDate, endDate, projectId);
                fileName = "financial_report.pdf";
                contentType = MediaType.APPLICATION_PDF;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(contentType);
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(reportBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(reportBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @GetMapping("/generate/project-cost/{projectId}")
    public ResponseEntity<byte[]> generateProjectCostReport(@PathVariable Integer projectId,
            @RequestParam(defaultValue = "pdf") String format) {
        try {
            byte[] reportBytes;
            String fileName;
            MediaType contentType;

            if ("excel".equals(format)) {
                reportBytes = reportService.generateProjectCostReportExcel(projectId);
                fileName = "project_cost_report.xlsx";
                contentType = MediaType
                        .parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            } else {
                reportBytes = reportService.generateProjectCostReport(projectId);
                fileName = "project_cost_report.pdf";
                contentType = MediaType.APPLICATION_PDF;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(contentType);
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(reportBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(reportBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @GetMapping("/generate/material")
    public ResponseEntity<byte[]> generateMaterialReport(@RequestParam(defaultValue = "pdf") String format) {
        try {
            byte[] reportBytes;
            String fileName;
            MediaType contentType;

            if ("excel".equals(format)) {
                reportBytes = reportService.generateMaterialReportExcel();
                fileName = "material_report.xlsx";
                contentType = MediaType
                        .parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            } else {
                reportBytes = reportService.generateMaterialReport();
                fileName = "material_report.pdf";
                contentType = MediaType.APPLICATION_PDF;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(contentType);
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(reportBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(reportBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @PostMapping("/generate/custom")
    public ResponseEntity<byte[]> generateCustomReport(@RequestBody Map<String, Object> params) {
        try {
            String reportType = (String) params.get("reportType");
            String format = (String) params.getOrDefault("format", "pdf");
            byte[] reportBytes = reportService.generateCustomReport(reportType, params);

            HttpHeaders headers = new HttpHeaders();
            String fileName;
            MediaType contentType;

            if ("excel".equals(format)) {
                fileName = "custom_report.xlsx";
                contentType = MediaType
                        .parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            } else {
                fileName = "custom_report.pdf";
                contentType = MediaType.APPLICATION_PDF;
            }

            headers.setContentType(contentType);
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(reportBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(reportBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(null);
        }
    }
}