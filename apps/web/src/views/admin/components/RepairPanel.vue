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
  ElTimelineItem,
  ElUpload
} from 'element-plus'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import { formatDateTime } from '@ceams/utils'
import { ceamsApi } from '@/services/http'
import type { Id, RepairVO, RepairStatus, RepairCategory, RepairCreateRequest, UserVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { hasPermission } from '@/composables/usePermission'
import { apiErrorMessage } from '@/utils/error'
import { repairCategoryLabels, repairStatusLabels, toOptions } from '@/utils/labels'
import RepairImageThumbs from './RepairImageThumbs.vue'

defineOptions({ name: 'RepairPanel' })

/** 报修现场 / 完工图片数量上限。 */
const MAX_IMAGES = 6

/** 已上传图片：name 为文件服务文件名，url 为本地 objectURL（仅用于弹窗内预览）。 */
interface LocalImage {
  name: string
  url: string
}

/**
 * 图片选择器：关闭 el-upload 自动上传，选中即上传并保存文件服务返回的 name，
 * 同时用原始文件生成本地 objectURL 供弹窗内即时预览。
 */
function useImagePicker() {
  const images = ref<LocalImage[]>([])
  const uploading = ref(false)
  /** 代次：clear 后递增，用于丢弃弹窗关闭后才返回的上传结果。 */
  let generation = 0

  /** 释放全部 objectURL 并清空列表（打开 / 关闭弹窗时调用）。 */
  function clear(): void {
    generation += 1
    images.value.forEach((item) => URL.revokeObjectURL(item.url))
    images.value = []
  }

  async function onChange(file: UploadFile): Promise<void> {
    const raw = file.raw
    if (!raw) return
    if (!/^image\/(jpeg|png|webp)$/i.test(raw.type) && !/\.(jpe?g|png|webp)$/i.test(raw.name)) {
      ElMessage.warning('仅支持 JPG/PNG/WEBP 图片')
      return
    }
    if (images.value.length >= MAX_IMAGES) {
      ElMessage.warning(`最多上传 ${MAX_IMAGES} 张图片`)
      return
    }
    const current = generation
    uploading.value = true
    try {
      const uploaded = await ceamsApi.file.upload(raw)
      if (current !== generation) return
      images.value.push({ name: uploaded.name, url: URL.createObjectURL(raw) })
    } catch (err) {
      ElMessage.error(apiErrorMessage(err, '图片上传失败'))
    } finally {
      uploading.value = false
    }
  }

  function remove(index: number): void {
    const target = images.value[index]
    if (!target) return
    URL.revokeObjectURL(target.url)
    images.value.splice(index, 1)
  }

  /** 提交用文件名数组。 */
  function names(): string[] {
    return images.value.map((item) => item.name)
  }

  return { images, uploading, clear, onChange, remove, names }
}

const canManage = computed(() => hasPermission('admin:repair:manage'))
const statusOptions = toOptions(repairStatusLabels)
const categoryOptions = toOptions(repairCategoryLabels)

// 新建报修 / 完成维修的图片选择器（各弹窗独立，互不影响）
const {
  images: createImages,
  uploading: createUploading,
  clear: clearCreateImages,
  onChange: onCreateImageChange,
  remove: removeCreateImage,
  names: createImageNames
} = useImagePicker()
const {
  images: finishImages,
  uploading: finishUploading,
  clear: clearFinishImages,
  onChange: onFinishImageChange,
  remove: removeFinishImage,
  names: finishImageNames
} = useImagePicker()

function assigneeName(id: Id | null): string {
  if (!id) return '-'
  return users.value.find((item) => item.id === id)?.name ?? id
}

function repairTagType(status: RepairStatus): 'success' | 'warning' | 'info' | 'danger' {
  switch (status) {
    case 'DONE':
    case 'ACCEPTED':
      return 'success'
    case 'SUBMITTED':
    case 'ASSIGNED':
    case 'PROCESSING':
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
    const res = await ceamsApi.iam.userPage({ page: 1, size: 100 })
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
  clearFinishImages()
  remarkVisible.value = true
}

async function submitRemark(): Promise<void> {
  if (!remarkRepair.value) return
  if (remarkMode.value === 'finish' && finishImages.value.length === 0) {
    ElMessage.warning('请至少上传 1 张完工图片')
    return
  }
  try {
    if (remarkMode.value === 'process') {
      await ceamsApi.admin.repairProcess(remarkRepair.value.id, {
        remark: remarkText.value.trim() || undefined
      })
      ElMessage.success('已提交处理进度')
    } else {
      await ceamsApi.admin.repairFinish(remarkRepair.value.id, {
        remark: remarkText.value.trim() || undefined,
        images: finishImageNames()
      })
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
  description: ''
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
    description: ''
  })
  clearCreateImages()
  createVisible.value = true
}

async function submitCreate(): Promise<void> {
  if (!createFormRef.value) return
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (createImages.value.length === 0) {
    ElMessage.warning('请至少上传 1 张现场图片')
    return
  }
  const payload: RepairCreateRequest = {
    location: createForm.location.trim(),
    category: createForm.category,
    description: createForm.description.trim(),
    images: createImageNames()
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
  <div class="repair-panel">
    <!-- 筛选区 -->
    <div class="filter-bar">
      <el-select v-model="query.status" placeholder="工单状态" clearable style="width: 150px">
        <el-option
          v-for="opt in statusOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <div class="filter-actions">
        <el-button type="primary" @click="onSearch">查询</el-button>
      </div>
    </div>

    <!-- 工单列表卡片 -->
    <div class="data-card panel-card">
      <div class="card-header">
        <div class="card-heading">
          <span class="card-title">报修工单</span>
          <span class="card-count">共 {{ total }} 单</span>
        </div>
        <el-button type="primary" @click="openCreate">新建报修</el-button>
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
              <RepairImageThumbs :names="(row as RepairVO).images" />
            </div>
            <div class="detail-item">
              <span class="detail-label">完工图片</span>
              <RepairImageThumbs :names="(row as RepairVO).finishImages" />
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

    <!-- 我的报修卡片 -->
    <div class="data-card panel-card">
      <div class="card-header">
        <div class="card-heading">
          <span class="card-title">我的报修</span>
          <span class="card-count">共 {{ myRepairs.length }} 条</span>
        </div>
      </div>
      <el-table v-loading="myLoading" :data="myRepairs" border size="small">
        <el-table-column prop="repairNo" label="工单号" width="170" />
        <el-table-column prop="location" label="报修位置" min-width="150" />
        <el-table-column label="报修分类" width="110">
          <template #default="{ row }">
            {{ repairCategoryLabels[(row as RepairVO).category] }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="问题描述" min-width="200" />
        <el-table-column label="现场图片" width="140">
          <template #default="{ row }">
            <RepairImageThumbs :names="(row as RepairVO).images" :size="48" />
          </template>
        </el-table-column>
        <el-table-column label="完工图片" width="140">
          <template #default="{ row }">
            <RepairImageThumbs :names="(row as RepairVO).finishImages" :size="48" />
          </template>
        </el-table-column>
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
    <el-dialog
      v-model="createVisible"
      class="admin-dialog"
      title="新建报修"
      width="560px"
      @closed="clearCreateImages"
    >
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
          <div class="image-field">
            <div v-for="(img, index) in createImages" :key="img.name" class="image-item">
              <img :src="img.url" :alt="img.name" :title="img.name" class="image-item__thumb" />
              <span class="image-item__remove" title="删除" @click="removeCreateImage(index)">×</span>
            </div>
            <el-upload
              v-if="createImages.length < MAX_IMAGES"
              class="image-uploader"
              :auto-upload="false"
              :show-file-list="false"
              :disabled="createUploading"
              accept="image/jpeg,image/png,image/webp"
              :on-change="onCreateImageChange"
            >
              <div class="image-add">{{ createUploading ? '上传中' : '+' }}</div>
            </el-upload>
          </div>
          <div class="image-tip">至少 1 张，最多 {{ MAX_IMAGES }} 张，支持 JPG/PNG/WEBP</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">提交报修</el-button>
      </template>
    </el-dialog>

    <!-- 派单 -->
    <el-dialog v-model="assignVisible" class="admin-dialog" title="工单派单" width="480px">
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
      class="admin-dialog"
      :title="remarkMode === 'process' ? '提交处理进度' : '完成维修'"
      width="560px"
      @closed="clearFinishImages"
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
        <el-form-item v-if="remarkMode === 'finish'" label="完工图片">
          <div class="image-field">
            <div v-for="(img, index) in finishImages" :key="img.name" class="image-item">
              <img :src="img.url" :alt="img.name" :title="img.name" class="image-item__thumb" />
              <span class="image-item__remove" title="删除" @click="removeFinishImage(index)">×</span>
            </div>
            <el-upload
              v-if="finishImages.length < MAX_IMAGES"
              class="image-uploader"
              :auto-upload="false"
              :show-file-list="false"
              :disabled="finishUploading"
              accept="image/jpeg,image/png,image/webp"
              :on-change="onFinishImageChange"
            >
              <div class="image-add">{{ finishUploading ? '上传中' : '+' }}</div>
            </el-upload>
          </div>
          <div class="image-tip">至少 1 张，最多 {{ MAX_IMAGES }} 张，支持 JPG/PNG/WEBP</div>
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
/* 模块容器：各卡片之间保持 16px 间距 */
.repair-panel {
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

.detail {
  padding: var(--ceams-space-1) var(--ceams-space-4);
}

.detail-item {
  display: flex;
  gap: var(--ceams-space-3);
  margin-bottom: var(--ceams-space-2);
  font-size: var(--ceams-font-size-sm);
  color: var(--ceams-text-primary);
}

.detail-label {
  flex: 0 0 80px;
  color: var(--ceams-text-secondary);
}

.timeline-item {
  align-items: flex-start;
}

.image-field {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--ceams-space-2);
}

.image-item {
  position: relative;
  width: 72px;
  height: 72px;
  overflow: hidden;
  border: 1px solid var(--ceams-border-base);
  border-radius: var(--ceams-radius-md);
  background: var(--ceams-bg-elevated);
}

.image-item__thumb {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-item__remove {
  position: absolute;
  top: 0;
  right: 0;
  width: 18px;
  height: 18px;
  color: var(--ceams-text-inverse);
  font-size: var(--ceams-font-size-base);
  line-height: 18px;
  text-align: center;
  background: rgba(17, 24, 39, 0.6);
  border-bottom-left-radius: var(--ceams-radius-md);
  cursor: pointer;
}

.image-uploader :deep(.el-upload) {
  width: 72px;
  height: 72px;
  overflow: hidden;
  border: 1px dashed var(--ceams-border-strong);
  border-radius: var(--ceams-radius-md);
  background: var(--ceams-bg-elevated);
}

.image-add {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-xl);
  line-height: 1;
}

.image-tip {
  width: 100%;
  margin-top: var(--ceams-space-1);
  color: var(--ceams-text-placeholder);
  font-size: var(--ceams-font-size-xs);
  line-height: 1.5;
}

/* 弹窗内容 20px 24px */
:global(.admin-dialog .el-dialog__body) {
  padding: 20px 24px;
}
</style>
