package com.ceams.module.report.service;

import com.ceams.module.report.dto.AdminAssetReportQuery;
import com.ceams.module.report.dto.BpmEfficiencyQuery;
import com.ceams.module.report.dto.FinancialReportQuery;
import com.ceams.module.report.dto.HrCostQuery;
import com.ceams.module.report.dto.HrPerformanceQuery;
import com.ceams.module.report.dto.MaterialReportQuery;
import com.ceams.module.report.dto.ProjectCostReportQuery;
import com.ceams.module.report.vo.AdminAssetReportVO;
import com.ceams.module.report.vo.BpmEfficiencyReportVO;
import com.ceams.module.report.vo.DashboardVO;
import com.ceams.module.report.vo.FinancialReportVO;
import com.ceams.module.report.vo.HrCostReportVO;
import com.ceams.module.report.vo.HrPerformanceReportVO;
import com.ceams.module.report.vo.MaterialReportVO;
import com.ceams.module.report.vo.ProjectCostReportVO;

/** 跨域只读报表服务（report 模块，不单独成域、不开写事务）。 */
public interface ReportService {

    FinancialReportVO financial(FinancialReportQuery query);

    ProjectCostReportVO projectCost(ProjectCostReportQuery query);

    MaterialReportVO material(MaterialReportQuery query);

    DashboardVO dashboard();

    /** 人力成本报表：工资口径金额 + 员工口径在职人数，按部门合并。 */
    HrCostReportVO hrCost(HrCostQuery query);

    /** 绩效考核报表：评审量 / 定稿量 / 平均分 / 分数分布。 */
    HrPerformanceReportVO hrPerformance(HrPerformanceQuery query);

    /** 行政资产报表：总量 / 状态分布 / 分类统计 / 借用率。 */
    AdminAssetReportVO adminAssets(AdminAssetReportQuery query);

    /** 审批效率报表：实例量 / 通过量 / 平均时长 / 超时率 / 节点积压。 */
    BpmEfficiencyReportVO bpmEfficiency(BpmEfficiencyQuery query);
}
