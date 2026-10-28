/**
 * 跨域只读报表（report）类型化 API 工厂。
 * 所有路径前缀统一 `/api/v1`，与 docs/openapi/report.openapi.yaml paths 一致。
 * 均为只读 GET，不挂 HMAC 签名。
 */

import type { HttpClient } from '../http-client'
import type {
  DashboardVO,
  FinancialReportQuery,
  FinancialReportVO,
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
    }
  }
}
