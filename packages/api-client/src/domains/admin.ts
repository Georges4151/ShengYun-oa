/**
 * 行政后勤域（admin）类型化 API 工厂。
 * 所有路径前缀统一 `/api/v1/admin`，与后端 AdminAssetController / AdminMaterialController /
 * AdminVehicleController / AdminMeetingController / AdminVisitorController / AdminRepairController 保持一致。
 */

import type { HttpClient } from '../http-client'
import type {
  Id,
  PageResult,
  AssetVO,
  AssetBorrowVO,
  AssetPageQuery,
  AssetCreateRequest,
  AssetUpdateRequest,
  AssetBorrowRequest,
  AssetScrapRequest,
  AssetInventoryRequest,
  AdminMaterialVO,
  AdminMaterialApplyVO,
  AdminMaterialPageQuery,
  AdminMaterialCreateRequest,
  AdminMaterialUpdateRequest,
  AdminMaterialStockInRequest,
  AdminMaterialApplyRequest,
  VehicleVO,
  VehicleApplyVO,
  VehicleExpenseVO,
  VehiclePageQuery,
  VehicleCreateRequest,
  VehicleUpdateRequest,
  VehicleApplyRequest,
  VehicleReturnRequest,
  VehicleExpenseRequest,
  MeetingRoomVO,
  MeetingApplyVO,
  MeetingAvailabilityVO,
  MeetingRoomCreateRequest,
  MeetingRoomUpdateRequest,
  MeetingApplyRequest,
  VisitorVO,
  VisitorQrVO,
  VisitorPageQuery,
  VisitorCreateRequest,
  VisitorCheckinRequest,
  RepairVO,
  RepairPageQuery,
  RepairCreateRequest,
  RepairAssignRequest,
  RepairProcessRequest,
  RepairFinishRequest
} from '@ceams/types'

export interface AdminApi {
  // 资产台账
  assetPage(query?: AssetPageQuery): Promise<PageResult<AssetVO>>
  assetDetail(id: Id): Promise<AssetVO>
  assetCreate(body: AssetCreateRequest): Promise<Id>
  assetUpdate(id: Id, body: AssetUpdateRequest): Promise<void>

  // 资产领用 / 归还 / 报废 / 盘点
  assetBorrow(id: Id, body: AssetBorrowRequest): Promise<Id>
  myAssetBorrows(): Promise<AssetBorrowVO[]>
  assetReturn(id: Id): Promise<void>
  assetScrap(id: Id, body?: AssetScrapRequest): Promise<void>
  assetInventory(body: AssetInventoryRequest): Promise<number>

  // 易耗品
  materialPage(query?: AdminMaterialPageQuery): Promise<PageResult<AdminMaterialVO>>
  materialCreate(body: AdminMaterialCreateRequest): Promise<Id>
  materialUpdate(id: Id, body: AdminMaterialUpdateRequest): Promise<void>
  materialStockIn(id: Id, body: AdminMaterialStockInRequest): Promise<void>
  materialApply(body: AdminMaterialApplyRequest): Promise<Id>
  myMaterialApplies(): Promise<AdminMaterialApplyVO[]>
  materialIssue(id: Id): Promise<void>
  materialAlerts(): Promise<AdminMaterialVO[]>

  // 车辆
  vehiclePage(query?: VehiclePageQuery): Promise<PageResult<VehicleVO>>
  vehicleCreate(body: VehicleCreateRequest): Promise<Id>
  vehicleUpdate(id: Id, body: VehicleUpdateRequest): Promise<void>
  vehicleApply(body: VehicleApplyRequest): Promise<Id>
  myVehicleApplies(): Promise<VehicleApplyVO[]>
  vehicleReturn(id: Id, body: VehicleReturnRequest): Promise<void>
  vehicleExpenseCreate(id: Id, body: VehicleExpenseRequest): Promise<Id>
  vehicleExpenses(id: Id): Promise<VehicleExpenseVO[]>
  vehicleAlerts(): Promise<VehicleVO[]>

