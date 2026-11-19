<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  ElButton,
  ElDatePicker,
  ElDialog,
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
  ElSwitch,
  ElTable,
  ElTableColumn,
  ElTabPane,
  ElTabs,
  ElTag
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ceamsApi } from '@/services/http'
import type {
  CcRecordVO,
  DelegateVO,
  InstanceVO,
  RejectTarget,
  TaskVO,
  TemplateVO,
  UserOptionVO
} from '@ceams/types'
import { useAuthStore } from '@/stores/auth'
import { hasPermission } from '@/composables/usePermission'
import { apiErrorMessage } from '@/utils/error'
import {
  bpmBizTypeLabels,
  bpmInstanceStatusLabels,
  bpmTaskStatusLabels,
  bpmTemplateStatusLabels,
  bpmTemplateVersionStatusLabels
} from '@/utils/labels'
import BpmInstanceDrawer from './components/BpmInstanceDrawer.vue'
import FlowDesigner from './components/FlowDesigner.vue'

defineOptions({ name: 'BpmView' })

const auth = useAuthStore()
const isAdmin = computed(() => auth.roles.some((role) => role.code === 'SYS_ADMIN'))

const activeTab = ref('pending')

// ---------------- 待办 ----------------
const pending = reactive({ page: 1, size: 20, total: 0 })
const pendingList = ref<TaskVO[]>([])
const pendingLoading = ref(false)
const pendingBizType = ref<string>('')
const onlyOverdue = ref(false)

async function loadPending(): Promise<void> {
  pendingLoading.value = true
  try {
    const res = await ceamsApi.bpm.pendingTasks({
      page: pending.page,
      size: pending.size,
      bizType: pendingBizType.value || undefined,
      overdue: onlyOverdue.value || undefined
    })
    pendingList.value = res.records
    pending.total = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载待办失败'))
  } finally {
    pendingLoading.value = false
  }
}

// ---------------- 我发起 ----------------
const initiated = reactive({ page: 1, size: 20, total: 0 })
const initiatedList = ref<InstanceVO[]>([])
const initiatedLoading = ref(false)

async function loadInitiated(): Promise<void> {
  initiatedLoading.value = true
  try {
    const res = await ceamsApi.bpm.initiated({ page: initiated.page, size: initiated.size })
    initiatedList.value = res.records
    initiated.total = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载我发起的流程失败'))
  } finally {
    initiatedLoading.value = false
  }
}

// ---------------- 我已办 / 抄送 ----------------
const done = reactive({ page: 1, size: 20, total: 0 })
const doneList = ref<TaskVO[]>([])
const doneLoading = ref(false)

async function loadDone(): Promise<void> {
  doneLoading.value = true
  try {
    const res = await ceamsApi.bpm.doneTasks({ page: done.page, size: done.size })
    doneList.value = res.records
    done.total = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载已办失败'))
  } finally {
    doneLoading.value = false
  }
}

const cc = reactive({ page: 1, size: 20, total: 0 })
const ccList = ref<CcRecordVO[]>([])
const ccLoading = ref(false)

async function loadCc(): Promise<void> {
  ccLoading.value = true
  try {
    const res = await ceamsApi.bpm.ccRecords({ page: cc.page, size: cc.size })
    ccList.value = res.records
    cc.total = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载抄送失败'))
  } finally {
    ccLoading.value = false
  }
}

// ---------------- 模板 ----------------
const templates = ref<TemplateVO[]>([])
const templateLoading = ref(false)
const publishVisible = ref(false)
const publishTemplateId = ref('')
const publishSchema = ref('')
const publishEffectiveTime = ref('')
const publishMode = ref<'visual' | 'json'>('visual')
const designerRef = ref<InstanceType<typeof FlowDesigner> | null>(null)

async function loadTemplates(): Promise<void> {
  templateLoading.value = true
  try {
    const res = await ceamsApi.bpm.templatePage({ page: 1, size: 50 })
    templates.value = res.records
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载流程模板失败'))
  } finally {
    templateLoading.value = false
  }
}

async function openPublish(row: TemplateVO): Promise<void> {
  publishTemplateId.value = row.id
  publishEffectiveTime.value = ''
  publishMode.value = 'visual'
  try {
    const versions = await ceamsApi.bpm.templateVersions(row.id)
    const latest = versions[versions.length - 1]
    publishSchema.value = latest?.schemaJson ?? '{"nodes":[]}'
  } catch {
    publishSchema.value = '{"nodes":[]}'
  }
  publishVisible.value = true
  await nextTick()
  designerRef.value?.load(publishSchema.value)
}

