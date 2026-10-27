import type {
  UserStatus,
  RoleStatus,
  PermissionType,
  Platform,
  DeviceStatus,
  OperationType,
  CategoryType,
  ContactType,
  EquipmentStatus,
  PaymentStatus,
  ProjectStatus,
  BudgetStatus,
  ApprovalStatus,
  ContractStatus
} from '@ceams/types'

/** 枚举值 -> 中文标签映射（与三域类型字段一致）。 */

export const userStatusLabels: Record<UserStatus, string> = {
  ACTIVE: '启用',
  DISABLED: '禁用',
  LOCKED: '锁定'
}

export const roleStatusLabels: Record<RoleStatus, string> = {
  ACTIVE: '启用',
  DISABLED: '禁用'
}

export const permissionTypeLabels: Record<PermissionType, string> = {
  MENU: '菜单',
  BUTTON: '按钮',
  API: '接口'
}

export const platformLabels: Record<Platform, string> = {
  WEB: 'Web',
  DESKTOP: '桌面端',
  MINIPROGRAM: '小程序'
}

export const deviceStatusLabels: Record<DeviceStatus, string> = {
  ACTIVE: '在线',
  UNBOUND: '已解绑'
}

export const operationTypeLabels: Record<OperationType, string> = {
  LOGIN: '登录',
  LOGOUT: '登出',
  CREATE: '新增',
  UPDATE: '修改',
  DELETE: '删除',
  APPROVE: '审批',
  EXPORT: '导出',
  IMPORT: '导入',
  OTHER: '其他'
}

export const categoryTypeLabels: Record<CategoryType, string> = {
  EXPENSE: '费用',
  MATERIAL: '材料',
  PROJECT: '项目',
  CONTACT: '往来单位',
  PAYMENT: '支付',
  OTHER: '其他'
}

export const contactTypeLabels: Record<ContactType, string> = {
  SUPPLIER: '供应商',
  CUSTOMER: '客户',
  SUBCONTRACTOR: '分包商',
  OTHER: '其他'
}

export const equipmentStatusLabels: Record<EquipmentStatus, string> = {
  IDLE: '闲置',
  IN_USE: '使用中',
  LEASED: '已出租',
  MAINTENANCE: '维修中',
  SCRAPPED: '已报废'
}

export const paymentStatusLabels: Record<PaymentStatus, string> = {
  UNPAID: '未付',
  PARTIAL: '部分支付',
  PAID: '已付清'
}

export const projectStatusLabels: Record<ProjectStatus, string> = {
  PLANNING: '规划中',
  IN_PROGRESS: '进行中',
  PAUSED: '已暂停',
  COMPLETED: '已完成',
  CLOSED: '已关闭'
}

export const budgetStatusLabels: Record<BudgetStatus, string> = {
  DRAFT: '草稿',
  ACTIVE: '启用',
  CLOSED: '已关闭'
}

export const approvalStatusLabels: Record<ApprovalStatus, string> = {
  DRAFT: '草稿',
  PENDING: '待审批',
  APPROVED: '已通过',
  REJECTED: '已驳回'
}

export const contractStatusLabels: Record<ContractStatus, string> = {
  DRAFT: '草稿',
  ACTIVE: '履行中',
  COMPLETED: '已完成',
  TERMINATED: '已终止'
}

/** 将枚举标签映射转换为 el-select 可用的 options 数组。 */
export function toOptions<T extends string>(labels: Record<T, string>): { value: T; label: string }[] {
  return (Object.keys(labels) as T[]).map((value) => ({ value, label: labels[value] }))
}
