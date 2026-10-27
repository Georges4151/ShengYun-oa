package com.ceams.service;

import java.util.List;
import java.util.Map;

public interface ReportServiceInterface {
    /**
     * 获取分析维度
     * @return 分析维度列表
     */
    List<String> getAnalysisDimensions();

    /**
     * 获取多维度分析数据
     * @param dimensions 分析维度
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 分析数据
     */
    Map<String, Object> getMultiDimensionalAnalysis(List<String> dimensions, String startDate, String endDate);

    /**
     * 获取合同支付报表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 合同支付报表数据
     */
    Map<String, Object> getContractPaymentReport(String startDate, String endDate);

    /**
     * 获取现金流预测
     * @param months 预测月数
     * @return 现金流预测数据
     */
    Map<String, Object> getCashFlowForecast(Integer months);

    /**
     * 获取成本分析报表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 成本分析报表数据
     */
    Map<String, Object> getCostAnalysisReport(String startDate, String endDate);

    /**
     * 生成自定义报表
     * @param reportParams 报表参数
     * @return 自定义报表数据
     */
    Map<String, Object> generateCustomReport(Map<String, Object> reportParams);

    /**
     * 导出报表
     * @param reportType 报表类型
     * @param format 导出格式
     * @param params 报表参数
     * @return 导出结果
     */
    Map<String, Object> exportReport(String reportType, String format, Map<String, Object> params);
}
