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

// ---------- 人力成本报表（P2） ----------
export interface HrCostDeptVO {
  /** 部门 ID；未分配部门为 null */
  deptId?: Id | null
  deptName: string
  headcount: number
  payrollHeadcount: number
  grossPay: Money
  netPay: Money
  socialInsurance: Money
  housingFund: Money
  tax: Money
  avgNetPay: Money
}

export interface HrCostReportVO {
  month: string
  headcount: number
  payrollHeadcount: number
  grossPay: Money
  netPay: Money
  socialInsurance: Money
  housingFund: Money
  tax: Money
  avgNetPay: Money
  byDept: HrCostDeptVO[]
}

export interface HrCostQuery {
  /** 核算月（YYYY-MM），必填 */
  month: string
  deptId?: Id
}

// ---------- 绩效考核报表（P2） ----------
export interface ScoreDistributionVO {
  /** A 档（≥ 90） */
  a: number
  /** B 档（80 ~ 89.99） */
  b: number
  /** C 档（70 ~ 79.99） */
  c: number
  /** D 档（< 70） */
  d: number
}

export interface HrPerformanceDeptVO {
  deptId?: Id | null
  deptName: string
  reviewCount: number
  finalizedCount: number
  avgFinalScore: Money
}

export interface HrPerformanceReportVO {
  period: string
  reviewCount: number
  finalizedCount: number
  avgFinalScore: Money
  distribution: ScoreDistributionVO
  byDept: HrPerformanceDeptVO[]
}

export interface HrPerformanceQuery {
  /** 考核周期（YYYY-MM 或 YYYY-Qn），必填 */
  period: string
  deptId?: Id
}

// ---------- 行政资产报表（P2） ----------
export interface AdminAssetCategoryStatVO {
  category: string
  count: number
  totalValue: Money
}

export interface AdminAssetReportVO {
  totalCount: number
  totalValue: Money
  byStatus: Record<string, number>
  byCategory: AdminAssetCategoryStatVO[]
  borrowingCount: number
  overdueCount: number
  /** 借用率（0~1 的小数字符串，4 位小数） */
  borrowRate: Money
}

export interface AdminAssetReportQuery {
  /** ELECTRONIC/FURNITURE/VEHICLE_TOOL/OTHER */
  category?: string
}

// ---------- 审批效率报表（P2） ----------
export interface BpmBizTypeStatVO {
  bizType: string
  instanceCount: number
  approvedCount: number
  avgDurationHours: Money
}

export interface BpmNodeBacklogVO {
  nodeKey: string
  pendingCount: number
}

export interface BpmEfficiencyReportVO {
  instanceCount: number
  approvedCount: number
  avgDurationHours: Money
  overdueTaskCount: number
  /** 超时率（0~1 的小数字符串，4 位小数） */
  overdueRate: Money
  byBizType: BpmBizTypeStatVO[]
  backlogByNode: BpmNodeBacklogVO[]
}

export interface BpmEfficiencyQuery {
  /** 起始日期（YYYY-MM-DD） */
  start?: string
  /** 结束日期（YYYY-MM-DD） */
  end?: string
  bizType?: string
}