/** 从 JSON 模式切回可视化时重新解析当前文本，避免画布停留在旧结构。 */
function onPublishModeChange(mode: string | number | boolean | undefined): void {
  if (mode !== 'visual') return
  void nextTick(() => designerRef.value?.load(publishSchema.value))
}

async function submitPublish(): Promise<void> {
  try {
    JSON.parse(publishSchema.value)
  } catch {
    ElMessage.error('流程定义不是合法 JSON')
    return
  }
  try {
    await ceamsApi.bpm.templatePublish(publishTemplateId.value, {
      schemaJson: publishSchema.value,
      effectiveTime: publishEffectiveTime.value || undefined
    })
    ElMessage.success('新版本已发布，仅对新发起的单据生效')
    publishVisible.value = false
    await loadTemplates()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '发布失败'))
  }
}

// ---------------- 模板新增 ----------------
const createVisible = ref(false)
const createSaving = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({ code: '', name: '', bizType: '' })

const createRules: FormRules<typeof createForm> = {
  code: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  bizType: [{ required: true, message: '请选择业务类型', trigger: 'change' }]
}

function openCreateTemplate(): void {
  Object.assign(createForm, { code: '', name: '', bizType: '' })
  createVisible.value = true
  void nextTick(() => createFormRef.value?.clearValidate())
}

async function submitCreateTemplate(): Promise<void> {
  if (!createFormRef.value) return
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  createSaving.value = true
  try {
    await ceamsApi.bpm.templateCreate({
      code: createForm.code.trim(),
      name: createForm.name.trim(),
      bizType: createForm.bizType
    })
    ElMessage.success('模板已创建，请发布首个版本后再启用')
    createVisible.value = false
    await loadTemplates()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '创建模板失败'))
  } finally {
    createSaving.value = false
  }
}

async function deleteTemplate(row: TemplateVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除模板「${row.name}」吗？删除后不可恢复；已有流程实例的模板不能删除。`,
      '删除模板',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await ceamsApi.bpm.templateDelete(row.id)
    ElMessage.success('删除成功')
    await loadTemplates()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '删除失败'))
  }
}

async function toggleTemplate(row: TemplateVO): Promise<void> {
  try {
    if (row.status === 'ENABLED') {
      await ceamsApi.bpm.templateDisable(row.id)
      ElMessage.success('已停用')
    } else {
      await ceamsApi.bpm.templateEnable(row.id)
      ElMessage.success('已启用')
    }
    await loadTemplates()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '操作失败'))
  }
}

// ---------------- 代理设置 ----------------
const delegates = reactive({ page: 1, size: 20, total: 0 })
const delegateList = ref<DelegateVO[]>([])
const delegateLoading = ref(false)
const delegateVisible = ref(false)
const delegateSaving = ref(false)
const candidateOptions = ref<UserOptionVO[]>([])
const candidateLoading = ref(false)
const delegateForm = reactive<{
  delegateId: string
  bizType: string
  startTime: string
  endTime: string
  reason: string
}>({ delegateId: '', bizType: '', startTime: '', endTime: '', reason: '' })

async function loadDelegates(): Promise<void> {
  delegateLoading.value = true
  try {
    const res = await ceamsApi.bpm.delegatePage({ page: delegates.page, size: delegates.size })
    delegateList.value = res.records
    delegates.total = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载代理设置失败'))
  } finally {
    delegateLoading.value = false
  }
}

async function searchCandidates(keyword: string): Promise<void> {
  candidateLoading.value = true
  try {
    candidateOptions.value = await ceamsApi.bpm.delegateCandidates(keyword || undefined)
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载人员失败'))
  } finally {
    candidateLoading.value = false
  }
}

function openDelegateCreate(): void {
  delegateForm.delegateId = ''
  delegateForm.bizType = ''
  delegateForm.startTime = ''
  delegateForm.endTime = ''
  delegateForm.reason = ''
  delegateVisible.value = true
  void searchCandidates('')
}

async function submitDelegate(): Promise<void> {
  if (!delegateForm.delegateId) {
    ElMessage.warning('请选择代理人')
    return
  }
  if (!delegateForm.startTime || !delegateForm.endTime) {
    ElMessage.warning('请选择代理时间段')
    return
  }
  delegateSaving.value = true
  try {
    await ceamsApi.bpm.delegateCreate({
      delegateId: delegateForm.delegateId,
      bizType: delegateForm.bizType || null,
      startTime: delegateForm.startTime,
      endTime: delegateForm.endTime,
      reason: delegateForm.reason || undefined
    })
    ElMessage.success('代理已生效，时段内科目的待办将转交代理人')
    delegateVisible.value = false
    await loadDelegates()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '新建代理失败'))
  } finally {
    delegateSaving.value = false
  }
}

async function cancelDelegate(row: DelegateVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定停用对 ${row.delegateName || row.delegateId} 的代理吗？`, '停用代理', {
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await ceamsApi.bpm.delegateCancel(row.id)
    ElMessage.success('已停用')
    await loadDelegates()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '停用失败'))
  }
}

