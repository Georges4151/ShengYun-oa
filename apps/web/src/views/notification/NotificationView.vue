<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  ElButton,
  ElDatePicker,
  ElDialog,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElPagination,
  ElRadio,
  ElRadioButton,
  ElRadioGroup,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTabPane,
  ElTabs,
  ElTag
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { notificationTypeLabels } from '@ceams/types'
import type {
  ContentType,
  Id,
  NotificationCreateRequest,
  NotificationPriority,
  NotificationPublishRequest,
  NotificationPublishScope,
  NotificationRecipientRoleVO,
  NotificationRecipientUserVO,
  NotificationStatus,
  NotificationType,
  NotificationVO,
  UserNotificationPageQuery,
  UserNotificationVO
} from '@ceams/types'
import { formatDateTime } from '@ceams/utils'
import { PageContainer } from '@ceams/ui-web'
import { ceamsApi } from '@/services/http'
import { deepLinkToPath } from '@/services/desktop'
import { hasPermission } from '@/composables/usePermission'
import { apiErrorMessage } from '@/utils/error'
import { pushChannelLabels } from '@/utils/labels'

defineOptions({ name: 'NotificationView' })

/** 日报摘要深链前缀：命中后展示「查看日报摘要」，并派发全局事件由 AI 抽屉处理 */
const SUMMARY_DEEP_LINK = 'ceams://ai/summary'
/** 正文超过该长度（或超过 3 行）时折叠，避免长通知撑高列表 */
const CONTENT_CLAMP_LENGTH = 120

/** 通知类型筛选项的值：空字符串表示「全部」，其余与后端 NotificationType 枚举名一致 */
type NotificationFilterValue = NotificationType | ''

/** 顶部筛选组：仅暴露业务域（审批 / 人力 / 行政 / 文档），系统与公告统一归入「全部」 */
const filterOptions: Array<{ value: NotificationFilterValue; label: string }> = [
  { value: '', label: '全部' },
  { value: 'APPROVAL', label: '审批' },
  { value: 'HR', label: 'HR' },
  { value: 'ADMIN', label: '行政' },
  { value: 'DOC', label: '文档' }
]

const records = ref<UserNotificationVO[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const unreadCount = ref(0)
const loading = ref(false)
/** 当前选中的通知类型过滤（空串 = 全部） */
const filterType = ref<NotificationFilterValue>('')
/** 正文展开状态（按用户通知 id 记录） */
const expanded = ref<Record<string, boolean>>({})

const hasRecords = computed(() => records.value.length > 0)

const router = useRouter()

async function load(): Promise<void> {
  loading.value = true
  try {
    // type 为空时保持原有两参请求行为（不向后端传 type 查询参数）
    const query: UserNotificationPageQuery = { page: page.value, size: size.value }
    if (filterType.value) query.type = filterType.value
    const res = await ceamsApi.notification.userNotificationMine(query)
    records.value = res.records
    total.value = res.total
    unreadCount.value = res.unreadCount
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载通知失败'))
  } finally {
    loading.value = false
  }
}

/** 切换类型筛选：重置到第一页、清空当前列表后重新查询 */
function onFilterChange(): void {
  page.value = 1
  records.value = []
  total.value = 0
  expanded.value = {}
  void load()
}

function onSearch(): void {
  page.value = 1
  void load()
}

/** 通知类型标签；旧数据缺少 notificationType 时返回空串（不渲染标签） */
function typeLabel(item: UserNotificationVO): string {
  return item.notificationType ? notificationTypeLabels[item.notificationType] : ''
}

/**
 * 通知类型对应的标签配色（纯视觉映射，不改动类型枚举与数据）：
 * 审批 warning、人力 success、行政 info、文档 primary，其余中性 info。
 */
function typeTagType(item: UserNotificationVO): 'warning' | 'success' | 'info' | 'primary' {
  switch (item.notificationType) {
    case 'APPROVAL':
      return 'warning'
    case 'HR':
      return 'success'
    case 'ADMIN':
      return 'info'
    case 'DOC':
      return 'primary'
    default:
      return 'info'
  }
}

function displayTitle(item: UserNotificationVO): string {
  return item.title || '系统通知'
}

function displayTime(item: UserNotificationVO): string {
  return formatDateTime(item.publishTime ?? item.createTime)
}

function channelLabel(item: UserNotificationVO): string {
  return pushChannelLabels[item.channel ?? 'SYSTEM']
}

function isSummaryLink(item: UserNotificationVO): boolean {
  return (item.linkUrl ?? '').startsWith(SUMMARY_DEEP_LINK)
}

/** 业务深链（非日报摘要）：标记已读后跳转到对应页面 */
function isNavigableLink(item: UserNotificationVO): boolean {
  return deepLinkToPath(item.linkUrl ?? '') !== null && !isSummaryLink(item)
}

async function openLink(item: UserNotificationVO): Promise<void> {
  await markRead(item)
  const path = deepLinkToPath(item.linkUrl ?? '')
  if (path) void router.push(path)
}

/** 从 linkUrl 查询串解析 date；缺失返回空串 */
function summaryDate(item: UserNotificationVO): string {
  const linkUrl = item.linkUrl ?? ''
  const queryIndex = linkUrl.indexOf('?')
  if (queryIndex === -1) return ''
  const params = new URLSearchParams(linkUrl.slice(queryIndex + 1))
  return params.get('date')?.trim() ?? ''
}

function isLongContent(item: UserNotificationVO): boolean {
  const content = item.content ?? ''
  return content.length > CONTENT_CLAMP_LENGTH || content.split('\n').length > 3
}

function isExpanded(item: UserNotificationVO): boolean {
  return expanded.value[item.id] === true
}

function toggleContent(item: UserNotificationVO): void {
  expanded.value = { ...expanded.value, [item.id]: !isExpanded(item) }
}

async function markRead(item: UserNotificationVO): Promise<void> {
  if (item.isRead === 1) return
  try {
    await ceamsApi.notification.read(item.id)
    item.isRead = 1
    item.readTime = new Date().toISOString()
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '标记已读失败'))
  }
}

