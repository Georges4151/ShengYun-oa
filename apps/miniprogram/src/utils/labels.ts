import type {
  AccountStatus,
  AccountType,
  AllocationStatus,
  ApprovalStatus,
  AssetCategory,
  AssetStatus,
  HrApprovalStatus,
  HrCheckinStatus,
  HrCheckinType,
  HrContractStatus,
  HrContractType,
  HrEmployeeStatus,
  HrEmploymentType,
  HrLeaveType,
  HrPerformanceReviewStatus,
  HrSalarySlipReadStatus,
  NotificationPriority,
  NotificationType,
  PaymentMethod,
  PaymentStatus,
  ProjectStatus,
  PushChannel,
  PushStatus,
  ReceiptApprovalStatus,
  ReceiptType,
  RepairCategory,
  RepairStatus,
  VerificationStatus,
  VehicleStatus
} from '@ceams/types'

export const projectStatusLabels: Record<ProjectStatus, string> = {
  PLANNING: '规划中',
  IN_PROGRESS: '进行中',
  PAUSED: '已暂停',
  COMPLETED: '已完成',
  CLOSED: '已关闭'
}

export const approvalStatusLabels: Record<ApprovalStatus, string> = {
  DRAFT: '草稿',
  PENDING: '审批中',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  CANCELLED: '已终止'
}

export const receiptTypeLabels: Record<ReceiptType, string> = {
  TRANSPORTATION: '交通',
  ACCOMMODATION: '住宿',
  MEAL: '餐饮',
  OFFICE: '办公',
  OTHER: '其他'
}

export const verificationStatusLabels: Record<VerificationStatus, string> = {
  PENDING: '待核验',
  VERIFIED: '已核验',
  REJECTED: '已驳回'
}

export const paymentMethodLabels: Record<PaymentMethod, string> = {
  CASH: '现金',
  BANK_TRANSFER: '银行转账',
  WECHAT: '微信',
  ALIPAY: '支付宝',
  OTHER: '其他'
}

export const receiptApprovalStatusLabels: Record<ReceiptApprovalStatus, string> = {
  DRAFT: '草稿',
  PENDING: '审批中',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  CANCELLED: '已终止'
}

// ---------- 流程域（bpm） ----------

export const bpmBizTypeLabels: Record<string, string> = {
  EXPENSE: '报销',
  CHANGE_REQUEST: '项目变更',
  HR_REGULAR: '转正',
  HR_TRANSFER: '调岗',
  HR_RESIGN: '离职',
  HR_LEAVE: '请假',
  HR_CHECKIN_SUPPLEMENT: '补卡',
  // M6 行政域流程（P5 待办页复用，提前补齐避免二次改动）
  ADMIN_ASSET_BORROW: '资产领用',
  ADMIN_VEHICLE_APPLY: '用车',
  ADMIN_MEETING_APPLY: '会议室预定'
}

export const bpmInstanceStatusLabels: Record<string, string> = {
  DRAFT: '草稿',
  RUNNING: '审批中',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  WITHDRAWN: '已撤回',
  TERMINATED: '已终止'
}

export const bpmTaskStatusLabels: Record<string, string> = {
  PENDING: '待处理',
  PENDING_ASSIGNEE: '待分配审批人',
  APPROVED: '已同意',
  REJECTED: '已驳回',
  CLOSED: '已关闭'
}

export const bpmNodeStatusLabels: Record<string, string> = {
  PENDING: '未到达',
  ACTIVE: '处理中',
  COMPLETED: '已完成',
  REJECTED: '已驳回',
  CLOSED: '已关闭'
}

export const paymentStatusLabels: Record<PaymentStatus, string> = {
  UNPAID: '未支付',
  PARTIAL: '部分支付',
  PAID: '已支付'
}

export const accountTypeLabels: Record<AccountType, string> = {
  INCOME: '收入',
  EXPENSE: '支出'
}

export const accountStatusLabels: Record<AccountStatus, string> = {
  DRAFT: '草稿',
  CONFIRMED: '已确认',
  CANCELLED: '已取消'
}

