<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
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
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import QRCode from 'qrcode'
import { formatDate, formatDateTime } from '@ceams/utils'
import { ceamsApi } from '@/services/http'
import type {
  VisitorVO,
  VisitorPageQuery,
  VisitorQrVO,
  VisitorStatus,
  UserVO
} from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { hasPermission } from '@/composables/usePermission'
import { useAuthStore } from '@/stores/auth'
import { apiErrorMessage } from '@/utils/error'
import { toOptions, visitorStatusLabels } from '@/utils/labels'

defineOptions({ name: 'VisitorPanel' })

const auth = useAuthStore()
const canManage = computed(() => hasPermission('admin:visitor:manage'))
const statusOptions = toOptions(visitorStatusLabels)

function visitorTagType(status: VisitorStatus): 'success' | 'warning' | 'info' | 'danger' {
  switch (status) {
    case 'CHECKED_IN':
      return 'success'
    case 'PENDING':
      return 'warning'
    case 'CHECKED_OUT':
    case 'CANCELLED':
      return 'info'
    default:
      return 'info'
  }
}

// ---------- 访客列表 ----------
const users = ref<UserVO[]>([])

async function loadUsers(): Promise<void> {
  try {
    const res = await ceamsApi.iam.userPage({ page: 1, size: 200 })
    users.value = res.records
  } catch {
    // 被访人下拉加载失败不阻断访客列表
  }
}

const query = reactive<{ date: string; status: VisitorStatus | '' }>({
  date: formatDate(new Date(), ''),
  status: ''
})
const { page, size, total, resetPage } = usePage()
const list = ref<VisitorVO[]>([])
const loading = ref(false)

async function load(): Promise<void> {
  loading.value = true
  try {
    const params: VisitorPageQuery = {
      page: page.value,
      size: size.value,
      date: query.date || undefined,
      status: query.status || undefined
    }
    const res = await ceamsApi.admin.visitorPage(params)
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载访客列表失败'))
  } finally {
    loading.value = false
  }
}

function onSearch(): void {
  resetPage()
  void load()
}

// ---------- 访客登记 ----------
const createVisible = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({
  visitorName: '',
  phone: '',
  company: '',
  hostUserId: auth.user?.id ?? '',
  visitDate: formatDate(new Date(), ''),
  purpose: ''
})
const createRules: FormRules<typeof createForm> = {
  visitorName: [{ required: true, message: '请输入访客姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入 11 位手机号', trigger: 'blur' }
  ],
  hostUserId: [{ required: true, message: '请选择被访人', trigger: 'change' }],
  visitDate: [{ required: true, message: '请选择来访日期', trigger: 'change' }]
}

function openCreate(): void {
  Object.assign(createForm, {
    visitorName: '',
    phone: '',
    company: '',
    hostUserId: auth.user?.id ?? '',
    visitDate: formatDate(new Date(), ''),
    purpose: ''
  })
  createVisible.value = true
}

async function submitCreate(): Promise<void> {
  if (!createFormRef.value) return
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    const res = await ceamsApi.admin.visitorCreate({
      visitorName: createForm.visitorName.trim(),
      phone: createForm.phone.trim(),
      company: createForm.company.trim() || undefined,
      hostUserId: createForm.hostUserId || undefined,
      visitDate: createForm.visitDate,
      purpose: createForm.purpose.trim() || undefined
    })
    ElMessage.success('访客登记成功')
    createVisible.value = false
    await load()
    await showQr(res)
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '访客登记失败'))
  }
}

// ---------- 二维码 ----------
const qrVisible = ref(false)
const qrInfo = ref<VisitorQrVO | null>(null)
const qrImage = ref('')

async function showQr(info: VisitorQrVO): Promise<void> {
  qrInfo.value = info
  qrVisible.value = true
  try {
    // 二维码内容仅包含一次性核销令牌，不写入手机号等隐私信息
    qrImage.value = await QRCode.toDataURL(info.token, { width: 260, margin: 1 })
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '生成二维码失败'))
  }
}

async function refreshQr(row: VisitorVO): Promise<void> {
  try {
    const res = await ceamsApi.admin.visitorQr(row.id)
    await showQr(res)
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '获取二维码失败'))
  }
}

