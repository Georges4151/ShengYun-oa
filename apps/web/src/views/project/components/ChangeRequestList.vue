<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  ElButton,
  ElDescriptions,
  ElDescriptionsItem,
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
import { ceamsApi } from '@/services/http'
import type { ApprovalStatus, ChangeRequestVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { approvalStatusLabels, toOptions } from '@/utils/labels'
import BpmInstanceDrawer from '@/views/bpm/components/BpmInstanceDrawer.vue'

defineOptions({ name: 'ChangeRequestList' })

const statusOptions = toOptions(approvalStatusLabels)

const query = reactive<{ projectId?: string; status?: ApprovalStatus }>({})
const { page, size, total, resetPage } = usePage()
const list = ref<ChangeRequestVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<ChangeRequestVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  projectId: '',
  requestType: '',
  description: '',
  estimatedCost: ''
})

const rules: FormRules<typeof form> = {
  projectId: [{ required: true, message: '请输入项目 ID', trigger: 'blur' }],
  requestType: [{ required: true, message: '请输入变更类型', trigger: 'blur' }]
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.project.changePage({
      page: page.value,
      size: size.value,
      projectId: query.projectId,
      status: query.status
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载变更请求失败'))
  } finally {
    loading.value = false
  }
}

function onSearch(): void {
  resetPage()
  void load()
}

function openCreate(): void {
  editingId.value = null
  Object.assign(form, { projectId: '', requestType: '', description: '', estimatedCost: '' })
  dialogVisible.value = true
}

function openEdit(row: ChangeRequestVO): void {
  editingId.value = row.id
  form.projectId = row.projectId
  form.requestType = row.requestType
  form.description = row.description ?? ''
  form.estimatedCost = row.estimatedCost ?? ''
  dialogVisible.value = true
}

function openDetail(row: ChangeRequestVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.project.changeUpdate(editingId.value, {
        requestType: form.requestType,
        description: form.description || undefined,
        estimatedCost: form.estimatedCost || undefined
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.project.changeCreate({
        projectId: form.projectId,
        requestType: form.requestType,
        description: form.description || null,
        estimatedCost: form.estimatedCost || undefined
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存失败'))
  }
}

async function onRemove(row: ChangeRequestVO): Promise<void> {
  try {
    await ElMessageBox.confirm('确定删除该变更请求吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.project.changeRemove(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '删除失败'))
  }
}

/** 提交审批：发起 BPM 项目变更流程（部门审批 → 结束）。 */
async function submitChange(row: ChangeRequestVO): Promise<void> {
  try {
    await ElMessageBox.confirm('提交后进入部门审批，确定提交吗？', '提交变更', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.project.changeSubmit(row.id)
    ElMessage.success('已提交，请到待办中心跟进审批')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '提交失败'))
  }
}

const drawerVisible = ref(false)
const drawerInstanceId = ref<string | null>(null)

function openInstance(instanceId?: string | null): void {
  if (!instanceId) return
  drawerInstanceId.value = instanceId
  drawerVisible.value = true
}

onMounted(load)
</script>

<template>
  <div>
    <div class="toolbar">
      <el-input v-model="query.projectId" placeholder="项目 ID（可选）" clearable style="width: 200px" />
      <el-select v-model="query.status" placeholder="审批状态" clearable style="width: 160px">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="openCreate">新增变更</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="projectId" label="项目 ID" width="180" />
      <el-table-column prop="requestType" label="变更类型" width="140" />
      <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
      <el-table-column prop="estimatedCost" label="预估费用" width="120" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag size="small">{{ approvalStatusLabels[(row as ChangeRequestVO).status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="requestedBy" label="申请人" width="110" />
      <el-table-column prop="approvedBy" label="审批人" width="110" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row as ChangeRequestVO)">详情</el-button>
          <el-button link type="primary" size="small" @click="openEdit(row as ChangeRequestVO)">编辑</el-button>
          <el-button
            v-if="(row as ChangeRequestVO).status === 'DRAFT' || (row as ChangeRequestVO).status === 'REJECTED'"
            link
            type="success"
            size="small"
            @click="submitChange(row as ChangeRequestVO)"
          >
            提交审批
          </el-button>
          <el-button
            v-if="(row as ChangeRequestVO).bpmInstanceId"
            link
            type="primary"
            size="small"
            @click="openInstance((row as ChangeRequestVO).bpmInstanceId)"
          >
            查看流程
          </el-button>
          <el-button link type="danger" size="small" @click="onRemove(row as ChangeRequestVO)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑变更请求' : '新增变更请求'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="项目 ID" prop="projectId">
          <el-input v-model="form.projectId" :disabled="!!editingId" placeholder="请输入项目 ID" />
        </el-form-item>
        <el-form-item label="变更类型" prop="requestType">
          <el-input v-model="form.requestType" placeholder="如：设计变更 / 工期变更" />
        </el-form-item>
        <el-form-item label="预估费用">
          <el-input v-model="form.estimatedCost" placeholder="金额（可选）" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="变更描述（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="变更请求详情" width="520px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="项目 ID">{{ detail.projectId }}</el-descriptions-item>
        <el-descriptions-item label="变更类型">{{ detail.requestType }}</el-descriptions-item>
        <el-descriptions-item label="描述">{{ detail.description ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="预估费用">{{ detail.estimatedCost ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ approvalStatusLabels[detail.status] }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ detail.requestedBy ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批人">{{ detail.approvedBy ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批时间">{{ detail.approvedAt ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime ?? '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <BpmInstanceDrawer v-model="drawerVisible" :instance-id="drawerInstanceId" />
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
