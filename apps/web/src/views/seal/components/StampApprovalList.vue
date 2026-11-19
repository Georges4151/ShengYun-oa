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
import type { SealApprovalStatus, StampApprovalVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { sealApprovalStatusLabels, toOptions } from '@/utils/labels'

defineOptions({ name: 'StampApprovalList' })

const statusOptions = toOptions(sealApprovalStatusLabels)

const query = reactive<{
  stampId?: string
  sealId?: string
  status?: SealApprovalStatus
}>({})
const { page, size, total, resetPage } = usePage()
const list = ref<StampApprovalVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<StampApprovalVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  stampId: '',
  sealPermission: '',
  stampPositions: '',
  originalContractPath: ''
})

const rules: FormRules<typeof form> = {
  stampId: [{ required: true, message: '请输入用印申请 ID', trigger: 'blur' }]
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.seal.approvalPage({
      page: page.value,
      size: size.value,
      stampId: query.stampId,
      sealId: query.sealId,
      status: query.status
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载用印审批失败'))
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
  Object.assign(form, { stampId: '', sealPermission: '', stampPositions: '', originalContractPath: '' })
  dialogVisible.value = true
}

function openEdit(row: StampApprovalVO): void {
  editingId.value = row.id
  form.stampId = row.stampId
  form.sealPermission = row.sealPermission ?? ''
  form.stampPositions = row.stampPositions ?? ''
  form.originalContractPath = row.originalContractPath ?? ''
  dialogVisible.value = true
}

function openDetail(row: StampApprovalVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.seal.approvalUpdate(editingId.value, {
        sealPermission: form.sealPermission || null,
        stampPositions: form.stampPositions || null,
        originalContractPath: form.originalContractPath || null
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.seal.approvalCreate({
        stampId: form.stampId,
        sealPermission: form.sealPermission || null,
        stampPositions: form.stampPositions || null,
        originalContractPath: form.originalContractPath || null
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存失败'))
  }
}

async function onRemove(row: StampApprovalVO): Promise<void> {
  try {
    await ElMessageBox.confirm('确定删除该用印审批吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.seal.approvalRemove(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '删除失败'))
  }
}

async function onApprove(row: StampApprovalVO): Promise<void> {
  try {
    const { value } = await ElMessageBox.prompt('请输入审批意见', '审批通过', {
      confirmButtonText: '通过',
      cancelButtonText: '取消',
      inputType: 'textarea'
    })
    await ceamsApi.seal.approvalApprove(row.id, { approvalComment: value || null })
    ElMessage.success('审批通过')
    await load()
  } catch (err) {
    if (err === 'cancel' || err === 'close') return
    ElMessage.error(apiErrorMessage(err, '审批操作失败'))
  }
}

async function onReject(row: StampApprovalVO): Promise<void> {
  let reason = ''
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因', '审批驳回', {
      confirmButtonText: '驳回',
      cancelButtonText: '取消',
      inputType: 'textarea'
    })
    reason = value ?? ''
  } catch {
    return
  }
  if (!reason.trim()) {
    ElMessage.warning('驳回原因不能为空')
    return
  }
  try {
    await ceamsApi.seal.approvalReject(row.id, { rejectReason: reason })
    ElMessage.success('已驳回')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '驳回失败'))
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="toolbar">
      <el-input v-model="query.stampId" placeholder="用印申请 ID" clearable style="width: 180px" />
      <el-input v-model="query.sealId" placeholder="印章 ID" clearable style="width: 180px" />
      <el-select v-model="query.status" placeholder="审批状态" clearable style="width: 140px">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="openCreate">新增审批</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="stampId" label="用印申请 ID" width="170" show-overflow-tooltip />
      <el-table-column prop="sealName" label="印章名称" width="130" />
      <el-table-column prop="sealCode" label="印章编码" width="130" />
      <el-table-column label="审批状态" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ sealApprovalStatusLabels[(row as StampApprovalVO).status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="applicantName" label="申请人" width="100" />
      <el-table-column prop="approverName" label="审批人" width="100" />
      <el-table-column prop="sealPermission" label="用印权限" min-width="130" show-overflow-tooltip />
      <el-table-column prop="applyTime" label="申请时间" width="180" />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row as StampApprovalVO)">详情</el-button>
          <el-button link type="primary" size="small" @click="openEdit(row as StampApprovalVO)">编辑</el-button>
          <el-button
            v-if="(row as StampApprovalVO).status === 'PENDING'"
            link
            type="success"
            size="small"
            @click="onApprove(row as StampApprovalVO)"
          >
            通过
          </el-button>
          <el-button
            v-if="(row as StampApprovalVO).status === 'PENDING'"
            link
            type="warning"
            size="small"
            @click="onReject(row as StampApprovalVO)"
          >
            驳回
          </el-button>
          <el-button link type="danger" size="small" @click="onRemove(row as StampApprovalVO)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑用印审批' : '新增用印审批'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="用印申请 ID" prop="stampId">
          <el-input v-model="form.stampId" :disabled="!!editingId" placeholder="请输入用印申请 ID" />
        </el-form-item>
        <el-form-item label="用印权限">
          <el-input v-model="form.sealPermission" placeholder="用印权限（可选）" />
        </el-form-item>
        <el-form-item label="盖章位置">
          <el-input v-model="form.stampPositions" placeholder="盖章位置（可选）" />
        </el-form-item>
        <el-form-item label="原始合同路径">
          <el-input v-model="form.originalContractPath" placeholder="原始合同路径（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="用印审批详情" width="620px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="用印申请 ID">{{ detail.stampId }}</el-descriptions-item>
        <el-descriptions-item label="审批状态">{{ sealApprovalStatusLabels[detail.status] }}</el-descriptions-item>
        <el-descriptions-item label="印章名称">{{ detail.sealName ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="印章编码">{{ detail.sealCode ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ detail.applicantName ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批人">{{ detail.approverName ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="项目名称">{{ detail.projectName ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="合同名称">{{ detail.contractName ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="用印权限">{{ detail.sealPermission ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="盖章位置">{{ detail.stampPositions ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="驳回原因">{{ detail.rejectReason ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ detail.applyTime ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批时间">{{ detail.approvalTime ?? '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
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
