/**
 * 行政后勤域（admin）类型：资产 / 易耗品 / 车辆 / 会议室 / 访客 / 报修。
 *
 * 字段与后端 `com.ceams.module.admin.vo` / `dto` 严格一致：
 * - 主键 Id = string（后端雪花 ID 序列化为字符串）
 * - 金额为后端 BigDecimal，Jackson 统一序列化为字符串（decimal(18,2)），允许为空
 * - 日期 yyyy-MM-dd、日期时间 ISO8601（yyyy-MM-ddTHH:mm:ss），允许为空
 */

import type { DateTime, Id, Money } from './common'

// ---------- 枚举 ----------

/** 资产分类。 */
export type AssetCategory = 'ELECTRONIC' | 'FURNITURE' | 'VEHICLE_TOOL' | 'OTHER'

/** 资产状态。 */
export type AssetStatus = 'IN_STOCK' | 'BORROWED' | 'REPAIRING' | 'SCRAPPED'

/** 资产领用记录状态。 */
export type AssetBorrowStatus = 'APPROVING' | 'BORROWING' | 'RETURNED' | 'REJECTED'

/** 易耗品状态。 */
export type MaterialStatus = 'ENABLED' | 'DISABLED'

/** 易耗品申领单状态。 */
export type MaterialApplyStatus = 'DRAFT' | 'APPROVING' | 'APPROVED' | 'REJECTED' | 'ISSUED'

/** 车辆状态。 */
export type VehicleStatus = 'IDLE' | 'USING' | 'MAINTENANCE'

/** 用车申请状态。 */
export type VehicleApplyStatus =
  | 'APPROVING'
  | 'APPROVED'
  | 'REJECTED'
  | 'USING'
  | 'RETURNED'
  | 'CANCELLED'

/** 车辆费用类型。 */
export type VehicleExpenseType = 'MAINTENANCE' | 'VIOLATION' | 'FUEL'

/** 会议室预定状态。 */
export type MeetingApplyStatus = 'APPROVING' | 'APPROVED' | 'REJECTED' | 'CANCELLED'

/** 访客状态。 */
export type VisitorStatus = 'PENDING' | 'CHECKED_IN' | 'CHECKED_OUT' | 'CANCELLED'

/** 报修分类。 */
export type RepairCategory = 'ELECTRIC' | 'WATER' | 'FURNITURE' | 'IT' | 'OTHER'

/** 报修工单状态。 */
export type RepairStatus =
  | 'SUBMITTED'
  | 'ASSIGNED'
  | 'PROCESSING'
  | 'DONE'
  | 'ACCEPTED'
  | 'REJECTED'

// ---------- 资产台账 ----------

export interface AssetVO {
  id: Id
  assetNo: string
  name: string
  category: AssetCategory | null
  specification: string | null
  purchaseDate: string | null
  /** 购置金额（后端 BigDecimal 统一序列化为字符串）。 */
  purchasePrice: Money | null
  deptId: Id | null
  keeperId: Id | null
  location: string | null
  status: AssetStatus
  depreciationMonths: number | null
  remark: string | null
  createTime?: DateTime
}

export interface AssetBorrowVO {
  id: Id
  assetId: Id
  assetNo: string | null
  assetName: string | null
  borrowerId: Id
  borrowerName: string | null
  borrowDate: string
  expectedReturnDate: string | null
  actualReturnDate: string | null
  bpmInstanceId: Id | null
  status: AssetBorrowStatus
  remark: string | null
  createTime?: DateTime
}

export interface AssetPageQuery {
  page?: number
  size?: number
  keyword?: string
  status?: AssetStatus
  deptId?: Id
}

export interface AssetCreateRequest {
  name: string
  category?: AssetCategory
  specification?: string
  purchaseDate?: string
  purchasePrice?: number
  deptId?: Id
  keeperId?: Id
  location?: string
  depreciationMonths?: number
  remark?: string
}

export interface AssetUpdateRequest extends AssetCreateRequest {
  id: Id
}

