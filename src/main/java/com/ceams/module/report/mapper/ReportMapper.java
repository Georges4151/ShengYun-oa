package com.ceams.module.report.mapper;

import com.ceams.module.report.vo.DashboardVO;
import com.ceams.module.report.vo.FinancialReportItemVO;
import com.ceams.module.report.vo.MaterialReportItemVO;
import com.ceams.module.report.vo.ProjectCostItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 跨域只读报表 Mapper：仅 SELECT，不开写事务。
 *
 * <p>报表为多表聚合，统一走 MyBatis XML（{@code mapper/report/ReportMapper.xml}）。</p>
 */
@Mapper
public interface ReportMapper {

    /**
     * 财务报表：合并收支流水 / 付款交易 / 银行流水 / 总账。
     */
    List<FinancialReportItemVO> selectFinancialItems(@Param("month") String month,
                                                     @Param("projectId") Long projectId,
                                                     @Param("accountId") Long accountId);

    /**
     * 项目成本报表：项目 + 预算 + 变更 + 进度款 + 成本分摊。
     */
    List<ProjectCostItemVO> selectProjectCostItems(@Param("projectId") Long projectId);

    /**
     * 材料报表：材料库 + 关联收支流水。
     */
    List<MaterialReportItemVO> selectMaterialItems(@Param("materialId") Long materialId,
                                                   @Param("categoryId") Long categoryId);

    /**
     * Dashboard 概览：待办数 / 未读通知 / 项目进度 / 近期收支汇总。
     */
    DashboardVO selectDashboard(@Param("userId") Long userId, @Param("month") String month);
}