async function markAllRead(): Promise<void> {
  if (unreadCount.value === 0) return
  try {
    await ceamsApi.notification.readAll()
    ElMessage.success('已全部标记为已读')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '全部已读失败'))
  }
}

/**
 * 查看日报摘要：先标记已读，再通过全局事件交给 AI 抽屉打开摘要视图。
 * date 从深链查询参数解析，无则不带。
 */
async function openSummary(item: UserNotificationVO): Promise<void> {
  await markRead(item)
  const date = summaryDate(item)
  window.dispatchEvent(
    new CustomEvent('ceams:open-ai-summary', { detail: date ? { date } : {} })
  )
}

// ---------------- 通知管理（发布 / 编辑 / 撤回 / 删除） ----------------

/** 是否具备通知发布权限：无权限时不渲染「通知管理」页签 */
const canPublish = computed(() => hasPermission('notification:publish'))

/** 顶部页签：默认停留在「我的通知」，不改变原有取数逻辑 */
const activeTab = ref<'mine' | 'manage'>('mine')

type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'

/** 优先级中文标签 */
const priorityLabels: Record<NotificationPriority, string> = {
  LOW: '低',
  NORMAL: '普通',
  HIGH: '高',
  URGENT: '紧急'
}

/** 通知状态中文标签 */
const statusLabels: Record<NotificationStatus, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  REVOKED: '已撤销'
}

/** 管理页签筛选项：首项为空值「全部」，其余枚举值与后端一致 */
const manageStatusOptions: Array<{ value: NotificationStatus | ''; label: string }> = [
  { value: '', label: '全部' },
  { value: 'DRAFT', label: '草稿' },
  { value: 'PUBLISHED', label: '已发布' },
  { value: 'REVOKED', label: '已撤销' }
]

const manageTypeOptions: Array<{ value: NotificationType | ''; label: string }> = [
  { value: '', label: '全部' },
  { value: 'SYSTEM', label: notificationTypeLabels.SYSTEM },
  { value: 'APPROVAL', label: notificationTypeLabels.APPROVAL },
  { value: 'ANNOUNCEMENT', label: notificationTypeLabels.ANNOUNCEMENT },
  { value: 'HR', label: notificationTypeLabels.HR },
  { value: 'ADMIN', label: notificationTypeLabels.ADMIN },
  { value: 'DOC', label: notificationTypeLabels.DOC }
]

const managePriorityOptions: Array<{ value: NotificationPriority | ''; label: string }> = [
  { value: '', label: '全部' },
  { value: 'LOW', label: priorityLabels.LOW },
  { value: 'NORMAL', label: priorityLabels.NORMAL },
  { value: 'HIGH', label: priorityLabels.HIGH },
  { value: 'URGENT', label: priorityLabels.URGENT }
]

const manageQuery = reactive<{
  status: NotificationStatus | ''
  type: NotificationType | ''
  priority: NotificationPriority | ''
}>({ status: '', type: '', priority: '' })