function onQrClosed(): void {
  qrImage.value = ''
  qrInfo.value = null
}

// ---------- 核销 / 离场 / 取消 ----------
const checkinVisible = ref(false)
const checkinToken = ref('')

function openCheckin(): void {
  checkinToken.value = ''
  checkinVisible.value = true
}

async function submitCheckin(): Promise<void> {
  const token = checkinToken.value.trim()
  if (!token) {
    ElMessage.warning('请输入访客核销令牌')
    return
  }
  try {
    await ceamsApi.admin.visitorCheckin({ token })
    ElMessage.success('访客核销成功')
    checkinVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '访客核销失败'))
  }
}

async function onCheckout(row: VisitorVO): Promise<void> {
  try {
    await ceamsApi.admin.visitorCheckout(row.id)
    ElMessage.success('访客已离场')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '访客离场登记失败'))
  }
}

async function onCancel(row: VisitorVO): Promise<void> {
  const confirmed = await ElMessageBox.confirm(
    `确认取消访客「${row.visitorName}」的来访？`,
    '取消确认',
    { type: 'warning' }
  ).catch(() => false)
  if (!confirmed) return
  try {
    await ceamsApi.admin.visitorCancel(row.id)
    ElMessage.success('访客来访已取消')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '取消失败'))
  }
}

onMounted(() => {
  void loadUsers()
  void load()
})
</script>