  // 会议室
  meetingRoomList(): Promise<MeetingRoomVO[]>
  meetingRoomCreate(body: MeetingRoomCreateRequest): Promise<Id>
  meetingRoomUpdate(id: Id, body: MeetingRoomUpdateRequest): Promise<void>
  meetingApply(body: MeetingApplyRequest): Promise<Id>
  myMeetingApplies(): Promise<MeetingApplyVO[]>
  meetingAvailability(roomId: Id, date: string): Promise<MeetingAvailabilityVO>

  // 访客
  visitorCreate(body: VisitorCreateRequest): Promise<VisitorQrVO>
  visitorPage(query?: VisitorPageQuery): Promise<PageResult<VisitorVO>>
  visitorQr(id: Id): Promise<VisitorQrVO>
  visitorCheckin(body: VisitorCheckinRequest): Promise<void>
  visitorCheckout(id: Id): Promise<void>
  visitorCancel(id: Id): Promise<void>

  // 报修
  repairCreate(body: RepairCreateRequest): Promise<Id>
  repairPage(query?: RepairPageQuery): Promise<PageResult<RepairVO>>
  myRepairs(): Promise<RepairVO[]>
  repairAssign(id: Id, body: RepairAssignRequest): Promise<void>
  repairProcess(id: Id, body: RepairProcessRequest): Promise<void>
  repairFinish(id: Id, body: RepairFinishRequest): Promise<void>
  repairAccept(id: Id): Promise<void>
}

const PREFIX = '/api/v1/admin'

