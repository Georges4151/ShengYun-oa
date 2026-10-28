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
import type { ApplicationStatus, ElectronicSealApplicationVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { applicationStatusLabels, toOptions } from '@/utils/labels'

defineOptions({ name: 'ElectronicSealApplicationList' })

const statusOptions = toOptions(applicationStatusLabels)

const query = reactive<{ sealId?: string; status?: ApplicationStatus; applicantId?: string }>({})
const { page, size, total, resetPage } = usePage()
const list = ref<ElectronicSealApplicationVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<ElectronicSealApplicationVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  sealId: '',
  purpose: '',
  projectName: '',
  contractName: '',
  stampPosition: '',
  applicantName: ''
})

const rules: FormRules<typeof form> = {
  sealId: [{ required: true, message: '请输入印章 ID', trigger: 'blur' }]
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.seal.applicationPage({
      page: page.value,
      size: size.value,
      sealId: query.sealId,
      status: query.status,
      applicantId: query.applicantId
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载用印申请失败'))
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
  Object.assign(form, {
    sealId: '',
    purpose: '',
    projectName: '',
    contractName: '',
    stampPosition: '',
    applicantName: ''
  })
  dialogVisible.value = true
}

function openEdit(row: ElectronicSealApplicationVO): void {
  editingId.value = row.id
  form.sealId = row.sealId
  form.purpose = row.purpose ?? ''
  form.projectName = row.projectName ?? ''
  form.contractName = row.contractName ?? ''
  form.stampPosition = row.stampPosition ?? ''
  form.applicantName = row.applicantName ?? ''
  dialogVisible.value = true
}

function openDetail(row: ElectronicSealApplicationVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.seal.applicationUpdate(editingId.value, {
        purpose: form.purpose || null,
        projectName: form.projectName || null,
        contractName: form.contractName || null,
        stampPosition: form.stampPosition || null
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.seal.applicationCreate({
        sealId: form.sealId,
        purpose: form.purpose || null,
        projectName: form.projectName || null,
        contractName: form.contractName || null,
        stampPosition: form.stampPosition || null,
        applicantName: form.applicantName || null
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存失败'))
  }
}

async function onRemove(row: ElectronicSealApplicationVO): Promise<void> {
  try {
    await ElMessageBox.confirm('确定删除该用印申请吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.seal.applicationRemove(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '删除失败'))
  }
}

async function onCancel(row: ElectronicSealApplicationVO): Promise<void> {
  try {
    await ElMessageBox.confirm('确定取消该用印申请吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.seal.applicationCancel(row.id)
    ElMessage.success('已取消')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '取消失败'))
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="toolbar">
      <el-input v-model="query.sealId" placeholder="印章 ID" clearable style="width: 180px" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-input v-model="query.applicantId" placeholder="申请人 ID" clearable style="width: 180px" />
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="openCreate">新增申请</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="sealId" label="印章 ID" width="170" show-overflow-tooltip />
      <el-table-column prop="sealName" label="印章名称" width="130" />
      <el-table-column prop="applicantName" label="申请人" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ applicationStatusLabels[(row as ElectronicSealApplicationVO).status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="purpose" label="用途" min-width="140" show-overflow-tooltip />
      <el-table-column prop="projectName" label="项目名称" width="130" show-overflow-tooltip />
      <el-table-column prop="contractName" label="合同名称" width="130" show-overflow-tooltip />
      <el-table-column prop="applyTime" label="申请时间" width="180" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row as ElectronicSealApplicationVO)">详情</el-button>
          <el-button link type="primary" size="small" @click="openEdit(row as ElectronicSealApplicationVO)">编辑</el-button>
          <el-button
            v-if="(row as ElectronicSealApplicationVO).status === 'PENDING'"
            link
            type="warning"
            size="small"
            @click="onCancel(row as ElectronicSealApplicationVO)"
          >
            取消
          </el-button>
          <el-button link type="danger" size="small" @click="onRemove(row as ElectronicSealApplicationVO)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑用印申请' : '新增用印申请'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="印章 ID" prop="sealId">
          <el-input v-model="form.sealId" :disabled="!!editingId" placeholder="请输入印章 ID" />
        </el-form-item>
        <el-form-item label="申请人">
          <el-input v-model="form.applicantName" :disabled="!!editingId" placeholder="申请人姓名（可选）" />
        </el-form-item>
        <el-form-item label="用途">
          <el-input v-model="form.purpose" type="textarea" :rows="2" placeholder="用途（可选）" />
        </el-form-item>
        <el-form-item label="项目名称">
          <el-input v-model="form.projectName" placeholder="项目名称（可选）" />
        </el-form-item>
        <el-form-item label="合同名称">
          <el-input v-model="form.contractName" placeholder="合同名称（可选）" />
        </el-form-item>
        <el-form-item label="盖章位置">
          <el-input v-model="form.stampPosition" placeholder="盖章位置（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="用印申请详情" width="600px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="印章 ID">{{ detail.sealId }}</el-descriptions-item>
        <el-descriptions-item label="印章名称">{{ detail.sealName ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ detail.applicantName ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ applicationStatusLabels[detail.status] }}</el-descriptions-item>
        <el-descriptions-item label="项目名称">{{ detail.projectName ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="合同名称">{{ detail.contractName ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="用途" :span="2">{{ detail.purpose ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="盖章位置" :span="2">{{ detail.stampPosition ?? '-' }}</el-descriptions-item>
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