<template>
  <div class="visitor-panel">
    <!-- 筛选区：白色圆角卡片，操作按钮靠右 -->
    <div class="filter-bar">
      <el-date-picker
        v-model="query.date"
        type="date"
        value-format="YYYY-MM-DD"
        placeholder="来访日期"
        style="width: 160px"
        @change="onSearch"
      />
      <el-select v-model="query.status" placeholder="访客状态" clearable style="width: 140px">
        <el-option
          v-for="opt in statusOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <div class="filter-actions">
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button v-if="canManage" @click="openCheckin">扫码核销</el-button>
      </div>
    </div>

    <!-- 访客记录卡片 -->
    <div class="data-card panel-card">
      <div class="card-header">
        <div class="card-heading">
          <span class="card-title">访客记录</span>
          <span class="card-count">共 {{ total }} 条</span>
        </div>
        <el-button v-if="canManage" type="primary" @click="openCreate">访客登记</el-button>
      </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="visitorName" label="访客姓名" width="120" />
      <el-table-column prop="company" label="单位" min-width="140">
        <template #default="{ row }">{{ (row as VisitorVO).company ?? '-' }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="140">
        <template #default="{ row }">{{ (row as VisitorVO).phone ?? '-' }}</template>
      </el-table-column>
      <el-table-column prop="visitDate" label="来访日期" width="120" />
      <el-table-column prop="purpose" label="来访事由" min-width="150">
        <template #default="{ row }">{{ (row as VisitorVO).purpose ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="visitorTagType((row as VisitorVO).status)">
            {{ visitorStatusLabels[(row as VisitorVO).status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="入场时间" width="170">
        <template #default="{ row }">{{ formatDateTime((row as VisitorVO).checkinTime) }}</template>
      </el-table-column>
      <el-table-column label="离场时间" width="170">
        <template #default="{ row }">{{ formatDateTime((row as VisitorVO).checkoutTime) }}</template>
      </el-table-column>
      <el-table-column v-if="canManage" label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="(row as VisitorVO).status === 'PENDING'"
            link
            type="primary"
            size="small"
            @click="refreshQr(row as VisitorVO)"
          >
            二维码
          </el-button>
          <el-button
            v-if="(row as VisitorVO).status === 'CHECKED_IN'"
            link
            type="primary"
            size="small"
            @click="onCheckout(row as VisitorVO)"
          >
            离场
          </el-button>
          <el-button
            v-if="(row as VisitorVO).status === 'PENDING'"
            link
            type="danger"
            size="small"
            @click="onCancel(row as VisitorVO)"
          >
            取消
          </el-button>
        </template>
      </el-table-column>
    </el-table>

      <!-- 分页右对齐，距表 16px -->
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="onSearch"
        @current-change="load"
      />
    </div>

    <!-- 访客登记 -->
    <el-dialog v-model="createVisible" class="admin-dialog" title="访客登记" width="520px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-form-item label="访客姓名" prop="visitorName">
          <el-input v-model="createForm.visitorName" placeholder="请输入访客姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="createForm.phone" placeholder="请输入 11 位手机号" />
        </el-form-item>
        <el-form-item label="来访单位">
          <el-input v-model="createForm.company" placeholder="来访单位（可选）" />
        </el-form-item>
        <el-form-item label="被访人" prop="hostUserId">
          <el-select
            v-model="createForm.hostUserId"
            filterable
            placeholder="请选择被访人"
            style="width: 100%"
          >
            <el-option
              v-for="item in users"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="来访日期" prop="visitDate">
          <el-date-picker
            v-model="createForm.visitDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="来访事由">
          <el-input
            v-model="createForm.purpose"
            type="textarea"
            :rows="2"
            placeholder="来访事由（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">登记并生成二维码</el-button>
      </template>
    </el-dialog>

    <!-- 访客二维码 -->
    <el-dialog v-model="qrVisible" class="admin-dialog" title="访客通行二维码" width="400px" @closed="onQrClosed">
      <div class="qr-wrap">
        <div v-if="qrInfo" class="qr-title">{{ qrInfo.visitorName }}</div>
        <img v-if="qrImage" :src="qrImage" alt="访客通行二维码" class="qr-image" />
        <div v-if="qrInfo" class="qr-token">核销令牌：{{ qrInfo.token }}</div>
        <div v-if="qrInfo" class="qr-tip">
          有效期至 {{ formatDateTime(qrInfo.expireAt) }}，请访客在入口出示二维码核销
        </div>
      </div>
    </el-dialog>

    <!-- 扫码核销（手动输入令牌） -->
    <el-dialog v-model="checkinVisible" class="admin-dialog" title="扫码核销" width="460px">
      <el-form label-width="90px">
        <el-form-item label="核销令牌">
          <el-input
            v-model="checkinToken"
            type="textarea"
            :rows="3"
            placeholder="请输入或粘贴访客二维码中的核销令牌"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="checkinVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCheckin">确认核销</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 模块容器：筛选区与列表卡片保持 16px 间距 */
.visitor-panel {
  display: flex;
  flex-direction: column;
  gap: var(--ceams-space-4);
}

/* 筛选卡片：16px 20px 内边距，控件 12px 间距 */
.filter-bar {
  padding: 16px 20px;
  margin-bottom: 0;
  gap: var(--ceams-space-3);
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: var(--ceams-space-3);
  margin-left: auto;
}

/* 列表卡片：20px 内边距 */
.panel-card {
  padding: var(--ceams-space-5);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ceams-space-3);
  margin-bottom: var(--ceams-space-4);
}

.card-heading {
  display: flex;
  align-items: baseline;
  gap: var(--ceams-space-2);
  min-width: 0;
}

.card-title {
  font-size: var(--ceams-font-size-md);
  font-weight: 600;
  color: var(--ceams-text-primary);
}

.card-count {
  font-size: var(--ceams-font-size-sm);
  color: var(--ceams-text-secondary);
}

/* 分页右对齐，距表 16px */
.pagination {
  margin-top: var(--ceams-space-4);
  justify-content: flex-end;
}

/* 空态居中灰色、40px 留白 */
.panel-card :deep(.el-table__empty-text) {
  color: var(--ceams-text-placeholder);
}

.panel-card :deep(.el-table__empty-block) {
  padding: var(--ceams-space-10) 0;
  min-height: 0;
}

.qr-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--ceams-space-3);
}

.qr-title {
  font-size: var(--ceams-font-size-lg);
  font-weight: 600;
  color: var(--ceams-text-primary);
}

.qr-image {
  width: 260px;
  height: 260px;
}

.qr-tip {
  font-size: var(--ceams-font-size-xs);
  color: var(--ceams-text-secondary);
  text-align: center;
}

.qr-token {
  width: 100%;
  padding: 6px 8px;
  font-size: var(--ceams-font-size-xs);
  color: var(--ceams-text-primary);
  word-break: break-all;
  background: var(--ceams-bg-elevated);
  border-radius: var(--ceams-radius-sm);
}

/* 弹窗内容 20px 24px */
:global(.admin-dialog .el-dialog__body) {
  padding: 20px 24px;
}
</style>
