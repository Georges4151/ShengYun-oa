/**
 * 项目合同域（project）类型化 API 工厂。
 * 所有路径前缀统一 `/api/v1`，与 docs/openapi/project.openapi.yaml paths 一致。
 */

import type { HttpClient } from '../http-client'
import type {
  Id,
  ProjectVO,
  ProjectCreateRequest,
  ProjectUpdateRequest,
  ProjectPageQuery,
  BudgetVO,
  BudgetCreateRequest,
  BudgetUpdateRequest,
  BudgetPageQuery,
  ChangeRequestVO,
  ChangeRequestCreateRequest,
  ChangeRequestUpdateRequest,
  ChangeRequestPageQuery,
  ProgressPaymentVO,
  ProgressPaymentCreateRequest,
  ProgressPaymentUpdateRequest,
  ProgressPaymentPageQuery,
  SubcontractorVO,
  SubcontractorCreateRequest,
  SubcontractorUpdateRequest,
  SubcontractorPageQuery,
  SubcontractorContractVO,
  SubcontractorContractCreateRequest,
  SubcontractorContractUpdateRequest,
  SubcontractorContractPageQuery,
  SettlementProofVO,
  SettlementProofCreateRequest,
  SettlementProofUpdateRequest,
  SettlementProofPageQuery,
  PageResult
} from '@ceams/types'

export interface ProjectApi {
  projectPage(query?: ProjectPageQuery): Promise<PageResult<ProjectVO>>
  projectDetail(id: Id): Promise<ProjectVO>
  projectCreate(body: ProjectCreateRequest): Promise<Id>
  projectUpdate(id: Id, body: ProjectUpdateRequest): Promise<void>
  projectRemove(id: Id): Promise<void>

  budgetPage(query?: BudgetPageQuery): Promise<PageResult<BudgetVO>>
  budgetDetail(id: Id): Promise<BudgetVO>
  budgetCreate(body: BudgetCreateRequest): Promise<Id>
  budgetUpdate(id: Id, body: BudgetUpdateRequest): Promise<void>
  budgetRemove(id: Id): Promise<void>

  changePage(query?: ChangeRequestPageQuery): Promise<PageResult<ChangeRequestVO>>
  changeDetail(id: Id): Promise<ChangeRequestVO>
  changeCreate(body: ChangeRequestCreateRequest): Promise<Id>
  changeUpdate(id: Id, body: ChangeRequestUpdateRequest): Promise<void>
  changeRemove(id: Id): Promise<void>
  /** 提交变更单：发起 BPM 项目变更流程（部门审批 → 结束）。 */
  changeSubmit(id: Id): Promise<void>

  paymentPage(query?: ProgressPaymentPageQuery): Promise<PageResult<ProgressPaymentVO>>
  paymentDetail(id: Id): Promise<ProgressPaymentVO>
  paymentCreate(body: ProgressPaymentCreateRequest): Promise<Id>
  paymentUpdate(id: Id, body: ProgressPaymentUpdateRequest): Promise<void>
  paymentRemove(id: Id): Promise<void>

  subcontractorPage(query?: SubcontractorPageQuery): Promise<PageResult<SubcontractorVO>>
  subcontractorDetail(id: Id): Promise<SubcontractorVO>
  subcontractorCreate(body: SubcontractorCreateRequest): Promise<Id>
  subcontractorUpdate(id: Id, body: SubcontractorUpdateRequest): Promise<void>
  subcontractorRemove(id: Id): Promise<void>

  contractPage(query?: SubcontractorContractPageQuery): Promise<PageResult<SubcontractorContractVO>>
  contractDetail(id: Id): Promise<SubcontractorContractVO>
  contractCreate(body: SubcontractorContractCreateRequest): Promise<Id>
  contractUpdate(id: Id, body: SubcontractorContractUpdateRequest): Promise<void>
  contractRemove(id: Id): Promise<void>

  proofPage(query?: SettlementProofPageQuery): Promise<PageResult<SettlementProofVO>>
  proofDetail(id: Id): Promise<SettlementProofVO>
  proofCreate(body: SettlementProofCreateRequest): Promise<Id>
  proofUpdate(id: Id, body: SettlementProofUpdateRequest): Promise<void>
  proofRemove(id: Id): Promise<void>
}