/** 代理状态展示：优先按是否处于代理时段给出“生效中”。 */
function delegateStateText(row: DelegateVO): string {
  if (row.effective) return '生效中'
  if (row.status === 'DISABLED') return '已停用'
  return Date.parse(row.startTime) > Date.now() ? '未开始' : '已过期'
}

// ---------------- 审批动作 ----------------
async function approve(row: TaskVO): Promise<void> {
  try {
    const { value } = await ElMessageBox.prompt('请输入审批意见', '审批通过', {
      confirmButtonText: '通过',
      cancelButtonText: '取消',
      inputType: 'textarea'
    })
    await ceamsApi.bpm.approveTask(row.id, { taskVersion: row.taskVersion, comment: value || undefined })
    ElMessage.success('已通过')
    await loadPending()
  } catch (err) {
    if (err === 'cancel' || err === 'close') return
    ElMessage.error(apiErrorMessage(err, '审批失败，请刷新后重试'))
  }
}

const rejectVisible = ref(false)
const rejectTask = ref<TaskVO | null>(null)
const rejectForm = reactive<{ target: RejectTarget; reason: string }>({ target: 'INITIATOR', reason: '' })

function openReject(row: TaskVO): void {
  rejectTask.value = row
  rejectForm.target = 'INITIATOR'
  rejectForm.reason = ''
  rejectVisible.value = true
}

async function submitReject(): Promise<void> {
  if (!rejectTask.value) return
  try {
    await ceamsApi.bpm.rejectTask(rejectTask.value.id, {
      taskVersion: rejectTask.value.taskVersion,
      target: rejectForm.target,
      reason: rejectForm.reason || undefined
    })
    ElMessage.success('已驳回')
    rejectVisible.value = false
    await loadPending()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '驳回失败，请刷新后重试'))
  }
}

async function withdraw(row: InstanceVO): Promise<void> {
  try {
    await ElMessageBox.confirm('撤回后流程结束，单据回到草稿态，确定撤回吗？', '撤回流程', {
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await ceamsApi.bpm.withdrawInstance(row.id, { instanceVersion: row.version })
    ElMessage.success('已撤回')
    await loadInitiated()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '撤回失败'))
  }
}

async function terminate(row: InstanceVO): Promise<void> {
  try {
    const { value } = await ElMessageBox.prompt('请输入终止原因', '终止流程', {
      confirmButtonText: '终止',
      cancelButtonText: '取消',
      inputType: 'textarea'
    })
    await ceamsApi.bpm.terminateInstance(row.id, { instanceVersion: row.version, reason: value || undefined })
    ElMessage.success('已终止')
    await loadInitiated()
  } catch (err) {
    if (err === 'cancel' || err === 'close') return
    ElMessage.error(apiErrorMessage(err, '终止失败'))
  }
}

// ---------------- 流程详情 ----------------
const drawerVisible = ref(false)
const drawerInstanceId = ref<string | null>(null)

function openInstance(instanceId?: string | null): void {
  if (!instanceId) return
  drawerInstanceId.value = instanceId
  drawerVisible.value = true
}

function onTabChange(name: string | number): void {
  if (name === 'pending') void loadPending()
  if (name === 'initiated') void loadInitiated()
  if (name === 'done') void loadDone()
  if (name === 'cc') void loadCc()
  if (name === 'delegate') void loadDelegates()
  if (name === 'template') void loadTemplates()
}

/** 通知/桌面端深链入口：?tab=cc&instanceId=xx → 切到对应页签并打开流程详情 */
const route = useRoute()
const TAB_NAMES = ['pending', 'initiated', 'done', 'cc']

