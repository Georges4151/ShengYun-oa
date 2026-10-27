package com.ceams.controller;

import com.ceams.service.ReportService;
import com.ceams.model.entity.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/analytics")
public class AnalyticsController {

    @Autowired
    private ReportService reportService;

    /**
     * 多维度分析页面
     * @param model 模型
     * @return 多维度分析视图
     */
    @GetMapping("/multi-dimensional")
    public String multiDimensionalAnalysis(Model model) {
        // 获取分析维度
        List<String> dimensions = reportService.getAnalysisDimensions();
        model.addAttribute("dimensions", dimensions);
        return "analytics/multi-dimensional";
    }

    /**
     * 获取分析维度
     * @return 分析维度列表
     */
    @GetMapping("/dimensions")
    @ResponseBody
    public List<String> getAnalysisDimensions() {
        return reportService.getAnalysisDimensions();
    }

    /**
     * 获取多维度分析数据
     * @param dimensions 分析维度
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 分析数据
     */
    @PostMapping("/data")
    @ResponseBody
    public Map<String, Object> getAnalysisData(@RequestParam List<String> dimensions,
                                             @RequestParam(required = false) String startDate,
                                             @RequestParam(required = false) String endDate) {
        return reportService.getMultiDimensionalAnalysis(dimensions, startDate, endDate);
    }

    /**
     * 财务报表页面
     * @param model 模型
     * @return 财务报表视图
     */
    @GetMapping("/financial-reports")
    public String financialReports(Model model) {
        return "analytics/financial-reports";
    }

    /**
     * 获取合同支付报表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 合同支付报表数据
     */
    @GetMapping("/reports/contract-payments")
    @ResponseBody
    public Map<String, Object> getContractPaymentReport(@RequestParam(required = false) String startDate,
                                                      @RequestParam(required = false) String endDate) {
        return reportService.getContractPaymentReport(startDate, endDate);
    }

    /**
     * 获取现金流预测
     * @param months 预测月数
     * @return 现金流预测数据
     */
    @GetMapping("/reports/cash-flow")
    @ResponseBody
    public Map<String, Object> getCashFlowForecast(@RequestParam(defaultValue = "6") Integer months) {
        return reportService.getCashFlowForecast(months);
    }

    /**
     * 获取成本分析报表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 成本分析报表数据
     */
    @GetMapping("/reports/cost-analysis")
    @ResponseBody
    public Map<String, Object> getCostAnalysisReport(@RequestParam(required = false) String startDate,
                                                   @RequestParam(required = false) String endDate) {
        return reportService.getCostAnalysisReport(startDate, endDate);
    }

    /**
     * 生成自定义报表
     * @param reportParams 报表参数
     * @return 自定义报表数据
     */
    @PostMapping("/reports/custom")
    @ResponseBody
    public Map<String, Object> generateCustomReport(@RequestBody Map<String, Object> reportParams) {
        return reportService.generateCustomReport(reportParams);
    }

    /**
     * 导出报表
     * @param reportType 报表类型
     * @param format 导出格式
     * @param params 报表参数
     * @return 导出结果
     */
    @PostMapping("/reports/export")
    @ResponseBody
    public Map<String, Object> exportReport(@RequestParam String reportType,
                                          @RequestParam String format,
                                          @RequestBody Map<String, Object> params) {
        return reportService.exportReport(reportType, format, params);
    }
}