const manageRecords = ref<NotificationVO[]>([])
const managePage = ref(1)
const manageSize = ref(20)
const manageTotal = ref(0)
const manageLoading = ref(false)
/** 首次切换到管理页签时才取数，避免无谓请求 */
const manageLoaded = ref(false)

async function loadManage(): Promise<void> {
  manageLoading.value = true
  try {
    const res = await ceamsApi.notification.page({
      page: managePage.value,
      size: manageSize.value,
      status: manageQuery.status || undefined,
      type: manageQuery.type || undefined,
      priority: manageQuery.priority || undefined
    })
    manageRecords.value = res.records
    manageTotal.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载通知列表失败'))
  } finally {
    manageLoading.value = false
  }
}

/** 管理页签查询：回到第一页后重新取数 */
function onManageSearch(): void {
  managePage.value = 1
  void loadManage()
}

/** 每页条数变化：回到第一页 */
function onManageSizeChange(): void {
  managePage.value = 1
  void loadManage()
}

// 首次进入管理页签时惰性加载
watch(activeTab, (tab) => {
  if (tab === 'manage' && !manageLoaded.value) {
    manageLoaded.value = true
    void loadManage()
  }
})

// 权限变化时回退到「我的通知」，避免停留在不可见页签
watch(canPublish, (allowed) => {
  if (!allowed && activeTab.value === 'manage') activeTab.value = 'mine'
})

/** 管理列表的类型标签配色（与「我的通知」保持同一套视觉映射） */
function manageTypeTagType(type: NotificationType): TagType {
  switch (type) {
    case 'APPROVAL':
      return 'warning'
    case 'HR':
      return 'success'
    case 'ADMIN':
      return 'info'
    case 'DOC':
      return 'primary'
    default:
      return 'info'
  }
}

/** 优先级标签配色：低 / 普通 info、高 warning、紧急 danger */
function priorityTagType(priority: NotificationPriority): TagType {
  switch (priority) {
    case 'HIGH':
      return 'warning'
    case 'URGENT':
      return 'danger'
    default:
      return 'info'
  }
}

/** 状态标签配色：草稿 info、已发布 success、已撤销 danger */
function statusTagType(status: NotificationStatus): TagType {
  switch (status) {
    case 'PUBLISHED':
      return 'success'
    case 'REVOKED':
      return 'danger'
    default:
      return 'info'
  }
}

function manageTime(row: NotificationVO): string {
  return formatDateTime(row.publishTime)
}

// ---------------- 新建 / 编辑弹窗 ----------------

interface NotificationFormState {
  title: string
  notificationType: NotificationType
  priority: NotificationPriority
  contentType: ContentType
  content: string
  linkUrl: string
  /** 有效期区间，元素格式与 value-format 一致：YYYY-MM-DDTHH:mm:ss */
  validRange: string[] | null
}

function emptyForm(): NotificationFormState {
  return {
    title: '',
    notificationType: 'ANNOUNCEMENT',
    priority: 'NORMAL',
    contentType: 'TEXT',
    content: '',
    linkUrl: '',
    validRange: null
  }
}

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<string | null>(null)
const submitting = ref(false)
const form = reactive<NotificationFormState>(emptyForm())

const dialogTitle = computed(() => (editingId.value ? '编辑通知' : '新建通知'))

/** 链接地址校验：仅内容类型为 LINK 时必填，且必须为 http(s) 开头 */
function linkUrlValidator(
  _rule: unknown,
  value: unknown,
  callback: (error?: Error) => void
): void {
  if (form.contentType !== 'LINK') {
    callback()
    return
  }
  const text = String(value ?? '').trim()
  if (!text) {
    callback(new Error('请输入链接地址'))
    return
  }
  if (!/^https?:\/\//i.test(text)) {
    callback(new Error('链接地址需以 http:// 或 https:// 开头'))
    return
  }
  callback()
}

const formRules: FormRules<NotificationFormState> = {
  title: [
    { required: true, whitespace: true, message: '请输入通知标题', trigger: 'blur' },
    { max: 100, message: '标题不能超过 100 个字符', trigger: 'blur' }
  ],
  notificationType: [{ required: true, message: '请选择通知类型', trigger: 'change' }],
  linkUrl: [{ validator: linkUrlValidator, trigger: 'blur' }]
}

/** 打开弹窗前统一清空旧校验状态 */
function resetValidation(): void {
  void nextTick(() => formRef.value?.clearValidate())
}

function openCreate(): void {
  editingId.value = null
  Object.assign(form, emptyForm())
  dialogVisible.value = true
  resetValidation()
}

