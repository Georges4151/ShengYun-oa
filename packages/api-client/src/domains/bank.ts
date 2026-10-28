/**
 * 对账域（bank）类型化 API 工厂。
 * 所有路径前缀统一 `/api/v1`，与 docs/openapi/bank.openapi.yaml paths 一致。
 * 流水导入接口使用 multipart/FormData。
 */

import type { HttpClient } from '../http-client'
import type {
  Id,
  PageResult,
  BankStatementVO,
  BankStatementCreateRequest,
  BankStatementUpdateRequest,
  BankStatementPageQuery,
  StatementImportResultVO,
  GeneralLedgerVO,
  GeneralLedgerCreateRequest,
  GeneralLedgerUpdateRequest,
  GeneralLedgerPageQuery,
  ReconciliationResultVO,
  ReconciliationMatchRequest,
  ReconciliationResolveRequest,
  ReconciliationMatchResultVO,
  ReconciliationResultPageQuery
} from '@ceams/types'

export interface BankApi {
  statementPage(query?: BankStatementPageQuery): Promise<PageResult<BankStatementVO>>
  statementDetail(id: Id): Promise<BankStatementVO>
  statementCreate(body: BankStatementCreateRequest): Promise<Id>
  statementUpdate(id: Id, body: BankStatementUpdateRequest): Promise<void>
  statementRemove(id: Id): Promise<void>
  statementImport(accountId: Id, file: Blob, fileName?: string): Promise<StatementImportResultVO>

  ledgerPage(query?: GeneralLedgerPageQuery): Promise<PageResult<GeneralLedgerVO>>
  ledgerDetail(id: Id): Promise<GeneralLedgerVO>
  ledgerCreate(body: GeneralLedgerCreateRequest): Promise<Id>
  ledgerUpdate(id: Id, body: GeneralLedgerUpdateRequest): Promise<void>
  ledgerRemove(id: Id): Promise<void>

  reconciliationPage(query?: ReconciliationResultPageQuery): Promise<PageResult<ReconciliationResultVO>>
  reconciliationDetail(id: Id): Promise<ReconciliationResultVO>
  reconciliationMatch(body: ReconciliationMatchRequest): Promise<ReconciliationMatchResultVO>
  reconciliationResolve(id: Id, body: ReconciliationResolveRequest): Promise<void>
}

export function createBankApi(client: HttpClient): BankApi {
  return {
    async statementPage(query = {}) {
      return client.get<PageResult<BankStatementVO>>('/api/v1/bank/statements', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          accountId: query.accountId,
          status: query.status,
          type: query.type,
          startDate: query.startDate,
          endDate: query.endDate
        }
      })
    },
    async statementDetail(id) {
      return client.get<BankStatementVO>(`/api/v1/bank/statements/${id}`)
    },
    async statementCreate(body) {
      return client.post<Id>('/api/v1/bank/statements', body)
    },
    async statementUpdate(id, body) {
      return client.put<void>(`/api/v1/bank/statements/${id}`, body)
    },
    async statementRemove(id) {
      return client.delete<void>(`/api/v1/bank/statements/${id}`)
    },
    async statementImport(accountId, file, fileName) {
      const form = new FormData()
      form.append('accountId', accountId)
      if (fileName) {
        form.append('file', file, fileName)
      } else {
        form.append('file', file)
      }
      return client.post<StatementImportResultVO>('/api/v1/bank/statements/import', form)
    },

    async ledgerPage(query = {}) {
      return client.get<PageResult<GeneralLedgerVO>>('/api/v1/bank/ledgers', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          accountName: query.accountName,
          transactionType: query.transactionType,
          status: query.status
        }
      })
    },
    async ledgerDetail(id) {
      return client.get<GeneralLedgerVO>(`/api/v1/bank/ledgers/${id}`)
    },
    async ledgerCreate(body) {
      return client.post<Id>('/api/v1/bank/ledgers', body)
    },
    async ledgerUpdate(id, body) {
      return client.put<void>(`/api/v1/bank/ledgers/${id}`, body)
    },
    async ledgerRemove(id) {
      return client.delete<void>(`/api/v1/bank/ledgers/${id}`)
    },

    async reconciliationPage(query = {}) {
      return client.get<PageResult<ReconciliationResultVO>>('/api/v1/bank/reconciliations', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          accountId: query.accountId,
          status: query.status,
          resolutionStatus: query.resolutionStatus
        }
      })
    },
    async reconciliationDetail(id) {
      return client.get<ReconciliationResultVO>(`/api/v1/bank/reconciliations/${id}`)
    },
    async reconciliationMatch(body) {
      return client.post<ReconciliationMatchResultVO>('/api/v1/bank/reconciliations/match', body)
    },
    async reconciliationResolve(id, body) {
      return client.post<void>(`/api/v1/bank/reconciliations/${id}/resolve`, body)
    }
  }
}
