package com.ceams.module.report.service;

import com.ceams.module.report.dto.FinancialReportQuery;
import com.ceams.module.report.dto.MaterialReportQuery;
import com.ceams.module.report.dto.ProjectCostReportQuery;
import com.ceams.module.report.vo.DashboardVO;
import com.ceams.module.report.vo.FinancialReportVO;
import com.ceams.module.report.vo.MaterialReportVO;
import com.ceams.module.report.vo.ProjectCostReportVO;

/** 跨域只读报表服务（report 模块，不单独成域、不开写事务）。 */
public interface ReportService {

    FinancialReportVO financial(FinancialReportQuery query);

    ProjectCostReportVO projectCost(ProjectCostReportQuery query);

    MaterialReportVO material(MaterialReportQuery query);

    DashboardVO dashboard();
}