export function createAdminApi(client: HttpClient): AdminApi {
  return {
    // ---------- 资产台账 ----------
    async assetPage(query = {}) {
      return client.get<PageResult<AssetVO>>(`${PREFIX}/assets`, {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          keyword: query.keyword,
          status: query.status,
          deptId: query.deptId
        }
      })
    },
    async assetDetail(id) {
      return client.get<AssetVO>(`${PREFIX}/assets/${id}`)
    },
    async assetCreate(body) {
      return client.post<Id>(`${PREFIX}/assets`, body)
    },
    async assetUpdate(id, body) {
      return client.put<void>(`${PREFIX}/assets/${id}`, body)
    },

    // ---------- 资产领用 / 归还 / 报废 / 盘点 ----------
    async assetBorrow(id, body) {
      return client.post<Id>(`${PREFIX}/assets/${id}/borrow`, body)
    },
    async myAssetBorrows() {
      return client.get<AssetBorrowVO[]>(`${PREFIX}/assets/borrows/my`)
    },
    async assetReturn(id) {
      return client.post<void>(`${PREFIX}/assets/${id}/return`)
    },
    async assetScrap(id, body) {
      return client.post<void>(`${PREFIX}/assets/${id}/scrap`, body)
    },
    async assetInventory(body) {
      return client.post<number>(`${PREFIX}/assets/inventory`, body)
    },

    // ---------- 易耗品 ----------
    async materialPage(query = {}) {
      return client.get<PageResult<AdminMaterialVO>>(`${PREFIX}/materials`, {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          keyword: query.keyword,
          status: query.status
        }
      })
    },
    async materialCreate(body) {
      return client.post<Id>(`${PREFIX}/materials`, body)
    },
    async materialUpdate(id, body) {
      return client.put<void>(`${PREFIX}/materials/${id}`, body)
    },
    async materialStockIn(id, body) {
      return client.post<void>(`${PREFIX}/materials/${id}/stock-in`, body)
    },
    async materialApply(body) {
      return client.post<Id>(`${PREFIX}/materials/apply`, body)
    },
    async myMaterialApplies() {
      return client.get<AdminMaterialApplyVO[]>(`${PREFIX}/materials/applies/my`)
    },
    async materialIssue(id) {
      return client.put<void>(`${PREFIX}/materials/applies/${id}/issue`)
    },
    async materialAlerts() {
      return client.get<AdminMaterialVO[]>(`${PREFIX}/materials/alerts`)
    },

    // ---------- 车辆 ----------
    async vehiclePage(query = {}) {
      return client.get<PageResult<VehicleVO>>(`${PREFIX}/vehicles`, {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          keyword: query.keyword,
          status: query.status
        }
      })
    },
    async vehicleCreate(body) {
      return client.post<Id>(`${PREFIX}/vehicles`, body)
    },
    async vehicleUpdate(id, body) {
      return client.put<void>(`${PREFIX}/vehicles/${id}`, body)
    },
    async vehicleApply(body) {
      return client.post<Id>(`${PREFIX}/vehicles/apply`, body)
    },
    async myVehicleApplies() {
      return client.get<VehicleApplyVO[]>(`${PREFIX}/vehicles/applies/my`)
    },
    async vehicleReturn(id, body) {
      return client.post<void>(`${PREFIX}/vehicles/applies/${id}/return`, body)
    },
    async vehicleExpenseCreate(id, body) {
      return client.post<Id>(`${PREFIX}/vehicles/${id}/expenses`, body)
    },
    async vehicleExpenses(id) {
      return client.get<VehicleExpenseVO[]>(`${PREFIX}/vehicles/${id}/expenses`)
    },
    async vehicleAlerts() {
      return client.get<VehicleVO[]>(`${PREFIX}/vehicles/alerts`)
    },

    // ---------- 会议室 ----------
    async meetingRoomList() {
      return client.get<MeetingRoomVO[]>(`${PREFIX}/meeting-rooms`)
    },
    async meetingRoomCreate(body) {
      return client.post<Id>(`${PREFIX}/meeting-rooms`, body)
    },
    async meetingRoomUpdate(id, body) {
      return client.put<void>(`${PREFIX}/meeting-rooms/${id}`, body)
    },
    async meetingApply(body) {
      return client.post<Id>(`${PREFIX}/meeting-rooms/apply`, body)
    },
    async myMeetingApplies() {
      return client.get<MeetingApplyVO[]>(`${PREFIX}/meeting-rooms/applies/my`)
    },
    async meetingAvailability(roomId, date) {
      return client.get<MeetingAvailabilityVO>(`${PREFIX}/meeting-rooms/availability`, {
        query: { roomId, date }
      })
    },

    // ---------- 访客 ----------
    async visitorCreate(body) {
      return client.post<VisitorQrVO>(`${PREFIX}/visitors`, body)
    },
    async visitorPage(query = {}) {
      return client.get<PageResult<VisitorVO>>(`${PREFIX}/visitors`, {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          date: query.date,
          status: query.status
        }
      })
    },
    async visitorQr(id) {
      return client.get<VisitorQrVO>(`${PREFIX}/visitors/${id}/qr`)
    },
    async visitorCheckin(body) {
      return client.post<void>(`${PREFIX}/visitors/checkin`, body)
    },
    async visitorCheckout(id) {
      return client.post<void>(`${PREFIX}/visitors/${id}/checkout`)
    },
    async visitorCancel(id) {
      return client.post<void>(`${PREFIX}/visitors/${id}/cancel`)
    },

    // ---------- 报修 ----------
    async repairCreate(body) {
      return client.post<Id>(`${PREFIX}/repairs`, body)
    },
    async repairPage(query = {}) {
      return client.get<PageResult<RepairVO>>(`${PREFIX}/repairs`, {
        query: {
          page: query.page ?? 1,
          size: query.size ?? 20,
          status: query.status,
          assigneeId: query.assigneeId
        }
      })
    },
    async myRepairs() {
      return client.get<RepairVO[]>(`${PREFIX}/repairs/my`)
    },
    async repairAssign(id, body) {
      return client.put<void>(`${PREFIX}/repairs/${id}/assign`, body)
    },
    async repairProcess(id, body) {
      return client.put<void>(`${PREFIX}/repairs/${id}/process`, body)
    },
    async repairFinish(id, body) {
      return client.put<void>(`${PREFIX}/repairs/${id}/finish`, body)
    },
    async repairAccept(id) {
      return client.put<void>(`${PREFIX}/repairs/${id}/accept`)
    }
  }
}