function applyRouteQuery(): void {
  const tab = typeof route.query.tab === 'string' ? route.query.tab : ''
  if (TAB_NAMES.includes(tab)) activeTab.value = tab
  const instanceId = typeof route.query.instanceId === 'string' ? route.query.instanceId : ''
  if (instanceId) openInstance(instanceId)
}

onMounted(() => {
  applyRouteQuery()
  onTabChange(activeTab.value)
})

watch(
  () => route.query,
  () => {
    applyRouteQuery()
    onTabChange(activeTab.value)
  }
)
</script>

<template>
  <div>
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="我的待办" name="pending">
        <div class="toolbar">
          <el-select v-model="pendingBizType" placeholder="业务类型" clearable style="width: 160px">
            <el-option v-for="(label, value) in bpmBizTypeLabels" :key="value" :label="label" :value="value" />
          </el-select>
          <span class="overdue">
            仅看超时
            <el-switch v-model="onlyOverdue" @change="loadPending" />
          </span>
          <el-button type="primary" @click="loadPending">刷新</el-button>
        </div>
        <el-table v-loading="pendingLoading" :data="pendingList" border>
          <el-table-column label="业务类型" width="110">
            <template #default="{ row }">
              <el-tag size="small">{{ bpmBizTypeLabels[(row as TaskVO).bizType ?? ''] ?? row.bizType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="bizId" label="单据 ID" width="120" />
          <el-table-column prop="initiatorName" label="发起人" width="110" />
          <el-table-column label="代理" width="120">
            <template #default="{ row }">
              <el-tag v-if="(row as TaskVO).ownerAssigneeId" size="small" type="warning">
                代 {{ (row as TaskVO).ownerAssigneeName || (row as TaskVO).ownerAssigneeId }}
              </el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="currentNodeName" label="当前节点" width="140" />
          <el-table-column prop="dueAt" label="到期时间" width="170" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag
                size="small"
                :type="(row as TaskVO).status === 'PENDING' ? 'primary' : 'warning'"
              >
                {{ bpmTaskStatusLabels[(row as TaskVO).status] ?? row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openInstance((row as TaskVO).instanceId)">查看流程</el-button>
              <el-button
                v-if="(row as TaskVO).status === 'PENDING'"
                link
                type="success"
                size="small"
                @click="approve(row as TaskVO)"
              >
                同意
              </el-button>
              <el-button
                v-if="(row as TaskVO).status === 'PENDING'"
                link
                type="warning"
                size="small"
                @click="openReject(row as TaskVO)"
              >
                驳回
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="pending.page"
          v-model:page-size="pending.size"
          :total="pending.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          class="pagination"
          @size-change="loadPending"
          @current-change="loadPending"
        />
      </el-tab-pane>

      <el-tab-pane label="我发起" name="initiated">
        <el-table v-loading="initiatedLoading" :data="initiatedList" border>
          <el-table-column label="业务类型" width="110">
            <template #default="{ row }">
              <el-tag size="small">{{ bpmBizTypeLabels[(row as InstanceVO).bizType] ?? row.bizType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="bizId" label="单据 ID" width="120" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag size="small">{{ bpmInstanceStatusLabels[(row as InstanceVO).status] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="startedAt" label="发起时间" width="180" />
          <el-table-column prop="endedAt" label="结束时间" width="180" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openInstance((row as InstanceVO).id)">查看流程</el-button>
              <el-button
                v-if="(row as InstanceVO).status === 'RUNNING'"
                link
                type="warning"
                size="small"
                @click="withdraw(row as InstanceVO)"
              >
                撤回
              </el-button>
              <el-button
                v-if="isAdmin && (row as InstanceVO).status === 'RUNNING'"
                link
                type="danger"
                size="small"
                @click="terminate(row as InstanceVO)"
              >
                终止
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="initiated.page"
          v-model:page-size="initiated.size"
          :total="initiated.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          class="pagination"
          @size-change="loadInitiated"
          @current-change="loadInitiated"
        />
      </el-tab-pane>

      <el-tab-pane label="我已办" name="done">
        <el-table v-loading="doneLoading" :data="doneList" border>
          <el-table-column label="业务类型" width="110">
            <template #default="{ row }">
              <el-tag size="small">{{ bpmBizTypeLabels[(row as TaskVO).bizType ?? ''] ?? row.bizType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="bizId" label="单据 ID" width="120" />
          <el-table-column prop="initiatorName" label="发起人" width="110" />
          <el-table-column prop="currentNodeName" label="节点" width="140" />
          <el-table-column label="处理结果" width="110">
            <template #default="{ row }">
              <el-tag size="small">{{ bpmTaskStatusLabels[(row as TaskVO).status] ?? row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="updateTime" label="处理时间" min-width="170" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openInstance((row as TaskVO).instanceId)">查看流程</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="done.page"
          v-model:page-size="done.size"
          :total="done.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          class="pagination"
          @size-change="loadDone"
          @current-change="loadDone"
        />
      </el-tab-pane>

      <el-tab-pane label="抄送我的" name="cc">
        <el-table v-loading="ccLoading" :data="ccList" border>
          <el-table-column label="业务类型" width="110">
            <template #default="{ row }">
              <el-tag size="small">{{ bpmBizTypeLabels[(row as CcRecordVO).bizType ?? ''] ?? row.bizType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="bizId" label="单据 ID" width="120" />
          <el-table-column prop="initiatorName" label="发起人" width="110" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="(row as CcRecordVO).readStatus === 'UNREAD' ? 'warning' : 'info'">
                {{ (row as CcRecordVO).readStatus === 'UNREAD' ? '未读' : '已读' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="抄送时间" width="180" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openInstance((row as CcRecordVO).instanceId)">查看流程</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="cc.page"
          v-model:page-size="cc.size"
          :total="cc.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          class="pagination"
          @size-change="loadCc"
          @current-change="loadCc"
        />
      </el-tab-pane>

      <el-tab-pane label="代理设置" name="delegate">
        <div class="toolbar">
          <el-button type="primary" @click="openDelegateCreate">新建代理</el-button>
          <el-button @click="loadDelegates">刷新</el-button>
          <span class="hint">代理时段内到达的待办自动转交代理人，代理人处理后记录仍归属原审批人</span>
        </div>
        <el-table v-loading="delegateLoading" :data="delegateList" border>
          <el-table-column label="代理人" min-width="130">
            <template #default="{ row }">
              {{ (row as DelegateVO).delegateName || (row as DelegateVO).delegateId }}
            </template>
          </el-table-column>
          <el-table-column label="适用范围" width="120">
            <template #default="{ row }">
              {{ (row as DelegateVO).bizType ? bpmBizTypeLabels[(row as DelegateVO).bizType!] ?? (row as DelegateVO).bizType : '全部业务' }}
            </template>
          </el-table-column>
          <el-table-column prop="startTime" label="开始时间" width="180" />
          <el-table-column prop="endTime" label="结束时间" width="180" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag
                size="small"
                :type="(row as DelegateVO).effective ? 'success' : 'info'"
              >
                {{ delegateStateText(row as DelegateVO) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="原因" min-width="160" show-overflow-tooltip />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="(row as DelegateVO).status === 'ENABLED'"
                link
                type="warning"
                size="small"
                @click="cancelDelegate(row as DelegateVO)"
              >
                停用
              </el-button>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="delegates.page"
          v-model:page-size="delegates.size"
          :total="delegates.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          class="pagination"
          @size-change="loadDelegates"
          @current-change="loadDelegates"
        />
      </el-tab-pane>

      <el-tab-pane v-if="isAdmin" label="流程模板" name="template">
        <div class="toolbar">
          <el-button type="primary" @click="loadTemplates">刷新</el-button>
          <el-button v-if="hasPermission('bpm:template:create')" @click="openCreateTemplate">新增模板</el-button>
          <span class="hint">发布新版本只对新发起的单据生效，历史实例锁定原版本</span>
        </div>
        <el-table v-loading="templateLoading" :data="templates" border>
          <el-table-column prop="code" label="模板编码" width="160" />
          <el-table-column prop="name" label="模板名称" width="160" />
          <el-table-column label="业务类型" width="110">
            <template #default="{ row }">
              {{ bpmBizTypeLabels[(row as TemplateVO).bizType] ?? row.bizType }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small">{{ bpmTemplateStatusLabels[(row as TemplateVO).status] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="当前版本" width="110">
            <template #default="{ row }">
              {{ (row as TemplateVO).currentVersion?.versionNo ?? '-' }}
            </template>
          </el-table-column>
          <el-table-column label="版本状态" width="110">
            <template #default="{ row }">
              <el-tag v-if="(row as TemplateVO).currentVersion" size="small" type="success">
                {{ bpmTemplateVersionStatusLabels[(row as TemplateVO).currentVersion!.status] }}
              </el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openPublish(row as TemplateVO)">发布新版本</el-button>
              <el-button link type="warning" size="small" @click="toggleTemplate(row as TemplateVO)">
                {{ (row as TemplateVO).status === 'ENABLED' ? '停用' : '启用' }}
              </el-button>
              <el-button
                v-if="hasPermission('bpm:template:delete')"
                link
                type="danger"
                size="small"
                @click="deleteTemplate(row as TemplateVO)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <BpmInstanceDrawer v-model="drawerVisible" :instance-id="drawerInstanceId" />

    <el-dialog v-model="createVisible" title="新增流程模板" width="480px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-form-item label="模板编码" prop="code">
          <el-input v-model="createForm.code" placeholder="唯一编码，如 PURCHASE_APPLY" maxlength="64" />
        </el-form-item>
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="createForm.name" placeholder="如 采购申请审批" maxlength="128" />
        </el-form-item>
        <el-form-item label="业务类型" prop="bizType">
          <el-select v-model="createForm.bizType" placeholder="请选择业务类型" style="width: 100%">
            <el-option
              v-for="(label, value) in bpmBizTypeLabels"
              :key="value"
              :label="label"
              :value="value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSaving" @click="submitCreateTemplate">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="delegateVisible" title="新建代理" width="520px">
      <el-form label-width="90px">
        <el-form-item label="代理人">
          <el-select
            v-model="delegateForm.delegateId"
            filterable
            remote
            :remote-method="searchCandidates"
            :loading="candidateLoading"
            placeholder="输入姓名或账号搜索"
            style="width: 100%"
          >
            <el-option
              v-for="user in candidateOptions"
              :key="user.id"
              :label="`${user.name}（${user.username}）`"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="适用范围">
          <el-select v-model="delegateForm.bizType" clearable placeholder="全部业务" style="width: 100%">
            <el-option v-for="(label, value) in bpmBizTypeLabels" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="delegateForm.startTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择开始时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="delegateForm.endTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择结束时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="代理原因">
          <el-input v-model="delegateForm.reason" type="textarea" :rows="2" placeholder="如：出差、休假" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="delegateVisible = false">取消</el-button>
        <el-button type="primary" :loading="delegateSaving" @click="submitDelegate">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectVisible" title="驳回任务" width="480px">
      <el-form label-width="90px">
        <el-form-item label="驳回至">
          <el-radio-group v-model="rejectForm.target">
            <el-radio value="INITIATOR">发起人</el-radio>
            <el-radio value="PREVIOUS">上一级</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="驳回原因">
          <el-input v-model="rejectForm.reason" type="textarea" :rows="3" placeholder="请填写驳回原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="warning" @click="submitReject">确定驳回</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="publishVisible" title="发布流程新版本" width="1100px" top="6vh">
      <div class="publish-head">
        <el-radio-group v-model="publishMode" size="small" @change="onPublishModeChange">
          <el-radio-button value="visual">可视化设计</el-radio-button>
          <el-radio-button value="json">JSON 编辑</el-radio-button>
        </el-radio-group>
        <span class="hint">可视化模式支持线性审批链；含条件分支的模板请使用 JSON 编辑</span>
      </div>

      <div v-show="publishMode === 'visual'" class="publish-designer">
        <FlowDesigner
          ref="designerRef"
          :schema-json="publishSchema"
          @update:schema-json="publishSchema = $event"
        />
      </div>

      <el-form v-show="publishMode === 'json'" label-width="90px">
        <el-form-item label="流程定义">
          <el-input
            v-model="publishSchema"
            type="textarea"
            :rows="16"
            placeholder='{"nodes":[{"nodeKey":"start","seqNo":1,"nodeType":"START"},{"nodeKey":"approve","seqNo":2,"nodeType":"APPROVAL","approveMode":"ANY","assignees":[{"type":"DEPT_LEADER"}],"dueHours":24},{"nodeKey":"end","seqNo":3,"nodeType":"END"}]}'
          />
        </el-form-item>
      </el-form>

      <el-form label-width="90px">
        <el-form-item label="生效时间">
          <el-input v-model="publishEffectiveTime" placeholder="留空立即生效，格式 YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPublish">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
}
.overdue {
  display: flex;
  gap: 6px;
  align-items: center;
  font-size: 13px;
  color: #4b5563;
}
.hint {
  font-size: 12px;
  color: #6b7280;
}
.pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
.publish-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.publish-designer {
  margin-bottom: 12px;
}
</style>
