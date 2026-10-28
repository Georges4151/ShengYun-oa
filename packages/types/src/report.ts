/**
 * 跨域只读报表（report）类型：财务 / 项目成本 / 材料 / Dashboard。
 *
 * 字段建模规则（与 docs/openapi/report.openapi.yaml components/schemas 严格一致）：
 * - openapi `required` 中的字段 → 必填 `field: Type`
 * - 其余字段 → 可选 `field?: Type`
 * - `nullable: true` 且非必填 → 可选可空 `field?: Type | null`
 * - Id = string、金额用 Money（string）、时间用 DateTime（ISO8601）。
 */

import type { DateTime, Id, Money } from './common'

// ---------- 枚举 ----------
export type FinancialSourceType = 'ACCOUNT' | 'PAYMENT_TRANSACTION' | 'BANK_STATEMENT' | 'GENERAL_LEDGER'
export type IncomeExpenseType = 'INCOME' | 'EXPENSE'

// ---------- 财务报表 ----------
export interface FinancialReportItemVO {
  id: Id
  sourceType: FinancialSourceType
  projectId?: Id | null
  projectName?: string | null
  accountId?: Id | null
  accountName?: string | null
  transactionDate?: DateTime | null
  type: IncomeExpenseType
  amount: Money
  description?: string | null
}

export interface FinancialReportVO {
  totalIncome: Money
  totalExpense: Money
  netAmount: Money
  items: FinancialReportItemVO[]
}

export interface FinancialReportQuery {
  /** 月份（YYYY-MM） */
  month?: string
  projectId?: Id
  accountId?: Id
}

// ---------- 项目成本报表 ----------
export interface ProjectCostItemVO {
  projectId: Id
  projectName: string
  contractAmount?: Money | null
  budgetAmount?: Money | null
  changedAmount?: Money | null
  paidAmount?: Money | null
  allocatedAmount?: Money | null
  actualAmount?: Money | null
}

export interface ProjectCostReportVO {
  items: ProjectCostItemVO[]
  totalBudget: Money
  totalChanged: Money
  totalPaid: Money
  totalAllocated: Money
}

export interface ProjectCostReportQuery {
  projectId?: Id
}

// ---------- 材料报表 ----------
export interface MaterialReportItemVO {
  materialId: Id
  materialCode: string
  materialName: string
  specification?: string | null
  unit?: string | null
  currentPrice?: Money | null
  transactionCount: number
  totalAmount: Money
}

export interface MaterialReportVO {
  items: MaterialReportItemVO[]
  totalCount: number
  totalAmount: Money
}

export interface MaterialReportQuery {
  materialId?: Id
  categoryId?: Id
}

// ---------- Dashboard ----------
export interface DashboardVO {
  pendingReceiptCount: number
  pendingSealApplicationCount: number
  unreadNotificationCount: number
  projectCount: number
  inProgressProjectCount: number
  monthIncome: Money
  monthExpense: Money
  totalIncome: Money
  totalExpense: Money
}
