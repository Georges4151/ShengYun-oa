/**
 * 主数据域（masterdata）类型：分类树 / 往来单位 / 材料库 / 设备台账 / 设备租赁。
 *
 * 字段建模规则（与 docs/openapi/masterdata.openapi.yaml components/schemas 严格一致）：
 * - openapi `required` 中的字段 → 必填 `field: Type`
 * - 其余字段 → 可选 `field?: Type`
 * - `nullable: true` 且非必填 → 可选可空 `field?: Type | null`
 * - Id = string、金额用 Money（string）、日期用 string（yyyy-MM-dd）、时间用 DateTime（ISO8601）。
 */

import type { DateTime, Id, Money } from './common'

// ---------- 枚举 ----------
export type CategoryType = 'EXPENSE' | 'MATERIAL' | 'PROJECT' | 'CONTACT' | 'PAYMENT' | 'OTHER'
export type ContactType = 'SUPPLIER' | 'CUSTOMER' | 'SUBCONTRACTOR' | 'OTHER'
export type EquipmentStatus = 'IDLE' | 'IN_USE' | 'LEASED' | 'MAINTENANCE' | 'SCRAPPED'
export type PaymentStatus = 'UNPAID' | 'PARTIAL' | 'PAID'

// ---------- 分类 ----------
export interface CategoryVO {
  id: Id
  name: string
  type: CategoryType
  code?: string | null
  parentId?: Id | null
  sort?: number
  enabled?: boolean
  children?: CategoryVO[]
  createTime?: DateTime
}

export interface CategoryCreateRequest {
  name: string
  type: CategoryType
  code?: string | null
  parentId?: Id | null
  sort?: number
}

export interface CategoryUpdateRequest {
  name?: string
  code?: string
  parentId?: Id | null
  sort?: number
  enabled?: boolean
}

// ---------- 往来单位 ----------
export interface ContactVO {
  id: Id
  name: string
  type: ContactType
  contactPerson?: string | null
  /** 手机号（加密） */
  phone?: string | null
  email?: string | null
  address?: string | null
  /** 税号（加密） */
  taxIdentificationNumber?: string | null
  /** 银行账号（加密） */
  bankAccount?: string | null
  createTime?: DateTime
}

export interface ContactCreateRequest {
  name: string
  type: ContactType
  contactPerson?: string | null
  phone?: string | null
  email?: string | null
  address?: string | null
  taxIdentificationNumber?: string | null
  bankAccount?: string | null
}

export interface ContactUpdateRequest {
  name?: string
  type?: ContactType
  contactPerson?: string
  phone?: string
  email?: string
  address?: string
  taxIdentificationNumber?: string
  bankAccount?: string
}

// ---------- 材料 ----------
export interface MaterialVO {
  id: Id
  materialCode: string
  materialName: string
  specification?: string | null
  unit: string
  currentPrice?: Money
  supplierId?: Id | null
  categoryId?: Id | null
  /** 供应商名称（展示用） */
  supplierName?: string | null
  /** 分类名称（展示用） */
  categoryName?: string | null
  createTime?: DateTime
}

export interface MaterialCreateRequest {
  materialCode: string
  materialName: string
  specification?: string | null
  unit: string
  currentPrice?: Money
  supplierId?: Id | null
  categoryId?: Id | null
}

export interface MaterialUpdateRequest {
  materialName?: string
  specification?: string | null
  unit?: string
  currentPrice?: Money | null
  supplierId?: Id | null
  categoryId?: Id | null
}

// ---------- 设备台账 ----------
export interface EquipmentVO {
  id: Id
  name: string
  type: string
  model?: string | null
  status: EquipmentStatus
  purchaseDate?: string | null
  purchasePrice?: Money | null
  createTime?: DateTime
}

export interface EquipmentCreateRequest {
  name: string
  type: string
  model?: string | null
  status: EquipmentStatus
  purchaseDate?: string | null
  purchasePrice?: Money | null
}

export interface EquipmentUpdateRequest {
  name?: string
  type?: string
  model?: string
  status?: EquipmentStatus
  purchaseDate?: string
  purchasePrice?: Money
}

// ---------- 设备租赁 ----------
export interface EquipmentLeaseVO {
  id: Id
  equipmentId: Id
  /** 设备名称 */
  equipmentName?: string | null
  projectId: Id
  /** 项目名称 */
  projectName?: string | null
  /** 出租方（往来单位）ID */
  lessorId?: Id | null
  /** 出租方（往来单位）名称 */
  lessorName?: string | null
  leaseStartDate?: string
  leaseEndDate?: string
  dailyRent?: Money
  totalRent?: Money
  paymentStatus?: PaymentStatus
  createTime?: DateTime
}

export interface EquipmentLeaseCreateRequest {
  equipmentId: Id
  projectId: Id
  lessorId?: Id | null
  leaseStartDate: string
  leaseEndDate: string
  dailyRent?: Money
  totalRent?: Money
  paymentStatus?: PaymentStatus
}

export interface EquipmentLeaseUpdateRequest {
  projectId?: Id
  lessorId?: Id
  leaseStartDate?: string
  leaseEndDate?: string
  dailyRent?: Money
  totalRent?: Money
  paymentStatus?: PaymentStatus
}

// ---------- 分页 / 树查询入参 ----------
export interface CategoryTreeQuery {
  type?: CategoryType
}

export interface CategoryPageQuery {
  page?: number
  size?: number
  type?: CategoryType
  parentId?: Id
}

export interface ContactPageQuery {
  page?: number
  size?: number
  keyword?: string
  type?: ContactType
}

export interface MaterialPageQuery {
  page?: number
  size?: number
  keyword?: string
  categoryId?: Id
}

export interface EquipmentPageQuery {
  page?: number
  size?: number
  keyword?: string
  status?: EquipmentStatus
}

export interface EquipmentLeasePageQuery {
  page?: number
  size?: number
  equipmentId?: Id
  projectId?: Id
}