function openEdit(row: NotificationVO): void {
  if (row.status !== 'DRAFT') {
    ElMessage.warning('仅草稿状态的通知可以编辑')
    return
  }
  editingId.value = row.id
  Object.assign(form, {
    title: row.title ?? '',
    notificationType: row.notificationType,
    priority: row.priority ?? 'NORMAL',
    contentType: row.contentType ?? 'TEXT',
    content: row.content ?? '',
    linkUrl: row.linkUrl ?? '',
    validRange:
      row.validStartTime && row.validEndTime ? [row.validStartTime, row.validEndTime] : null
  })
  dialogVisible.value = true
  resetValidation()
}

/** 切换内容类型：纯文本时清空链接并移除其校验提示 */
function onContentTypeChange(): void {
  if (form.contentType === 'TEXT') form.linkUrl = ''
  void nextTick(() => formRef.value?.clearValidate('linkUrl'))
}

async function submitForm(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const payload: NotificationCreateRequest = {
    title: form.title.trim(),
    notificationType: form.notificationType,
    priority: form.priority,
    contentType: form.contentType,
    content: form.content.trim() || null,
    linkUrl: form.contentType === 'LINK' ? form.linkUrl.trim() : null,
    validStartTime: form.validRange?.[0] ?? null,
    validEndTime: form.validRange?.[1] ?? null
  }

  submitting.value = true
  try {
    if (editingId.value) {
      await ceamsApi.notification.update(editingId.value, payload)
      ElMessage.success('通知已更新')
    } else {
      await ceamsApi.notification.create(payload)
      ElMessage.success('通知已创建')
      // 新建后回到第一页，便于看到最新数据
      managePage.value = 1
    }
    dialogVisible.value = false
    manageLoaded.value = true
    await loadManage()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存通知失败'))
  } finally {
    submitting.value = false
  }
}

// ---------------- 发布弹窗（接收范围） ----------------

/** 发布弹窗是否可见 */
const publishVisible = ref(false)
/** 当前待发布的通知（弹窗打开时快照，避免列表刷新后丢失标题） */
const publishTarget = ref<NotificationVO | null>(null)
/** 接收范围：默认全员 */
const publishScope = ref<NotificationPublishScope>('ALL')
/** 「按角色」已选角色 id */
const publishRoleIds = ref<Id[]>([])
/** 「按用户」已选用户 id */
const publishUserIds = ref<Id[]>([])
/** 角色下拉选项（弹窗打开时按需加载一次） */
const roleOptions = ref<NotificationRecipientRoleVO[]>([])
const roleLoading = ref(false)
/** 角色列表是否已成功加载，避免重复请求 */
const rolesLoaded = ref(false)
/** 「按用户」远程搜索返回的候选用户 */
const userOptions = ref<NotificationRecipientUserVO[]>([])
const userLoading = ref(false)
/** 已选用户对象缓存：远程搜索替换候选后仍能正确回显「姓名（账号）」 */
const selectedUsers = ref<NotificationRecipientUserVO[]>([])
/** 发布中：作为按钮 loading，同时防止重复提交 */
const publishing = ref(false)

/** 下拉候选项 = 已选用户 + 本次搜索结果（按 id 去重） */
const userSelectOptions = computed<NotificationRecipientUserVO[]>(() => {
  const merged = [...selectedUsers.value]
  for (const user of userOptions.value) {
    if (!merged.some((item) => item.id === user.id)) merged.push(user)
  }
  return merged
})

/** 用户下拉展示文案：姓名（账号） */
function userLabel(user: NotificationRecipientUserVO): string {
  return `${user.name}（${user.username}）`
}

/** 按需加载角色列表；失败仅提示，不阻塞「全员 / 按用户」范围 */
async function loadRoleOptions(): Promise<void> {
  if (rolesLoaded.value || roleLoading.value) return
  roleLoading.value = true
  try {
    roleOptions.value = await ceamsApi.notification.recipientRoles()
    rolesLoaded.value = true
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载角色列表失败'))
  } finally {
    roleLoading.value = false
  }
}

/** 远程搜索用户（最多 50 条），关键字为空时加载默认候选 */
async function searchUsers(keyword: string): Promise<void> {
  userLoading.value = true
  try {
    userOptions.value = await ceamsApi.notification.recipientUsers(keyword)
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '搜索用户失败'))
  } finally {
    userLoading.value = false
  }
}

