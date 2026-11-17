<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ElButton,
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
  ElTag,
  ElTimeline,
  ElTimelineItem
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { formatDateTime } from '@ceams/utils'
import { ceamsApi } from '@/services/http'
import type { Id, RepairVO, RepairStatus, RepairCategory, RepairCreateRequest, UserVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { hasPermission } from '@/composables/usePermission'
import { apiErrorMessage } from '@/utils/error'
import { repairCategoryLabels, repairStatusLabels, toOptions } from '@/utils/labels'

defineOptions({ name: 'RepairPanel' })

const canManage = computed(() => hasPermission('admin:repair:manage'))
const statusOptions = toOptions(repairStatusLabels)
const categoryOptions = toOptions(repairCategoryLabels)

function assigneeName(id: Id | null): string {
  if (!id) return '-'
  return users.value.find((item) => item.id === id)?.name ?? id
}

function repairTagType(status: RepairStatus): 'success' | 'warning' | 'info' | 'danger' {
  switch (status) {
    case 'ACCEPTED':
      return 'success'
    case 'PROCESSING':
    case 'ASSIGNED':
      return 'warning'
    case 'REJECTED':
      return 'danger'
    default:
      return 'info'
  }
}

// ---------- 工单列表 ----------
const query = reactive<{ status: RepairStatus | '' }>({ status: '' })
const { page, size, total, resetPage } = usePage()
const list = ref<RepairVO[]>([])
const loading = ref(false)

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.admin.repairPage({
      page: page.value,
      size: size.value,
      status: query.status || undefined
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载报修工单失败'))
  } finally {
    loading.value = false
  }
}

function onSearch(): void {
  resetPage()
  void load()
}

// ---------- 派单 ----------
const users = ref<UserVO[]>([])
const assignVisible = ref(false)
const assignFormRef = ref<FormInstance>()
const assignRepair = ref<RepairVO | null>(null)
const assignForm = reactive({ assigneeId: '', remark: '' })
const assignRules: FormRules<typeof assignForm> = {
  assigneeId: [{ required: true, message: '请选择维修人', trigger: 'change' }]
}

async function loadUsers(): Promise<void> {
  try {
    const res = await ceamsApi.iam.userPage({ page: 1, size: 200 })
    users.value = res.records
  } catch {
    // 维修人下拉加载失败不阻断工单列表
  }
}

function openAssign(row: RepairVO): void {
  assignRepair.value = row
  Object.assign(assignForm, { assigneeId: row.assigneeId ?? '', remark: '' })
  assignVisible.value = true
}

async function submitAssign(): Promise<void> {
  if (!assignRepair.value || !assignFormRef.value) return
  const valid = await assignFormRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    await ceamsApi.admin.repairAssign(assignRepair.value.id, {
      assigneeId: assignForm.assigneeId,
      remark: assignForm.remark.trim() || undefined
    })
    ElMessage.success('派单成功')
    assignVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '派单失败'))
  }
}

// ---------- 处理 / 完工 ----------
const remarkVisible = ref(false)
const remarkMode = ref<'process' | 'finish'>('process')
const remarkRepair = ref<RepairVO | null>(null)
const remarkText = ref('')

function openRemark(row: RepairVO, mode: 'process' | 'finish'): void {
  remarkRepair.value = row
  remarkMode.value = mode
  remarkText.value = ''
  remarkVisible.value = true
}

async function submitRemark(): Promise<void> {
  if (!remarkRepair.value) return
  try {
    const body = { remark: remarkText.value.trim() || undefined }
    if (remarkMode.value === 'process') {
      await ceamsApi.admin.repairProcess(remarkRepair.value.id, body)
      ElMessage.success('已提交处理进度')
    } else {
      await ceamsApi.admin.repairFinish(remarkRepair.value.id, body)
      ElMessage.success('工单已完工')
    }
    remarkVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '提交失败'))
  }
}

// ---------- 验收 ----------
async function onAccept(row: RepairVO): Promise<void> {
  const confirmed = await ElMessageBox.confirm(
    `确认验收工单「${row.repairNo}」？`,
    '验收确认',
    { type: 'warning' }
  ).catch(() => false)
  if (!confirmed) return
  try {
    await ceamsApi.admin.repairAccept(row.id)
    ElMessage.success('工单已验收')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '验收失败'))
  }
}

