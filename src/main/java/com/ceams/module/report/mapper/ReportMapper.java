package com.ceams.module.report.mapper;

import com.ceams.module.report.vo.AdminAssetCategoryRowVO;
import com.ceams.module.report.vo.AdminAssetStatsRowVO;
import com.ceams.module.report.vo.AdminBorrowStatsRowVO;
import com.ceams.module.report.vo.BpmBizTypeRowVO;
import com.ceams.module.report.vo.BpmEfficiencySummaryRowVO;
import com.ceams.module.report.vo.BpmNodeBacklogVO;
import com.ceams.module.report.vo.DashboardVO;
import com.ceams.module.report.vo.FinancialReportItemVO;
import com.ceams.module.report.vo.HrPerformanceDeptRowVO;
import com.ceams.module.report.vo.MaterialReportItemVO;
import com.ceams.module.report.vo.ProjectCostItemVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 跨域只读报表 Mapper：仅 SELECT，不开写事务。
 *
 * <p>报表为多表聚合，统一走 MyBatis XML（{@code mapper/report/ReportMapper.xml}）。
 */
@Mapper
public interface ReportMapper {

    /** 财务报表：合并收支流水 / 付款交易 / 银行流水 / 总账。 */
    List<FinancialReportItemVO> selectFinancialItems(
            @Param("month") String month,
            @Param("projectId") Long projectId,
            @Param("accountId") Long accountId);

    /** 项目成本报表：项目 + 预算 + 变更 + 进度款 + 成本分摊。 */
    List<ProjectCostItemVO> selectProjectCostItems(@Param("projectId") Long projectId);

    /** 材料报表：材料库 + 关联收支流水。 */
    List<MaterialReportItemVO> selectMaterialItems(
            @Param("materialId") Long materialId, @Param("categoryId") Long categoryId);

    /** Dashboard 概览：待办数 / 未读通知 / 项目进度 / 近期收支汇总。 */
    DashboardVO selectDashboard(@Param("userId") Long userId, @Param("month") String month);

    /** 绩效考核报表：按部门分组统计评审数 / 定稿数 / 定稿分合计与分数分布。 */
    List<HrPerformanceDeptRowVO> selectHrPerformanceByDept(
            @Param("period") String period, @Param("deptId") Long deptId);

    /** 行政资产报表：总量、购置金额与状态分布。 */
    AdminAssetStatsRowVO selectAdminAssetStats(@Param("category") String category);

    /** 行政资产报表：按分类统计数量与购置金额。 */
    List<AdminAssetCategoryRowVO> selectAdminAssetByCategory(@Param("category") String category);

    /** 行政资产报表：借用中与逾期未还记录数。 */
    AdminBorrowStatsRowVO selectAdminBorrowStats(@Param("category") String category);

    /** 审批效率报表：实例量 / 通过量 / 平均时长 / 任务量与超时任务量。 */
    BpmEfficiencySummaryRowVO selectBpmEfficiencySummary(
            @Param("start") String start,
            @Param("end") String end,
            @Param("bizType") String bizType);

    /** 审批效率报表：按业务类型分组统计。 */
    List<BpmBizTypeRowVO> selectBpmEfficiencyByBizType(
            @Param("start") String start,
            @Param("end") String end,
            @Param("bizType") String bizType);

    /** 审批效率报表：待办任务按节点编码分组统计。 */
    List<BpmNodeBacklogVO> selectBpmBacklogByNode(
            @Param("start") String start,
            @Param("end") String end,
            @Param("bizType") String bizType);
}