/** 已选用户变化：把当前选中项存入缓存，保证标签可回显 */
function onUserSelectionChange(ids: Id[]): void {
  const pool = userSelectOptions.value
  selectedUsers.value = ids
    .map((id) => pool.find((item) => item.id === id))
    .filter((item): item is NotificationRecipientUserVO => item !== undefined)
}

/** 切换接收范围：按需触发角色 / 用户候选加载 */
function onPublishScopeChange(scope: NotificationPublishScope): void {
  if (scope === 'ROLES') void loadRoleOptions()
  if (scope === 'USERS' && userOptions.value.length === 0) void searchUsers('')
}

/** 打开「发布通知」弹窗：重置接收范围并加载角色候选 */
function openPublish(row: NotificationVO): void {
  publishTarget.value = row
  // 默认按角色发布，打开弹窗即可直接勾选接收角色（支持多选）
  publishScope.value = 'ROLES'
  publishRoleIds.value = []
  publishUserIds.value = []
  selectedUsers.value = []
  userOptions.value = []
  publishVisible.value = true
  void loadRoleOptions()
}

/** 提交发布：按范围校验后调用发布接口，返回实际送达人数 */
async function submitPublish(): Promise<void> {
  const target = publishTarget.value
  if (!target || publishing.value) return

  const scope = publishScope.value
  const body: NotificationPublishRequest = { scope }
  if (scope === 'ROLES') {
    if (publishRoleIds.value.length === 0) {
      ElMessage.warning('请至少选择一个角色')
      return
    }
    body.roleIds = [...publishRoleIds.value]
  } else if (scope === 'USERS') {
    if (publishUserIds.value.length === 0) {
      ElMessage.warning('请至少选择一个用户')
      return
    }
    body.userIds = [...publishUserIds.value]
  }

  publishing.value = true
  try {
    const delivered = await ceamsApi.notification.publish(target.id, body)
    ElMessage.success(`发布成功，已送达 ${delivered} 人`)
    publishVisible.value = false
    await loadManage()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '发布通知失败'))
  } finally {
    publishing.value = false
  }
}

// ---------------- 撤回 / 删除 ----------------

async function onRevoke(row: NotificationVO): Promise<void> {
  const confirmed = await ElMessageBox.confirm(
    `确认撤回通知「${row.title}」？撤回后该通知不再展示给用户。`,
    '撤回确认',
    { type: 'warning' }
  ).catch(() => false)
  if (!confirmed) return
  try {
    await ceamsApi.notification.revoke(row.id)
    ElMessage.success('通知已撤回')
    await loadManage()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '撤回通知失败'))
  }
}