// ---------- 新建报修 ----------
const createVisible = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({
  location: '',
  category: 'ELECTRIC' as RepairCategory,
  description: '',
  imagesText: ''
})
const createRules: FormRules<typeof createForm> = {
  location: [{ required: true, message: '请输入报修位置', trigger: 'blur' }],
  category: [{ required: true, message: '请选择报修分类', trigger: 'change' }],
  description: [{ required: true, message: '请描述报修问题', trigger: 'blur' }]
}

function openCreate(): void {
  Object.assign(createForm, {
    location: '',
    category: 'ELECTRIC',
    description: '',
    imagesText: ''
  })
  createVisible.value = true
}

async function submitCreate(): Promise<void> {
  if (!createFormRef.value) return
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  const images = createForm.imagesText
    .split(/[,，\s]+/)
    .map((item) => item.trim())
    .filter((item) => item.length > 0)
  const payload: RepairCreateRequest = {
    location: createForm.location.trim(),
    category: createForm.category,
    description: createForm.description.trim(),
    images: images.length > 0 ? images : undefined
  }
  try {
    await ceamsApi.admin.repairCreate(payload)
    ElMessage.success('报修已提交，请等待行政派单')
    createVisible.value = false
    await Promise.all([load(), loadMyRepairs()])
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '提交报修失败'))
  }
}

// ---------- 我的报修 ----------
const myRepairs = ref<RepairVO[]>([])
const myLoading = ref(false)

async function loadMyRepairs(): Promise<void> {
  myLoading.value = true
  try {
    myRepairs.value = await ceamsApi.admin.myRepairs()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载我的报修失败'))
  } finally {
    myLoading.value = false
  }
}

onMounted(() => {
  void loadUsers()
  void load()
  void loadMyRepairs()
})
</script>

