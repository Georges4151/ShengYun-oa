/**
 * 报销域（expense）类型化 API 工厂。
 * 所有路径前缀统一 `/api/v1`，与 docs/openapi/expense.openapi.yaml paths 一致。
 * 审批（同意/驳回/核验）已全部收口到 BPM，本域只保留业务数据维护与上报。
 */

import type { HttpClient } from '../http-client'
import type {
  Id,
  ReceiptVO,
  ReceiptCreateRequest,
  ReceiptUpdateRequest,
  ReceiptPageQuery,
  ApprovalRecordVO,
  ReceiptVerificationVO,
  AmountRuleVO,
  AmountRuleSaveRequest,
  AmountRulePageQuery,
  PageResult
} from '@ceams/types'

export interface ExpenseApi {
  receiptPage(query?: ReceiptPageQuery): Promise<PageResult<ReceiptVO>>
  receiptDetail(id: Id): Promise<ReceiptVO>
  receiptCreate(body: ReceiptCreateRequest): Promise<Id>
  receiptUpdate(id: Id, body: ReceiptUpdateRequest): Promise<void>
  receiptRemove(id: Id): Promise<void>
  receiptSubmit(id: Id): Promise<void>
  /** 历史审批记录（只读，旧审批链路已下线，仅存量追溯）。 */
  receiptApprovals(id: Id): Promise<ApprovalRecordVO[]>
  receiptVerifications(id: Id): Promise<ReceiptVerificationVO[]>

  // 报销额度分级规则：按金额区间匹配审批流程模板
  amountRuleList(query?: AmountRulePageQuery): Promise<PageResult<AmountRuleVO>>
  amountRuleCreate(body: AmountRuleSaveRequest): Promise<Id>
  amountRuleUpdate(id: Id, body: AmountRuleSaveRequest): Promise<void>
  amountRuleRemove(id: Id): Promise<void>
}

export function createExpenseApi(client: HttpClient): ExpenseApi {
  return {
    async receiptPage(query = {}) {
      return client.get<PageResult<ReceiptVO>>('/api/v1/expenses', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          receiptNumber: query.receiptNumber,
          userId: query.userId,
          projectId: query.projectId,
          verificationStatus: query.verificationStatus,
          paymentStatus: query.paymentStatus,
          approvalStatus: query.approvalStatus
        }
      })
    },
    async receiptDetail(id) {
      return client.get<ReceiptVO>(`/api/v1/expenses/${id}`)
    },
    async receiptCreate(body) {
      return client.post<Id>('/api/v1/expenses', body)
    },
    async receiptUpdate(id, body) {
      return client.put<void>(`/api/v1/expenses/${id}`, body)
    },
    async receiptRemove(id) {
      return client.delete<void>(`/api/v1/expenses/${id}`)
    },
    async receiptSubmit(id) {
      return client.post<void>(`/api/v1/expenses/${id}/submit`)
    },
    async receiptApprovals(id) {
      return client.get<ApprovalRecordVO[]>(`/api/v1/expenses/${id}/approvals`)
    },
    async receiptVerifications(id) {
      return client.get<ReceiptVerificationVO[]>(`/api/v1/expenses/${id}/verifications`)
    },
    async amountRuleList(query = {}) {
      return client.get<PageResult<AmountRuleVO>>('/api/v1/expense/amount-rules', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 100,
          keyword: query.keyword
        }
      })
    },
    async amountRuleCreate(body) {
      return client.post<Id>('/api/v1/expense/amount-rules', body)
    },
    async amountRuleUpdate(id, body) {
      return client.put<void>(`/api/v1/expense/amount-rules/${id}`, body)
    },
    async amountRuleRemove(id) {
      return client.delete<void>(`/api/v1/expense/amount-rules/${id}`)
    }
  }
}