export function createProjectApi(client: HttpClient): ProjectApi {
  return {
    async projectPage(query = {}) {
      return client.get<PageResult<ProjectVO>>('/api/v1/projects', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          keyword: query.keyword,
          status: query.status
        }
      })
    },
    async projectDetail(id) {
      return client.get<ProjectVO>(`/api/v1/projects/${id}`)
    },
    async projectCreate(body) {
      return client.post<Id>('/api/v1/projects', body)
    },
    async projectUpdate(id, body) {
      return client.put<void>(`/api/v1/projects/${id}`, body)
    },
    async projectRemove(id) {
      return client.delete<void>(`/api/v1/projects/${id}`)
    },

    async budgetPage(query = {}) {
      return client.get<PageResult<BudgetVO>>('/api/v1/budgets', {
        query: { page: query.page ?? 1, size: query.size ?? 20, projectId: query.projectId }
      })
    },
    async budgetDetail(id) {
      return client.get<BudgetVO>(`/api/v1/budgets/${id}`)
    },
    async budgetCreate(body) {
      return client.post<Id>('/api/v1/budgets', body)
    },
    async budgetUpdate(id, body) {
      return client.put<void>(`/api/v1/budgets/${id}`, body)
    },
    async budgetRemove(id) {
      return client.delete<void>(`/api/v1/budgets/${id}`)
    },

    async changePage(query = {}) {
      return client.get<PageResult<ChangeRequestVO>>('/api/v1/change-requests', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          projectId: query.projectId,
          status: query.status
        }
      })
    },
    async changeDetail(id) {
      return client.get<ChangeRequestVO>(`/api/v1/change-requests/${id}`)
    },
    async changeCreate(body) {
      return client.post<Id>('/api/v1/change-requests', body)
    },
    async changeUpdate(id, body) {
      return client.put<void>(`/api/v1/change-requests/${id}`, body)
    },
    async changeRemove(id) {
      return client.delete<void>(`/api/v1/change-requests/${id}`)
    },
    async changeSubmit(id) {
      return client.post<void>(`/api/v1/change-requests/${id}/submit`)
    },

    async paymentPage(query = {}) {
      return client.get<PageResult<ProgressPaymentVO>>('/api/v1/progress-payments', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          projectId: query.projectId,
          status: query.status
        }
      })
    },
    async paymentDetail(id) {
      return client.get<ProgressPaymentVO>(`/api/v1/progress-payments/${id}`)
    },
    async paymentCreate(body) {
      return client.post<Id>('/api/v1/progress-payments', body)
    },
    async paymentUpdate(id, body) {
      return client.put<void>(`/api/v1/progress-payments/${id}`, body)
    },
    async paymentRemove(id) {
      return client.delete<void>(`/api/v1/progress-payments/${id}`)
    },

    async subcontractorPage(query = {}) {
      return client.get<PageResult<SubcontractorVO>>('/api/v1/subcontractors', {
        query: { page: query.page ?? 1, size: query.size ?? 20, keyword: query.keyword }
      })
    },
    async subcontractorDetail(id) {
      return client.get<SubcontractorVO>(`/api/v1/subcontractors/${id}`)
    },
    async subcontractorCreate(body) {
      return client.post<Id>('/api/v1/subcontractors', body)
    },
    async subcontractorUpdate(id, body) {
      return client.put<void>(`/api/v1/subcontractors/${id}`, body)
    },
    async subcontractorRemove(id) {
      return client.delete<void>(`/api/v1/subcontractors/${id}`)
    },

    async contractPage(query = {}) {
      return client.get<PageResult<SubcontractorContractVO>>('/api/v1/subcontractor-contracts', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          subcontractorId: query.subcontractorId,
          projectId: query.projectId
        }
      })
    },
    async contractDetail(id) {
      return client.get<SubcontractorContractVO>(`/api/v1/subcontractor-contracts/${id}`)
    },
    async contractCreate(body) {
      return client.post<Id>('/api/v1/subcontractor-contracts', body)
    },
    async contractUpdate(id, body) {
      return client.put<void>(`/api/v1/subcontractor-contracts/${id}`, body)
    },
    async contractRemove(id) {
      return client.delete<void>(`/api/v1/subcontractor-contracts/${id}`)
    },

    async proofPage(query = {}) {
      return client.get<PageResult<SettlementProofVO>>('/api/v1/settlement-proofs', {
        query: { page: query.page ?? 1, size: query.size ?? 20, contractId: query.contractId }
      })
    },
    async proofDetail(id) {
      return client.get<SettlementProofVO>(`/api/v1/settlement-proofs/${id}`)
    },
    async proofCreate(body) {
      return client.post<Id>('/api/v1/settlement-proofs', body)
    },
    async proofUpdate(id, body) {
      return client.put<void>(`/api/v1/settlement-proofs/${id}`, body)
    },
    async proofRemove(id) {
      return client.delete<void>(`/api/v1/settlement-proofs/${id}`)
    }
  }
}