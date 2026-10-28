import type {
  AccountStatus,
  AccountType,
  AllocationStatus,
  ApprovalStatus,
  NotificationPriority,
  NotificationType,
  PaymentStatus,
  ProjectStatus,
  PushChannel,
  PushStatus,
  ReceiptApprovalStatus,
  ReceiptType,
  VerificationStatus
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
  PENDING: '待审批',
  APPROVED: '已通过',
  REJECTED: '已驳回'
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

export const receiptApprovalStatusLabels: Record<ReceiptApprovalStatus, string> = {
  DRAFT: '草稿',
  PENDING: '待审批',
  APPROVED: '已通过',
  REJECTED: '已驳回'
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
  ANNOUNCEMENT: '公告'
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
