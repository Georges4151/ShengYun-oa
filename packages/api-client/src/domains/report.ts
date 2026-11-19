/**
 * 跨域只读报表（report）类型化 API 工厂。
 * 所有路径前缀统一 `/api/v1`，与 docs/openapi/report.openapi.yaml paths 一致。
 * 均为只读 GET，不挂 HMAC 签名。
 */

import type { HttpClient } from '../http-client'
import type {
  AdminAssetReportQuery,
  AdminAssetReportVO,
  BpmEfficiencyQuery,
  BpmEfficiencyReportVO,
  DashboardVO,
  FinancialReportQuery,
  FinancialReportVO,
  HrCostQuery,
  HrCostReportVO,
  HrPerformanceQuery,
  HrPerformanceReportVO,
  MaterialReportQuery,
  MaterialReportVO,
  ProjectCostReportQuery,
  ProjectCostReportVO
} from '@ceams/types'

export interface ReportApi {
  financial(query?: FinancialReportQuery): Promise<FinancialReportVO>
  projectCost(query?: ProjectCostReportQuery): Promise<ProjectCostReportVO>
  material(query?: MaterialReportQuery): Promise<MaterialReportVO>
  dashboard(): Promise<DashboardVO>
  hrCost(query: HrCostQuery): Promise<HrCostReportVO>
  hrPerformance(query: HrPerformanceQuery): Promise<HrPerformanceReportVO>
  adminAssets(query?: AdminAssetReportQuery): Promise<AdminAssetReportVO>
  bpmEfficiency(query?: BpmEfficiencyQuery): Promise<BpmEfficiencyReportVO>
}

export function createReportApi(client: HttpClient): ReportApi {
  return {
    async financial(query = {}) {
      return client.get<FinancialReportVO>('/api/v1/reports/financial', {
        query: {
          month: query.month,
          projectId: query.projectId,
          accountId: query.accountId
        }
      })
    },
    async projectCost(query = {}) {
      return client.get<ProjectCostReportVO>('/api/v1/reports/project-cost', {
        query: { projectId: query.projectId }
      })
    },
    async material(query = {}) {
      return client.get<MaterialReportVO>('/api/v1/reports/material', {
        query: {
          materialId: query.materialId,
          categoryId: query.categoryId
        }
      })
    },
    async dashboard() {
      return client.get<DashboardVO>('/api/v1/reports/dashboard')
    },
    async hrCost(query) {
      return client.get<HrCostReportVO>('/api/v1/reports/hr-cost', {
        query: {
          month: query.month,
          deptId: query.deptId
        }
      })
    },
    async hrPerformance(query) {
      return client.get<HrPerformanceReportVO>('/api/v1/reports/hr-performance', {
        query: {
          period: query.period,
          deptId: query.deptId
        }
      })
    },
    async adminAssets(query = {}) {
      return client.get<AdminAssetReportVO>('/api/v1/reports/admin-assets', {
        query: { category: query.category }
      })
    },
    async bpmEfficiency(query = {}) {
      return client.get<BpmEfficiencyReportVO>('/api/v1/reports/bpm-efficiency', {
        query: {
          start: query.start,
          end: query.end,
          bizType: query.bizType
        }
      })
    }
  }
}