export interface AssetBorrowRequest {
  borrowDate: string
  expectedReturnDate?: string
  remark?: string
}

export interface AssetScrapRequest {
  remark?: string
}

export interface AssetInventoryItem {
  assetId: Id
  location?: string
  keeperId?: Id
  /** 盘点后的状态：仅允许 IN_STOCK / REPAIRING。 */
  status?: 'IN_STOCK' | 'REPAIRING'
}

export interface AssetInventoryRequest {
  items: AssetInventoryItem[]
}

// ---------- 易耗品 ----------

export interface AdminMaterialVO {
  id: Id
  code: string
  name: string
  specification: string | null
  unit: string | null
  /** 当前库存（后端 BigDecimal 统一序列化为字符串）。 */
  stockQty: Money | null
  /** 安全库存（后端 BigDecimal 统一序列化为字符串）。 */
  safetyStock: Money | null
  unitPrice: Money | null
  status: MaterialStatus
  /** 库存缺口比例（安全库存 - 当前库存）/ 安全库存，仅预警列表返回。 */
  shortageRatio: Money | null
}

export interface AdminMaterialApplyVO {
  id: Id
  materialId: Id
  materialCode: string | null
  materialName: string | null
  unit: string | null
  applicantId: Id
  applicantName: string | null
  /** 申领数量（后端 BigDecimal 统一序列化为字符串）。 */
  quantity: Money | null
  reason: string | null
  bpmInstanceId: Id | null
  status: MaterialApplyStatus
  issuedAt: string | null
  issuedBy: Id | null
  createTime?: DateTime
}

export interface AdminMaterialPageQuery {
  page?: number
  size?: number
  keyword?: string
  status?: MaterialStatus
}

export interface AdminMaterialCreateRequest {
  code: string
  name: string
  specification?: string
  unit?: string
  stockQty?: number
  safetyStock?: number
  unitPrice?: number
}

export interface AdminMaterialUpdateRequest {
  id: Id
  code?: string
  name?: string
  specification?: string
  unit?: string
  safetyStock?: number
  unitPrice?: number
}

export interface AdminMaterialStockInRequest {
  /** 入库数量，必须大于 0。 */
  quantity: number
  remark?: string
}

export interface AdminMaterialApplyRequest {
  materialId: Id
  /** 申领数量，必须大于 0。 */
  quantity: number
  reason?: string
}

// ---------- 车辆 ----------

export interface VehicleVO {
  id: Id
  plateNo: string
  brandModel: string | null
  seats: number | null
  purchaseDate: string | null
  insuranceExpire: string | null
  inspectionExpire: string | null
  status: VehicleStatus
  remark: string | null
  /** 保险剩余天数（负数表示已过期）。 */
  insuranceRemainDays: number | null
  /** 年检剩余天数（负数表示已过期）。 */
  inspectionRemainDays: number | null
  createTime?: DateTime
}

export interface VehicleApplyVO {
  id: Id
  vehicleId: Id
  vehiclePlateNo: string | null
  applicantId: Id
  purpose: string | null
  destination: string | null
  startTime: string
  endTime: string
  bpmInstanceId: Id | null
  status: VehicleApplyStatus
  returnTime: string | null
  /** 还车里程（后端 BigDecimal 统一序列化为字符串）。 */
  mileage: Money | null
  createTime?: DateTime
}

export interface VehicleExpenseVO {
  id: Id
  vehicleId: Id
  applyId: Id | null
  expenseType: VehicleExpenseType
  amount: Money | null
  occurDate: string | null
  attachmentName: string | null
  remark: string | null
  createTime?: DateTime
}

export interface VehiclePageQuery {
  page?: number
  size?: number
  keyword?: string
  status?: VehicleStatus
}

export interface VehicleCreateRequest {
  plateNo: string
  brandModel?: string
  seats?: number
  purchaseDate?: string
  insuranceExpire?: string
  inspectionExpire?: string
  remark?: string
}

