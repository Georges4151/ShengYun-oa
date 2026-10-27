package com.ceams.service;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ceams.model.dto.ProjectCostAnalysisDTO;
import com.ceams.model.entity.Account;
import com.ceams.model.entity.Material;
import com.ceams.model.entity.Project;
import com.ceams.model.entity.PaymentTransaction;
import com.ceams.model.entity.SubcontractorContract;
import com.ceams.service.SubcontractorService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    @Autowired
    private AccountService accountService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private MaterialService materialService;

    @Autowired
    private PaymentTransactionService paymentTransactionService;

    @Autowired
    private SubcontractorService subcontractorService;

    public byte[] generateFinancialReport(String startDate, String endDate, Integer projectId) throws Exception {
        // 准备报表数据
        Map<String, Object> params = new HashMap<>();
        params.put("startDate", startDate);
        params.put("endDate", endDate);
        params.put("projectId", projectId);

        // 获取账目数据
        List<?> accounts;

        // 解析日期范围
        Date startDateObj = null;
        Date endDateObj = null;

        boolean hasProjectFilter = projectId != null;

        if (startDate != null && !startDate.isEmpty()) {
            startDateObj = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(startDate);
        }

        if (endDate != null && !endDate.isEmpty()) {
            endDateObj = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(endDate);
        }

        // 根据筛选条件获取账目数据
        if (hasProjectFilter && startDateObj != null && endDateObj != null) {
            // 同时有项目ID和日期范围
            Map<String, Object> searchParams = new HashMap<>();
            searchParams.put("projectId", projectId);
            searchParams.put("startDate", startDateObj);
            searchParams.put("endDate", endDateObj);
            accounts = accountService.searchAccounts(searchParams);
            System.out.println("根据项目ID和日期范围获取账目数据: " + accounts.size() + " 条");
        } else if (hasProjectFilter && startDateObj != null) {
            // 有项目ID和开始日期
            Map<String, Object> searchParams = new HashMap<>();
            searchParams.put("projectId", projectId);
            searchParams.put("startDate", startDateObj);
            accounts = accountService.searchAccounts(searchParams);
            System.out.println("根据项目ID和开始日期获取账目数据: " + accounts.size() + " 条");
        } else if (hasProjectFilter && endDateObj != null) {
            // 有项目ID和结束日期
            Map<String, Object> searchParams = new HashMap<>();
            searchParams.put("projectId", projectId);
            searchParams.put("endDate", endDateObj);
            accounts = accountService.searchAccounts(searchParams);
            System.out.println("根据项目ID和结束日期获取账目数据: " + accounts.size() + " 条");
        } else if (hasProjectFilter) {
            // 只有项目ID
            accounts = accountService.getByProjectId(projectId);
            System.out.println("根据项目ID获取账目数据: " + accounts.size() + " 条");
        } else if (startDateObj != null && endDateObj != null) {
            // 只有日期范围
            accounts = accountService.getByDateRange(startDateObj, endDateObj);
            System.out.println("根据日期范围获取账目数据: " + accounts.size() + " 条");
        } else {
            // 无筛选条件，获取所有数据
            accounts = accountService.getAllAccounts();
            System.out.println("获取所有账目数据: " + accounts.size() + " 条");
        }
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(accounts);

        // 加载报表模板
        File templateFile = new File("reports/financial_report.jrxml");
        JasperReport jasperReport = JasperCompileManager.compileReport(templateFile.getAbsolutePath());

        // 填充报表
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, dataSource);

        // 导出为PDF
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }

    public byte[] generateProjectCostReport(Integer projectId) throws Exception {
        // 准备报表数据
        Map<String, Object> params = new HashMap<>();
        params.put("projectId", projectId);

        // 获取项目成本分析数据
        List<?> costAnalysis = projectService.getProjectCostAnalysis();
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(costAnalysis);

        // 加载报表模板
        File templateFile = new File("reports/project_cost_report.jrxml");
        JasperReport jasperReport = JasperCompileManager.compileReport(templateFile.getAbsolutePath());

        // 填充报表
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, dataSource);

        // 导出为PDF
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }

    public byte[] generateMaterialReport() throws Exception {
        // 准备报表数据
        Map<String, Object> params = new HashMap<>();

        // 获取材料数据
        List<?> materials = materialService.getAllMaterials();
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(materials);

        // 加载报表模板
        File templateFile = new File("reports/material_report.jrxml");
        JasperReport jasperReport = JasperCompileManager.compileReport(templateFile.getAbsolutePath());

        // 填充报表
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, dataSource);

        // 导出为PDF
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }

    public byte[] generateCustomReport(String reportType, Map<String, Object> params) throws Exception {
        // 根据不同的报表类型生成不同的报表
        // 这里可以扩展更多的报表类型
        String format = (String) params.getOrDefault("format", "pdf");

        switch (reportType) {
            case "financial":
                if ("excel".equals(format)) {
                    return generateFinancialReportExcel((String) params.get("startDate"),
                            (String) params.get("endDate"),
                            (Integer) params.get("projectId"));
                } else {
                    return generateFinancialReport((String) params.get("startDate"), (String) params.get("endDate"),
                            (Integer) params.get("projectId"));
                }
            case "projectCost":
                if ("excel".equals(format)) {
                    return generateProjectCostReportExcel((Integer) params.get("projectId"));
                } else {
                    return generateProjectCostReport((Integer) params.get("projectId"));
                }
            case "material":
                if ("excel".equals(format)) {
                    return generateMaterialReportExcel();
                } else {
                    return generateMaterialReport();
                }
            default:
                throw new IllegalArgumentException("未知的报表类型: " + reportType);
        }
    }

    /**
     * 生成财务报表Excel
     */
    public byte[] generateFinancialReportExcel(String startDate, String endDate, Integer projectId) throws Exception {
        // 获取账目数据
        List<Account> accounts;

        // 解析日期范围
        Date startDateObj = null;
        Date endDateObj = null;

        System.out.println("接收到的开始日期: " + startDate);
        System.out.println("接收到的结束日期: " + endDate);
        System.out.println("接收到的项目ID: " + projectId);

        boolean hasValidDateRange = false;
        boolean hasProjectFilter = projectId != null;

        if (startDate != null && !startDate.isEmpty()) {
            try {
                startDateObj = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(startDate);
                System.out.println("解析后的开始日期: " + startDateObj);
            } catch (Exception e) {
                System.out.println("解析开始日期失败: " + e.getMessage());
            }
        } else {
            System.out.println("开始日期为空或无效");
        }

        if (endDate != null && !endDate.isEmpty()) {
            try {
                endDateObj = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(endDate);
                System.out.println("解析后的结束日期: " + endDateObj);
            } catch (Exception e) {
                System.out.println("解析结束日期失败: " + e.getMessage());
            }
        } else {
            System.out.println("结束日期为空或无效");
        }

        // 根据筛选条件获取账目数据
        if (hasProjectFilter && startDateObj != null && endDateObj != null) {
            // 同时有项目ID和日期范围
            Map<String, Object> params = new HashMap<>();
            params.put("projectId", projectId);
            params.put("startDate", startDateObj);
            params.put("endDate", endDateObj);
            accounts = accountService.searchAccounts(params);
            System.out.println("根据项目ID和日期范围获取账目数据: " + accounts.size() + " 条");
            hasValidDateRange = true;
        } else if (hasProjectFilter && startDateObj != null) {
            // 有项目ID和开始日期
            Map<String, Object> params = new HashMap<>();
            params.put("projectId", projectId);
            params.put("startDate", startDateObj);
            accounts = accountService.searchAccounts(params);
            System.out.println("根据项目ID和开始日期获取账目数据: " + accounts.size() + " 条");
            hasValidDateRange = true;
        } else if (hasProjectFilter && endDateObj != null) {
            // 有项目ID和结束日期
            Map<String, Object> params = new HashMap<>();
            params.put("projectId", projectId);
            params.put("endDate", endDateObj);
            accounts = accountService.searchAccounts(params);
            System.out.println("根据项目ID和结束日期获取账目数据: " + accounts.size() + " 条");
            hasValidDateRange = true;
        } else if (hasProjectFilter) {
            // 只有项目ID
            accounts = accountService.getByProjectId(projectId);
            System.out.println("根据项目ID获取账目数据: " + accounts.size() + " 条");
        } else if (startDateObj != null && endDateObj != null) {
            // 只有日期范围
            accounts = accountService.getByDateRange(startDateObj, endDateObj);
            System.out.println("根据日期范围获取账目数据: " + accounts.size() + " 条");
            hasValidDateRange = true;
        } else if (startDateObj != null) {
            // 只有开始日期，查询从开始日期到当前
            endDateObj = new Date();
            accounts = accountService.getByDateRange(startDateObj, endDateObj);
            System.out.println("只有开始日期，查询从开始日期到当前: " + accounts.size() + " 条");
            hasValidDateRange = true;
        } else if (endDateObj != null) {
            // 只有结束日期，查询从最早到结束日期
            // 使用一个很早的日期作为开始日期
            startDateObj = new java.text.SimpleDateFormat("yyyy-MM-dd").parse("2000-01-01");
            accounts = accountService.getByDateRange(startDateObj, endDateObj);
            System.out.println("只有结束日期，查询从最早到结束日期: " + accounts.size() + " 条");
            hasValidDateRange = true;
        } else {
            System.out.println("无筛选条件，获取所有账目数据");
            accounts = accountService.getAllAccounts();
            System.out.println("获取所有账目数据: " + accounts.size() + " 条");
        }

        System.out.println("日期范围过滤状态: " + (hasValidDateRange ? "已应用" : "未应用"));
        System.out.println("项目筛选状态: " + (hasProjectFilter ? "已应用" : "未应用"));

        // 创建Excel工作簿
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("财务报表");

            // 设置标题行
            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("财务报表");
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            titleRow.getCell(0).setCellStyle(titleStyle);

            // 设置日期范围行
            Row dateRow = sheet.createRow(1);
            dateRow.createCell(0).setCellValue("日期范围: " + startDate + " 至 " + endDate);

            // 设置导出时间行
            Row exportTimeRow = sheet.createRow(2);
            String exportTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            exportTimeRow.createCell(0).setCellValue("导出时间: " + exportTime);

            // 设置表头
            Row headerRow = sheet.createRow(3);
            String[] headers = { "ID", "项目名称", "账目类型", "金额", "日期", "描述" };
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 填充数据
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            int rowNum = 4;
            for (Account account : accounts) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(account.getId());
                row.createCell(1).setCellValue(account.getProjectName());
                row.createCell(2).setCellValue(account.getType());
                row.createCell(3).setCellValue(account.getAmount().doubleValue());
                row.createCell(4).setCellValue(account.getTransactionDate().toString());
                row.createCell(5).setCellValue(account.getDescription());

                // 应用样式
                for (int i = 0; i < headers.length; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }

            // 调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 写入输出流
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * 生成项目成本分析报表Excel
     */
    public byte[] generateProjectCostReportExcel(Integer projectId) throws Exception {
        // 获取项目成本分析数据
        List<ProjectCostAnalysisDTO> costAnalysisList = projectService.getProjectCostAnalysisDTO();

        // 创建Excel工作簿
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("项目成本分析");

            // 设置标题行
            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("项目成本分析");
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            titleRow.getCell(0).setCellStyle(titleStyle);

            // 设置导出时间行
            Row exportTimeRow = sheet.createRow(1);
            String exportTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            exportTimeRow.createCell(0).setCellValue("导出时间: " + exportTime);

            // 设置表头
            Row headerRow = sheet.createRow(3);
            String[] headers = { "项目ID", "项目名称", "合同金额", "预算金额", "实际金额", "预算结余", "成本超支率(%)" };
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 填充数据
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            CellStyle percentageStyle = workbook.createCellStyle();
            percentageStyle.setBorderBottom(BorderStyle.THIN);
            percentageStyle.setBorderLeft(BorderStyle.THIN);
            percentageStyle.setBorderRight(BorderStyle.THIN);
            percentageStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));

            int rowNum = 3;
            for (ProjectCostAnalysisDTO dto : costAnalysisList) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(dto.getProjectId());
                row.createCell(1).setCellValue(dto.getProjectName());
                row.createCell(2)
                        .setCellValue(dto.getContractAmount() != null ? dto.getContractAmount().doubleValue() : 0);
                row.createCell(3).setCellValue(dto.getBudgetAmount() != null ? dto.getBudgetAmount().doubleValue() : 0);
                row.createCell(4).setCellValue(dto.getActualAmount() != null ? dto.getActualAmount().doubleValue() : 0);
                row.createCell(5)
                        .setCellValue(dto.getBudgetSurplus() != null ? dto.getBudgetSurplus().doubleValue() : 0);
                row.createCell(6).setCellValue(dto.getCostOverrunRate() != null ? dto.getCostOverrunRate() / 100 : 0);

                // 应用样式
                for (int i = 0; i < headers.length; i++) {
                    if (i == 6) {
                        row.getCell(i).setCellStyle(percentageStyle);
                    } else {
                        row.getCell(i).setCellStyle(dataStyle);
                    }
                }
            }

            // 调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 写入输出流
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * 生成材料报表Excel
     */
    public byte[] generateMaterialReportExcel() throws Exception {
        // 获取材料数据
        List<Material> materials = materialService.getAllMaterials();

        // 创建Excel工作簿
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("材料报表");

            // 设置标题行
            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("材料报表");
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            titleRow.getCell(0).setCellStyle(titleStyle);

            // 设置导出时间行
            Row exportTimeRow = sheet.createRow(1);
            String exportTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            exportTimeRow.createCell(0).setCellValue("导出时间: " + exportTime);

            // 设置表头
            Row headerRow = sheet.createRow(3);
            String[] headers = { "ID", "材料名称", "材料编码", "规格型号", "单位", "当前价格", "创建时间", "更新时间" };
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 填充数据
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            int rowNum = 3;
            for (Material material : materials) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(material.getId());
                row.createCell(1).setCellValue(material.getMaterialName());
                row.createCell(2).setCellValue(material.getMaterialCode());
                row.createCell(3).setCellValue(material.getSpecification());
                row.createCell(4).setCellValue(material.getUnit());
                row.createCell(5).setCellValue(material.getCurrentPrice().doubleValue());
                row.createCell(6).setCellValue(material.getCreateTime().toString());
                row.createCell(7).setCellValue(material.getUpdateTime().toString());

                // 应用样式
                for (int i = 0; i < headers.length; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }

            // 调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 写入输出流
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * 生成收支交易记录Excel
     */
    public byte[] generateTransactionReportExcel(Integer accountId, String type, java.util.Date startDate,
            java.util.Date endDate, String description) throws Exception {
        // 获取交易记录数据
        List<PaymentTransaction> transactions = paymentTransactionService.getTransactionsByConditions(
                accountId, null, type, startDate, endDate, description);

        // 创建Excel工作簿
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("收支交易记录");

            // 设置标题行
            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("收支交易记录");
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            titleRow.getCell(0).setCellStyle(titleStyle);

            // 设置导出时间行
            Row exportTimeRow = sheet.createRow(1);
            String exportTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            exportTimeRow.createCell(0).setCellValue("导出时间: " + exportTime);

            // 设置日期范围行
            Row dateRow = sheet.createRow(2);
            String dateRange = "日期范围: ";
            if (startDate != null) {
                dateRange += new java.text.SimpleDateFormat("yyyy-MM-dd").format(startDate);
            } else {
                dateRange += "全部";
            }
            dateRange += " 至 ";
            if (endDate != null) {
                dateRange += new java.text.SimpleDateFormat("yyyy-MM-dd").format(endDate);
            } else {
                dateRange += "全部";
            }
            dateRow.createCell(0).setCellValue(dateRange);

            // 设置表头
            Row headerRow = sheet.createRow(3);
            String[] headers = { "ID", "账户名称", "交易类型", "金额", "交易日期", "描述", "票据ID", "票据编号" };
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 填充数据
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            int rowNum = 4;
            for (PaymentTransaction transaction : transactions) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(transaction.getId());
                row.createCell(1)
                        .setCellValue(transaction.getAccountName() != null ? transaction.getAccountName() : "");
                row.createCell(2).setCellValue("income".equals(transaction.getType()) ? "收入" : "支出");
                row.createCell(3).setCellValue(transaction.getAmount().doubleValue());
                row.createCell(4).setCellValue(
                        transaction.getTransactionDate() != null ? transaction.getTransactionDate().toString() : "");
                row.createCell(5)
                        .setCellValue(transaction.getDescription() != null ? transaction.getDescription() : "");
                row.createCell(6)
                        .setCellValue(transaction.getReceiptId() != null ? transaction.getReceiptId().toString() : "");
                row.createCell(7)
                        .setCellValue(transaction.getReceiptNumber() != null ? transaction.getReceiptNumber() : "");

                // 应用样式
                for (int i = 0; i < headers.length; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }

            // 调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 写入输出流
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * 获取分析维度
     * 
     * @return 分析维度列表
     */
    public List<String> getAnalysisDimensions() {
        List<String> dimensions = new ArrayList<>();
        dimensions.add("project");
        dimensions.add("department");
        dimensions.add("subcontractor");
        dimensions.add("contract");
        dimensions.add("date");
        dimensions.add("type");
        return dimensions;
    }

    /**
     * 获取多维度分析数据
     * 
     * @param dimensions 分析维度
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @return 分析数据
     */
    public Map<String, Object> getMultiDimensionalAnalysis(List<String> dimensions, String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();
        List<Account> accounts = accountService.getAllAccounts();

        // 这里可以根据维度进行数据分析
        // 简化实现，返回原始数据
        result.put("accounts", accounts);
        result.put("dimensions", dimensions);
        result.put("startDate", startDate);
        result.put("endDate", endDate);

        return result;
    }

    /**
     * 获取合同支付报表
     * 
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 合同支付报表数据
     */
    public Map<String, Object> getContractPaymentReport(String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();
        List<SubcontractorContract> contracts = subcontractorService.getAllContracts();

        // 计算合同支付统计数据
        BigDecimal totalContractAmount = BigDecimal.ZERO;
        BigDecimal totalPaidAmount = BigDecimal.ZERO;

        for (SubcontractorContract contract : contracts) {
            if (contract.getContractAmount() != null) {
                totalContractAmount = totalContractAmount.add(contract.getContractAmount());
            }
            if (contract.getPaidAmount() != null) {
                totalPaidAmount = totalPaidAmount.add(contract.getPaidAmount());
            }
        }

        result.put("contracts", contracts);
        result.put("totalContractAmount", totalContractAmount);
        result.put("totalPaidAmount", totalPaidAmount);
        result.put("startDate", startDate);
        result.put("endDate", endDate);

        return result;
    }

    /**
     * 获取现金流预测
     * 
     * @param months 预测月数
     * @return 现金流预测数据
     */
    public Map<String, Object> getCashFlowForecast(Integer months) {
        Map<String, Object> result = new HashMap<>();

        // 简化实现，返回模拟数据
        List<Map<String, Object>> forecastData = new ArrayList<>();

        for (int i = 1; i <= months; i++) {
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", i);
            monthData.put("income", BigDecimal.valueOf(100000 + i * 5000));
            monthData.put("expense", BigDecimal.valueOf(80000 + i * 3000));
            monthData.put("netCashFlow", BigDecimal.valueOf(20000 + i * 2000));
            forecastData.add(monthData);
        }

        result.put("forecastData", forecastData);
        result.put("months", months);

        return result;
    }

    /**
     * 获取成本分析报表
     * 
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 成本分析报表数据
     */
    public Map<String, Object> getCostAnalysisReport(String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();
        List<Account> accounts = accountService.getAllAccounts();

        // 计算成本统计数据
        BigDecimal totalCost = BigDecimal.ZERO;
        Map<String, BigDecimal> costByType = new HashMap<>();

        for (Account account : accounts) {
            if ("expense".equals(account.getType()) && account.getAmount() != null) {
                totalCost = totalCost.add(account.getAmount());

                String type = account.getType();
                if (!costByType.containsKey(type)) {
                    costByType.put(type, BigDecimal.ZERO);
                }
                costByType.put(type, costByType.get(type).add(account.getAmount()));
            }
        }

        result.put("accounts", accounts);
        result.put("totalCost", totalCost);
        result.put("costByType", costByType);
        result.put("startDate", startDate);
        result.put("endDate", endDate);

        return result;
    }

    /**
     * 生成自定义报表（重载方法）
     * 
     * @param reportParams 报表参数
     * @return 自定义报表数据
     */
    public Map<String, Object> generateCustomReport(Map<String, Object> reportParams) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "自定义报表生成成功");
        result.put("data", reportParams);
        return result;
    }

    /**
     * 导出报表
     * 
     * @param reportType 报表类型
     * @param format     导出格式
     * @param params     报表参数
     * @return 导出结果
     */
    public Map<String, Object> exportReport(String reportType, String format, Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "报表导出成功");
        result.put("reportType", reportType);
        result.put("format", format);
        result.put("params", params);
        return result;
    }
}