async function onRemove(row: NotificationVO): Promise<void> {
  const confirmed = await ElMessageBox.confirm(
    `确认删除通知「${row.title}」？删除后不可恢复。`,
    '删除确认',
    { type: 'warning' }
  ).catch(() => false)
  if (!confirmed) return
  try {
    await ceamsApi.notification.remove(row.id)
    ElMessage.success('通知已删除')
    // 当前页删空时回退一页，避免停留在空页
    if (manageRecords.value.length === 1 && managePage.value > 1) {
      managePage.value -= 1
    }
    await loadManage()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '删除通知失败'))
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <PageContainer title="通知" subtitle="系统与审批通知">
    <template #extra>
      <el-button
        v-if="activeTab === 'mine'"
        type="primary"
        :disabled="unreadCount === 0 || loading"
        @click="markAllRead"
      >
        全部已读
      </el-button>
    </template>

    <div class="notification-view">
      <el-tabs v-model="activeTab" class="notification-tabs">
        <!-- 我的通知：保留原有筛选、列表、已读与分页逻辑 -->
        <el-tab-pane label="我的通知" name="mine">
          <div class="data-card filter-card">
            <el-radio-group v-model="filterType" @change="onFilterChange">
              <el-radio-button v-for="opt in filterOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </el-radio-button>
            </el-radio-group>

            <div class="unread-summary">
              <span class="unread-summary__label">未读</span>
              <span class="unread-summary__count">{{ unreadCount }}</span>
              <span class="unread-summary__unit">条</span>
            </div>
          </div>

          <!-- 列表卡片：标题与条数 -->
          <div v-loading="loading" class="data-card list-card">
            <div class="card-head">
              <div class="card-heading">
                <span class="card-title">通知列表</span>
                <span class="card-sub">共 {{ total }} 条</span>
              </div>
            </div>

            <el-empty v-if="!loading && !hasRecords" class="empty-state" description="暂无通知" />

            <div v-else class="notification-list">
              <div
                v-for="item in records"
                :key="item.id"
                class="notification-item"
                :class="{
                  'notification-item--unread': item.isRead === 0,
                  'notification-item--read': item.isRead !== 0
                }"
              >
                <span v-if="item.isRead === 0" class="notification-item__bar" />

                <div class="notification-item__body">
                  <div class="notification-item__head">
                    <div class="notification-item__title-line">
                      <span class="notification-item__title">{{ displayTitle(item) }}</span>
                      <el-tag v-if="typeLabel(item)" size="small" :type="typeTagType(item)">{{ typeLabel(item) }}</el-tag>
                      <el-tag size="small" type="info" effect="plain">{{ channelLabel(item) }}</el-tag>
                      <el-tag v-if="item.isRead === 0" size="small" type="warning" effect="plain">未读</el-tag>
                      <el-tag v-else size="small" type="info" effect="plain">已读</el-tag>
                    </div>
                    <span class="notification-item__time">{{ displayTime(item) }}</span>
                  </div>

                  <template v-if="item.content">
                    <p
                      class="notification-item__content"
                      :class="{ 'notification-item__content--clamp': isLongContent(item) && !isExpanded(item) }"
                    >
                      {{ item.content }}
                    </p>
                    <el-button
                      v-if="isLongContent(item)"
                      link
                      type="primary"
                      size="small"
                      class="notification-item__toggle"
                      @click="toggleContent(item)"
                    >
                      {{ isExpanded(item) ? '收起' : '展开' }}
                    </el-button>
                  </template>

                  <div class="notification-item__actions">
                    <el-button v-if="item.isRead === 0" link type="primary" size="small" @click="markRead(item)">
                      标记已读
                    </el-button>
                    <el-button v-if="isSummaryLink(item)" link type="primary" size="small" @click="openSummary(item)">
                      查看日报摘要
                    </el-button>
                    <el-button v-if="isNavigableLink(item)" link type="primary" size="small" @click="openLink(item)">
                      查看详情
                    </el-button>
                  </div>
                </div>
              </div>
            </div>

            <el-pagination
              v-if="total > 0"
              v-model:current-page="page"
              v-model:page-size="size"
              :total="total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              class="pagination"
              @size-change="onSearch"
              @current-change="load"
            />
          </div>
        </el-tab-pane>

        <!-- 通知管理：仅具备 notification:publish 权限时渲染 -->
        <el-tab-pane v-if="canPublish" label="通知管理" name="manage">
          <!-- 筛选卡片：状态下拉、类型下拉、优先级下拉、查询按钮，右侧「新建通知」 -->
          <div class="data-card filter-card">
            <el-select
              v-model="manageQuery.status"
              placeholder="全部状态"
              style="width: 140px"
              @change="onManageSearch"
            >
              <el-option
                v-for="opt in manageStatusOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
            <el-select
              v-model="manageQuery.type"
              placeholder="全部类型"
              style="width: 140px"
              @change="onManageSearch"
            >
              <el-option
                v-for="opt in manageTypeOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
            <el-select
              v-model="manageQuery.priority"
              placeholder="全部优先级"
              style="width: 140px"
              @change="onManageSearch"
            >
              <el-option
                v-for="opt in managePriorityOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
            <el-button type="primary" @click="onManageSearch">查询</el-button>

            <div class="manage-actions">
              <el-button type="primary" @click="openCreate">新建通知</el-button>
            </div>
          </div>

          <!-- 表格卡片：分页表格 + 操作列 -->
          <div class="data-card list-card">
            <div class="card-head">
              <div class="card-heading">
                <span class="card-title">通知列表</span>
                <span class="card-sub">共 {{ manageTotal }} 条</span>
              </div>
            </div>

            <el-table v-loading="manageLoading" :data="manageRecords" border>
              <template #empty>
                <el-empty class="empty-state" description="暂无通知" />
              </template>

              <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
              <el-table-column label="类型" width="100">
                <template #default="{ row }">
                  <el-tag size="small" :type="manageTypeTagType((row as NotificationVO).notificationType)">
                    {{ notificationTypeLabels[(row as NotificationVO).notificationType] }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="优先级" width="90">
                <template #default="{ row }">
                  <el-tag size="small" :type="priorityTagType((row as NotificationVO).priority)">
                    {{ priorityLabels[(row as NotificationVO).priority] }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag size="small" :type="statusTagType((row as NotificationVO).status)">
                    {{ statusLabels[(row as NotificationVO).status] }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="发布人" width="110">
                <template #default="{ row }">{{ (row as NotificationVO).senderName ?? '-' }}</template>
              </el-table-column>
              <el-table-column label="发布时间" width="170">
                <template #default="{ row }">{{ manageTime(row as NotificationVO) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="200" fixed="right">
                <template #default="{ row }">
                  <template v-if="(row as NotificationVO).status === 'DRAFT'">
                    <el-button link type="primary" size="small" @click="openEdit(row as NotificationVO)">
                      编辑
                    </el-button>
                    <el-button link type="primary" size="small" @click="openPublish(row as NotificationVO)">
                      发布
                    </el-button>
                    <el-button link type="danger" size="small" @click="onRemove(row as NotificationVO)">
                      删除
                    </el-button>
                  </template>
                  <template v-else-if="(row as NotificationVO).status === 'PUBLISHED'">
                    <el-button link type="warning" size="small" @click="onRevoke(row as NotificationVO)">
                      撤回
                    </el-button>
                  </template>
                  <template v-else>
                    <el-button link type="danger" size="small" @click="onRemove(row as NotificationVO)">
                      删除
                    </el-button>
                  </template>
                </template>
              </el-table-column>
            </el-table>

            <el-pagination
              v-if="manageTotal > 0"
              v-model:current-page="managePage"
              v-model:page-size="manageSize"
              :total="manageTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              class="pagination"
              @size-change="onManageSizeChange"
              @current-change="loadManage"
            />
          </div>
        </el-tab-pane>
      </el-tabs>

      <!-- 新建 / 编辑通知弹窗 -->
      <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="560px"
        :close-on-click-modal="false"
      >
        <el-form ref="formRef" :model="form" :rules="formRules" label-width="96px">
          <el-form-item label="标题" prop="title">
            <el-input
              v-model="form.title"
              maxlength="100"
              show-word-limit
              placeholder="请输入通知标题"
            />
          </el-form-item>
          <el-form-item label="通知类型" prop="notificationType">
            <el-select v-model="form.notificationType" style="width: 100%">
              <el-option
                v-for="(label, value) in notificationTypeLabels"
                :key="value"
                :label="label"
                :value="value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="优先级" prop="priority">
            <el-select v-model="form.priority" style="width: 100%">
              <el-option
                v-for="(label, value) in priorityLabels"
                :key="value"
                :label="label"
                :value="value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="内容类型" prop="contentType">
            <el-radio-group v-model="form.contentType" @change="onContentTypeChange">
              <el-radio-button value="TEXT">纯文本</el-radio-button>
              <el-radio-button value="LINK">链接</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="内容" prop="content">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="4"
              placeholder="请输入通知内容（可选）"
            />
          </el-form-item>
          <el-form-item v-if="form.contentType === 'LINK'" label="链接地址" prop="linkUrl">
            <el-input v-model="form.linkUrl" placeholder="https://" />
          </el-form-item>
          <el-form-item label="有效期" prop="validRange">
            <el-date-picker
              v-model="form.validRange"
              type="datetimerange"
              value-format="YYYY-MM-DDTHH:mm:ss"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              range-separator="至"
              style="width: 100%"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
        </template>
      </el-dialog>

      <!-- 发布通知弹窗：选择接收范围（全员 / 按角色 / 按用户） -->
      <el-dialog
        v-model="publishVisible"
        title="发布通知"
        width="520px"
        :close-on-click-modal="false"
      >
        <el-form label-width="96px">
          <el-form-item label="通知标题">
            <span class="publish-title" :title="publishTarget?.title">
              {{ publishTarget?.title }}
            </span>
          </el-form-item>
          <el-form-item label="接收范围">
            <el-radio-group v-model="publishScope" @change="onPublishScopeChange(publishScope)">
              <el-radio value="ROLES">按角色</el-radio>
              <el-radio value="ALL">全员</el-radio>
              <el-radio value="USERS">按用户</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="publishScope === 'ROLES'" label="选择角色">
            <el-select
              v-model="publishRoleIds"
              multiple
              filterable
              :loading="roleLoading"
              placeholder="请选择接收角色（可多选）"
              style="width: 100%"
            >
              <el-option
                v-for="role in roleOptions"
                :key="role.id"
                :label="role.name"
                :value="role.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="publishScope === 'USERS'" label="选择用户">
            <el-select
              v-model="publishUserIds"
              multiple
              filterable
              remote
              reserve-keyword
              :remote-method="searchUsers"
              :loading="userLoading"
              placeholder="输入姓名或账号搜索（可多选）"
              style="width: 100%"
              @change="onUserSelectionChange"
            >
              <el-option
                v-for="user in userSelectOptions"
                :key="user.id"
                :label="userLabel(user)"
                :value="user.id"
              />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button :disabled="publishing" @click="publishVisible = false">取消</el-button>
          <el-button type="primary" :loading="publishing" @click="submitPublish">
            确定发布
          </el-button>
        </template>
      </el-dialog>
    </div>
  </PageContainer>
</template>

<style scoped>
/* 页签：与内容保持 16px 间距，选中项主色加粗 */
.notification-tabs :deep(.el-tabs__header) {
  margin-bottom: var(--ceams-space-4);
}

.notification-tabs :deep(.el-tabs__item) {
  font-size: var(--ceams-font-size-base);
  color: var(--ceams-text-secondary);
}

.notification-tabs :deep(.el-tabs__item.is-active) {
  font-weight: 600;
  color: var(--ceams-color-primary);
}

.notification-tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: var(--ceams-border-divider);
}

/* 筛选卡片：控件 12px 间距，未读统计靠右 */
.filter-card {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--ceams-space-3);
  padding: 16px 20px;
  margin-bottom: var(--ceams-space-4);
}

/* 管理页签右侧操作区 */
.manage-actions {
  display: flex;
  align-items: center;
  gap: var(--ceams-space-2);
  margin-left: auto;
}

.unread-summary {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-left: auto;
}

.unread-summary__label,
.unread-summary__unit {
  font-size: var(--ceams-font-size-xs);
  color: var(--ceams-text-secondary);
}

.unread-summary__count {
  font-size: var(--ceams-font-size-xl);
  font-weight: 600;
  color: var(--ceams-color-primary);
  font-variant-numeric: tabular-nums;
}

/* 列表卡片：20px 内边距 */
.list-card {
  min-height: 140px;
  padding: 20px;
}

.card-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--ceams-space-3);
  margin-bottom: var(--ceams-space-4);
}