export interface VehicleUpdateRequest {
  brandModel?: string
  seats?: number
  purchaseDate?: string
  insuranceExpire?: string
  inspectionExpire?: string
  remark?: string
}

export interface VehicleApplyRequest {
  vehicleId: Id
  purpose?: string
  destination?: string
  startTime: DateTime
  endTime: DateTime
}

export interface VehicleReturnRequest {
  returnTime?: DateTime
  /** 还车里程（公里，≥0）。 */
  mileage: number
  remark?: string
}

export interface VehicleExpenseRequest {
  expenseType: VehicleExpenseType
  /** 金额，必须大于 0。 */
  amount: number
  occurDate: string
  attachmentName?: string
  remark?: string
}

// ---------- 会议室 ----------

export interface MeetingRoomVO {
  id: Id
  name: string
  location: string | null
  capacity: number | null
  equipment: string | null
  /** 会议室状态：ENABLED / DISABLED。 */
  status: string
  createTime?: DateTime
}

export interface MeetingApplyVO {
  id: Id
  roomId: Id
  roomName: string | null
  applicantId: Id
  subject: string
  startTime: string
  endTime: string
  attendeeIds: Id[]
  bpmInstanceId: Id | null
  status: MeetingApplyStatus
  createTime?: DateTime
}

export interface MeetingAvailabilityInterval {
  startTime: string
  endTime: string
  subject: string | null
}

export interface MeetingAvailabilityVO {
  roomId: Id
  date: string
  /** 已占用区间，按开始时间升序。 */
  intervals: MeetingAvailabilityInterval[]
}

export interface MeetingRoomCreateRequest {
  name: string
  location?: string
  capacity?: number
  equipment?: string
}

export interface MeetingRoomUpdateRequest {
  name?: string
  location?: string
  capacity?: number
  equipment?: string
}

export interface MeetingApplyRequest {
  roomId: Id
  subject: string
  startTime: DateTime
  endTime: DateTime
  attendeeIds?: Id[]
}

// ---------- 访客 ----------

export interface VisitorVO {
  id: Id
  visitorName: string
  /** 手机号（接口返回已脱敏）。 */
  phone: string | null
  company: string | null
  hostUserId: Id | null
  visitDate: string
  purpose: string | null
  status: VisitorStatus
  checkinTime: string | null
  checkoutTime: string | null
}

export interface VisitorQrVO {
  visitorId: Id
  /** 一次性核销令牌（二维码内容只放该令牌）。 */
  token: string
  expireAt: string
  visitDate: string
  visitorName: string
}

export interface VisitorPageQuery {
  page?: number
  size?: number
  date?: string
  status?: VisitorStatus
}

export interface VisitorCreateRequest {
  visitorName: string
  phone: string
  company?: string
  hostUserId?: Id
  visitDate: string
  purpose?: string
}

export interface VisitorCheckinRequest {
  token: string
}

// ---------- 报修 ----------

export interface RepairVO {
  id: Id
  repairNo: string
  reporterId: Id
  deptId: Id | null
  location: string
  category: RepairCategory
  description: string
  /** 报修现场图片文件名列表。 */
  images: string[]
  /** 完工图片文件名列表。 */
  finishImages: string[]
  assigneeId: Id | null
  status: RepairStatus
  assignedAt: string | null
  processedAt: string | null
  finishedAt: string | null
  acceptedAt: string | null
  remark: string | null
}

export interface RepairPageQuery {
  page?: number
  size?: number
  status?: RepairStatus
  assigneeId?: Id
}

export interface RepairCreateRequest {
  location: string
  category: RepairCategory
  description: string
  /** 现场图片文件名列表，1～6 张。 */
  images: string[]
}

export interface RepairAssignRequest {
  assigneeId: Id
  remark?: string
}

export interface RepairProcessRequest {
  remark?: string
}

export interface RepairFinishRequest {
  remark?: string
  /** 完工图片文件名列表，1～6 张。 */
  images: string[]
}