<template>
  <div>
    <div class="toolbar">
      <el-select v-model="query.status" placeholder="工单状态" clearable style="width: 150px">
        <el-option
          v-for="opt in statusOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="openCreate">新建报修</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="detail">
            <div class="detail-item">
              <span class="detail-label">报修描述</span>
              <span>{{ (row as RepairVO).description }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">现场图片</span>
              <span v-if="(row as RepairVO).images.length === 0">-</span>
              <el-tag
                v-for="name in (row as RepairVO).images"
                :key="name"
                size="small"
                class="image-tag"
              >
                {{ name }}
              </el-tag>
            </div>
            <div class="detail-item">
              <span class="detail-label">处理备注</span>
              <span>{{ (row as RepairVO).remark ?? '-' }}</span>
            </div>
            <div class="detail-item timeline-item">
              <span class="detail-label">时间节点</span>
              <el-timeline>
                <el-timeline-item
                  timestamp="提交"
                  :color="(row as RepairVO).status === 'SUBMITTED' ? '#2563eb' : '#9ca3af'"
                >
                  --
                </el-timeline-item>
                <el-timeline-item
                  :timestamp="`派单 ${formatDateTime((row as RepairVO).assignedAt)}`"
                  :color="(row as RepairVO).assignedAt ? '#2563eb' : '#e5e7eb'"
                />
                <el-timeline-item
                  :timestamp="`处理 ${formatDateTime((row as RepairVO).processedAt)}`"
                  :color="(row as RepairVO).processedAt ? '#2563eb' : '#e5e7eb'"
                />
                <el-timeline-item
                  :timestamp="`完工 ${formatDateTime((row as RepairVO).finishedAt)}`"
                  :color="(row as RepairVO).finishedAt ? '#16a34a' : '#e5e7eb'"
                />
                <el-timeline-item
                  :timestamp="`验收 ${formatDateTime((row as RepairVO).acceptedAt)}`"
                  :color="(row as RepairVO).acceptedAt ? '#16a34a' : '#e5e7eb'"
                />
              </el-timeline>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="repairNo" label="工单号" width="170" />
      <el-table-column prop="location" label="报修位置" min-width="150" />
      <el-table-column label="报修分类" width="120">
        <template #default="{ row }">
          {{ repairCategoryLabels[(row as RepairVO).category] }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="repairTagType((row as RepairVO).status)">
            {{ repairStatusLabels[(row as RepairVO).status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="维修人" width="120">
        <template #default="{ row }">{{ assigneeName((row as RepairVO).assigneeId) }}</template>
      </el-table-column>
      <el-table-column label="派单时间" width="170">
        <template #default="{ row }">{{ formatDateTime((row as RepairVO).assignedAt) }}</template>
      </el-table-column>
      <el-table-column label="完工时间" width="170">
        <template #default="{ row }">{{ formatDateTime((row as RepairVO).finishedAt) }}</template>
      </el-table-column>
      <el-table-column label="验收时间" width="170">
        <template #default="{ row }">{{ formatDateTime((row as RepairVO).acceptedAt) }}</template>
      </el-table-column>
      <el-table-column v-if="canManage" label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="(row as RepairVO).status === 'SUBMITTED'"
            link
            type="primary"
            size="small"
            @click="openAssign(row as RepairVO)"
          >
            派单
          </el-button>
          <el-button
            v-if="(row as RepairVO).status === 'ASSIGNED'"
            link
            type="primary"
            size="small"
            @click="openRemark(row as RepairVO, 'process')"
          >
            处理
          </el-button>
          <el-button
            v-if="(row as RepairVO).status === 'PROCESSING'"
            link
            type="primary"
            size="small"
            @click="openRemark(row as RepairVO, 'finish')"
          >
            完工
          </el-button>
          <el-button
            v-if="(row as RepairVO).status === 'DONE'"
            link
            type="success"
            size="small"
            @click="onAccept(row as RepairVO)"
          >
            验收
          </el-button>
        </template>
      </el-table-column>
    </el-table>

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

    <!-- 我的报修 -->
    <div class="section">
      <div class="section-title">我的报修</div>
      <el-table v-loading="myLoading" :data="myRepairs" border size="small">
        <el-table-column prop="repairNo" label="工单号" width="170" />
        <el-table-column prop="location" label="报修位置" min-width="150" />
        <el-table-column label="报修分类" width="110">
          <template #default="{ row }">
            {{ repairCategoryLabels[(row as RepairVO).category] }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="问题描述" min-width="200" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="repairTagType((row as RepairVO).status)">
              {{ repairStatusLabels[(row as RepairVO).status] }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新建报修 -->
    <el-dialog v-model="createVisible" title="新建报修" width="560px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-form-item label="报修位置" prop="location">
          <el-input v-model="createForm.location" placeholder="如：3 号楼 2 层 201 室" />
        </el-form-item>
        <el-form-item label="报修分类" prop="category">
          <el-select v-model="createForm.category" style="width: 100%">
            <el-option
              v-for="opt in categoryOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="问题描述" prop="description">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            placeholder="请描述故障现象"
          />
        </el-form-item>
        <el-form-item label="现场图片">
          <el-input
            v-model="createForm.imagesText"
            type="textarea"
            :rows="2"
            placeholder="图片文件名，多个用逗号分隔（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">提交报修</el-button>
      </template>
    </el-dialog>

    <!-- 派单 -->
    <el-dialog v-model="assignVisible" title="工单派单" width="480px">
      <el-form ref="assignFormRef" :model="assignForm" :rules="assignRules" label-width="90px">
        <el-form-item label="工单号">
          <span>{{ assignRepair?.repairNo }}</span>
        </el-form-item>
        <el-form-item label="维修人" prop="assigneeId">
          <el-select v-model="assignForm.assigneeId" filterable placeholder="请选择维修人" style="width: 100%">
            <el-option v-for="item in users" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="assignForm.remark" type="textarea" :rows="2" placeholder="派单备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssign">确认派单</el-button>
      </template>
    </el-dialog>

    <!-- 处理 / 完工 -->
    <el-dialog
      v-model="remarkVisible"
      :title="remarkMode === 'process' ? '提交处理进度' : '完成维修'"
      width="480px"
    >
      <el-form label-width="90px">
        <el-form-item label="工单号">
          <span>{{ remarkRepair?.repairNo }}</span>
        </el-form-item>
        <el-form-item label="处理说明">
          <el-input
            v-model="remarkText"
            type="textarea"
            :rows="3"
            placeholder="请输入处理说明（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="remarkVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRemark">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.section {
  margin-bottom: 24px;
}
.section-title {
  margin-bottom: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}
.pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
.detail {
  padding: 4px 16px;
}
.detail-item {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 13px;
  color: #374151;
}
.detail-label {
  flex: 0 0 80px;
  color: #6b7280;
}
.timeline-item {
  align-items: flex-start;
}
.image-tag {
  margin-right: 6px;
}
</style>