.card-heading {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--ceams-space-2);
}

.card-title {
  font-size: var(--ceams-font-size-md);
  font-weight: 600;
  color: var(--ceams-text-primary);
}

.card-sub {
  font-size: var(--ceams-font-size-xs);
  color: var(--ceams-text-secondary);
}

/* 未读项：浅蓝底 + 左侧 3px 主色竖条 */
.notification-item {
  position: relative;
  display: flex;
  padding: 14px 16px 14px 18px;
  border-bottom: 1px solid var(--ceams-border-divider);
  border-radius: var(--ceams-radius-sm);
  transition: background-color 0.16s ease, opacity 0.16s ease;
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item--unread {
  background-color: var(--ceams-color-primary-light);
}

/* 已读项降低不透明度，悬停还原，保持层级区分 */
.notification-item--read {
  opacity: 0.68;
}

.notification-item--read:hover {
  opacity: 1;
}

.notification-item__bar {
  position: absolute;
  top: 14px;
  bottom: 14px;
  left: 0;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background-color: var(--ceams-color-primary);
}

.notification-item__body {
  flex: 1;
  min-width: 0;
}

.notification-item__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ceams-space-3);
}

.notification-item__title-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--ceams-space-2);
  min-width: 0;
}

.notification-item__title {
  overflow: hidden;
  font-size: var(--ceams-font-size-base);
  font-weight: 600;
  color: var(--ceams-text-primary);
  white-space: nowrap;
  text-overflow: ellipsis;
}

.notification-item__time {
  flex-shrink: 0;
  font-size: var(--ceams-font-size-xs);
  color: var(--ceams-text-secondary);
  font-variant-numeric: tabular-nums;
}

.notification-item__content {
  margin: 8px 0 0;
  font-size: var(--ceams-font-size-sm);
  line-height: 1.7;
  color: var(--ceams-text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
}

.notification-item__content--clamp {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.notification-item__toggle {
  margin-top: 4px;
  padding: 0;
}

.notification-item__actions {
  display: flex;
  align-items: center;
  gap: var(--ceams-space-2);
  min-height: 24px;
  margin-top: 4px;
}

/* 分页右对齐，距表格 16px */
.pagination {
  margin-top: var(--ceams-space-4);
  justify-content: flex-end;
}

/* 发布弹窗：只读通知标题，超长省略 */
.publish-title {
  display: block;
  width: 100%;
  overflow: hidden;
  color: var(--ceams-text-primary);
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* 空态：居中灰色 + 40px 留白 */
.empty-state {
  padding: 40px 0;
}

.empty-state :deep(.el-empty__description p) {
  color: var(--ceams-text-secondary);
}
</style>