export const allocationStatusLabels: Record<AllocationStatus, string> = {
  UNALLOCATED: '未分摊',
  PARTIAL: '部分分摊',
  ALLOCATED: '已分摊'
}

export const notificationTypeLabels: Record<NotificationType, string> = {
  SYSTEM: '系统',
  APPROVAL: '审批',
  ANNOUNCEMENT: '公告',
  HR: '人力',
  ADMIN: '行政',
  DOC: '文档'
}

export const notificationPriorityLabels: Record<NotificationPriority, string> = {
  LOW: '低',
  NORMAL: '普通',
  HIGH: '高',
  URGENT: '紧急'
}

export const pushChannelLabels: Record<PushChannel, string> = {
  SYSTEM: '系统',
  EMAIL: '邮件',
  SMS: '短信'
}

export const pushStatusLabels: Record<PushStatus, string> = {
  PENDING: '待发送',
  SENT: '已发送',
  FAILED: '发送失败'
}

// ---------- 人事域（hr） ----------

export const hrEmployeeStatusLabels: Record<HrEmployeeStatus, string> = {
  PROBATION: '试用期',
  ACTIVE: '在职',
  LEAVE: '休假中',
  TERMINATED: '已离职'
}

export const hrEmploymentTypeLabels: Record<HrEmploymentType, string> = {
  FULL_TIME: '全职',
  PART_TIME: '兼职',
  INTERN: '实习',
  CONTRACTOR: '外包'
}

export const hrContractTypeLabels: Record<HrContractType, string> = {
  FIXED: '固定期限',
  UNFIXED: '无固定期限',
  INTERN: '实习协议'
}

export const hrContractStatusLabels: Record<HrContractStatus, string> = {
  ACTIVE: '履行中',
  EXPIRED: '已到期',
  TERMINATED: '已终止'
}

export const hrLeaveTypeLabels: Record<HrLeaveType, string> = {
  ANNUAL: '年假',
  SICK: '病假',
  PERSONAL: '事假'
}

export const hrApprovalStatusLabels: Record<HrApprovalStatus, string> = {
  PENDING: '审批中',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  WITHDRAWN: '已撤回',
  CANCELLED: '已取消',
  TERMINATED: '已终止'
}

export const hrCheckinTypeLabels: Record<HrCheckinType, string> = {
  IN: '上班',
  OUT: '下班'
}

export const hrCheckinStatusLabels: Record<HrCheckinStatus, string> = {
  NORMAL: '正常',
  LATE: '迟到',
  EARLY: '早退',
  ABSENT: '缺卡'
}

export const hrSalarySlipReadStatusLabels: Record<HrSalarySlipReadStatus, string> = {
  PENDING: '未读',
  READ: '已读'
}

export const hrPerformanceReviewStatusLabels: Record<HrPerformanceReviewStatus, string> = {
  DRAFT: '待自评',
  SELF_SUBMITTED: '自评已提交',
  LEADER_DONE: '上级已评分',
  APPROVED: '已通过',
  REJECTED: '已驳回'
}

// ---------- 行政后勤域（admin） ----------

export const assetCategoryLabels: Record<AssetCategory, string> = {
  ELECTRONIC: '电子设备',
  FURNITURE: '办公家具',
  VEHICLE_TOOL: '车辆工具',
  OTHER: '其他'
}

export const assetStatusLabels: Record<AssetStatus, string> = {
  IN_STOCK: '在库',
  BORROWED: '已借出',
  REPAIRING: '维修中',
  SCRAPPED: '已报废'
}

export const vehicleStatusLabels: Record<VehicleStatus, string> = {
  IDLE: '空闲',
  USING: '使用中',
  MAINTENANCE: '维修保养'
}

export const repairCategoryLabels: Record<RepairCategory, string> = {
  ELECTRIC: '电气维修',
  WATER: '给排水维修',
  FURNITURE: '家具维修',
  IT: '信息技术',
  OTHER: '其他'
}

export const repairStatusLabels: Record<RepairStatus, string> = {
  SUBMITTED: '待派单',
  ASSIGNED: '已派单',
  PROCESSING: '处理中',
  DONE: '已完工',
  ACCEPTED: '已验收',
  REJECTED: '已驳回'
}
