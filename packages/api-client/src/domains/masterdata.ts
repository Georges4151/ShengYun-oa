/**
 * 主数据域（masterdata）类型化 API 工厂。
 * 所有路径前缀统一 `/api/v1`，与 docs/openapi/masterdata.openapi.yaml paths 一致。
 */

import type { HttpClient } from '../http-client'
import type {
  Id,
  CategoryVO,
  CategoryCreateRequest,
  CategoryUpdateRequest,
  CategoryTreeQuery,
  CategoryPageQuery,
  ContactVO,
  ContactCreateRequest,
  ContactUpdateRequest,
  ContactPageQuery,
  MaterialVO,
  MaterialCreateRequest,
  MaterialUpdateRequest,
  MaterialPageQuery,
  EquipmentVO,
  EquipmentCreateRequest,
  EquipmentUpdateRequest,
  EquipmentPageQuery,
  EquipmentLeaseVO,
  EquipmentLeaseCreateRequest,
  EquipmentLeaseUpdateRequest,
  EquipmentLeasePageQuery,
  PageResult
} from '@ceams/types'

export interface MasterdataApi {
  categoryTree(query?: CategoryTreeQuery): Promise<CategoryVO[]>
  categoryPage(query?: CategoryPageQuery): Promise<PageResult<CategoryVO>>
  categoryDetail(id: Id): Promise<CategoryVO>
  categoryCreate(body: CategoryCreateRequest): Promise<Id>
  categoryUpdate(id: Id, body: CategoryUpdateRequest): Promise<void>
  categoryRemove(id: Id): Promise<void>

  contactPage(query?: ContactPageQuery): Promise<PageResult<ContactVO>>
  contactDetail(id: Id): Promise<ContactVO>
  contactCreate(body: ContactCreateRequest): Promise<Id>
  contactUpdate(id: Id, body: ContactUpdateRequest): Promise<void>
  contactRemove(id: Id): Promise<void>

  materialPage(query?: MaterialPageQuery): Promise<PageResult<MaterialVO>>
  materialDetail(id: Id): Promise<MaterialVO>
  materialCreate(body: MaterialCreateRequest): Promise<Id>
  materialUpdate(id: Id, body: MaterialUpdateRequest): Promise<void>
  materialRemove(id: Id): Promise<void>

  equipmentPage(query?: EquipmentPageQuery): Promise<PageResult<EquipmentVO>>
  equipmentDetail(id: Id): Promise<EquipmentVO>
  equipmentCreate(body: EquipmentCreateRequest): Promise<Id>
  equipmentUpdate(id: Id, body: EquipmentUpdateRequest): Promise<void>
  equipmentRemove(id: Id): Promise<void>

  leasePage(query?: EquipmentLeasePageQuery): Promise<PageResult<EquipmentLeaseVO>>
  leaseDetail(id: Id): Promise<EquipmentLeaseVO>
  leaseCreate(body: EquipmentLeaseCreateRequest): Promise<Id>
  leaseUpdate(id: Id, body: EquipmentLeaseUpdateRequest): Promise<void>
  leaseRemove(id: Id): Promise<void>
}

export function createMasterdataApi(client: HttpClient): MasterdataApi {
  return {
    async categoryTree(query = {}) {
      return client.get<CategoryVO[]>('/api/v1/categories/tree', {
        query: { type: query.type }
      })
    },
    async categoryPage(query = {}) {
      return client.get<PageResult<CategoryVO>>('/api/v1/categories', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          type: query.type,
          parentId: query.parentId
        }
      })
    },
    async categoryDetail(id) {
      return client.get<CategoryVO>(`/api/v1/categories/${id}`)
    },
    async categoryCreate(body) {
      return client.post<Id>('/api/v1/categories', body)
    },
    async categoryUpdate(id, body) {
      return client.put<void>(`/api/v1/categories/${id}`, body)
    },
    async categoryRemove(id) {
      return client.delete<void>(`/api/v1/categories/${id}`)
    },

    async contactPage(query = {}) {
      return client.get<PageResult<ContactVO>>('/api/v1/contacts', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          keyword: query.keyword,
          type: query.type
        }
      })
    },
    async contactDetail(id) {
      return client.get<ContactVO>(`/api/v1/contacts/${id}`)
    },
    async contactCreate(body) {
      return client.post<Id>('/api/v1/contacts', body)
    },
    async contactUpdate(id, body) {
      return client.put<void>(`/api/v1/contacts/${id}`, body)
    },
    async contactRemove(id) {
      return client.delete<void>(`/api/v1/contacts/${id}`)
    },

    async materialPage(query = {}) {
      return client.get<PageResult<MaterialVO>>('/api/v1/materials', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          keyword: query.keyword,
          categoryId: query.categoryId
        }
      })
    },
    async materialDetail(id) {
      return client.get<MaterialVO>(`/api/v1/materials/${id}`)
    },
    async materialCreate(body) {
      return client.post<Id>('/api/v1/materials', body)
    },
    async materialUpdate(id, body) {
      return client.put<void>(`/api/v1/materials/${id}`, body)
    },
    async materialRemove(id) {
      return client.delete<void>(`/api/v1/materials/${id}`)
    },

    async equipmentPage(query = {}) {
      return client.get<PageResult<EquipmentVO>>('/api/v1/equipment', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          keyword: query.keyword,
          status: query.status
        }
      })
    },
    async equipmentDetail(id) {
      return client.get<EquipmentVO>(`/api/v1/equipment/${id}`)
    },
    async equipmentCreate(body) {
      return client.post<Id>('/api/v1/equipment', body)
    },
    async equipmentUpdate(id, body) {
      return client.put<void>(`/api/v1/equipment/${id}`, body)
    },
    async equipmentRemove(id) {
      return client.delete<void>(`/api/v1/equipment/${id}`)
    },

    async leasePage(query = {}) {
      return client.get<PageResult<EquipmentLeaseVO>>('/api/v1/equipment-leases', {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          equipmentId: query.equipmentId,
          projectId: query.projectId
        }
      })
    },
    async leaseDetail(id) {
      return client.get<EquipmentLeaseVO>(`/api/v1/equipment-leases/${id}`)
    },
    async leaseCreate(body) {
      return client.post<Id>('/api/v1/equipment-leases', body)
    },
    async leaseUpdate(id, body) {
      return client.put<void>(`/api/v1/equipment-leases/${id}`, body)
    },
    async leaseRemove(id) {
      return client.delete<void>(`/api/v1/equipment-leases/${id}`)
    }
  }
}