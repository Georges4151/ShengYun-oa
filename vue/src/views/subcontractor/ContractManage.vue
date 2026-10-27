<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ subcontractorName ? `${subcontractorName} - 合同管理` : '合同管理' }}</span>
          <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon>添加合同</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="contractNumber" label="合同编号" width="150" />
        <el-table-column prop="subcontractorName" label="分包商" width="150">
          <template #default="{ row }">{{ row.subcontractorName || row.subcontractor?.name || '-' }}</template>
        </el-table-column>
        <el-table-column prop="projectName" label="关联项目" width="150">
          <template #default="{ row }">{{ row.projectName || row.project?.name || '-' }}</template>
        </el-table-column>
        <el-table-column prop="contractAmount" label="合同金额" width="120">
          <template #default="{ row }">{{ formatMoney(row.contractAmount) }}</template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" width="120" />
        <el-table-column prop="endDate" label="结束日期" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }"><el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column label="文件" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.filePath" type="success" @click="handlePreview(row)" style="cursor:pointer;">查看文件</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">查看</el-button>
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑合同' : '添加合同'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="合同编号" prop="contractNumber"><el-input v-model="form.contractNumber" placeholder="请输入合同编号" /></el-form-item>
        <el-form-item label="分包商" prop="subcontractorId">
          <el-select v-model="form.subcontractorId" placeholder="请选择分包商" style="width: 100%;" :disabled="!!subcontractorId">
            <el-option v-for="item in subcontractorList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联项目" prop="projectId">
          <el-select v-model="form.projectId" placeholder="请选择项目" style="width: 100%;">
            <el-option v-for="item in projectList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="合同金额" prop="contractAmount"><el-input-number v-model="form.contractAmount" :min="0" :precision="2" style="width: 100%;" /></el-form-item>
        <el-form-item label="开始日期" prop="startDate"><el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%;" /></el-form-item>
        <el-form-item label="结束日期" prop="endDate"><el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%;" /></el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%;">
            <el-option label="执行中" value="执行中" />
            <el-option label="已完成" value="已完成" />
            <el-option label="已终止" value="已终止" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="viewDialogVisible" title="合同详情" width="700px">
      <el-descriptions :column="2" border v-if="currentContract">
        <el-descriptions-item label="合同编号">{{ currentContract.contractNumber }}</el-descriptions-item>
        <el-descriptions-item label="分包商">{{ currentContract.subcontractorName || currentContract.subcontractor?.name }}</el-descriptions-item>
        <el-descriptions-item label="关联项目">{{ currentContract.projectName || currentContract.project?.name }}</el-descriptions-item>
        <el-descriptions-item label="合同金额">{{ formatMoney(currentContract.contractAmount) }}</el-descriptions-item>
        <el-descriptions-item label="开始日期">{{ currentContract.startDate }}</el-descriptions-item>
        <el-descriptions-item label="结束日期">{{ currentContract.endDate }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentContract.status)">{{ currentContract.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentContract.createTime || currentContract.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="合同内容" :span="2">{{ currentContract.content || currentContract.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentContract.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="合同文件" :span="2">
          <template v-if="currentContract.filePath">
            <el-button type="primary" @click="handlePreview(currentContract)">预览/下载文件</el-button>
          </template>
          <template v-else>-</template>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getContractList, createContract, updateContract, deleteContract, getSubcontractorDetail, getSubcontractorList } from '@/api/subcontractor'
import { getProjectList } from '@/api/project'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const viewDialogVisible = ref(false)
const submitLoading = ref(false)
const subcontractorList = ref([])
const projectList = ref([])
const currentContract = ref(null)

const subcontractorId = computed(() => route.query.id ? Number(route.query.id) : null)
const subcontractorName = computed(() => route.query.name || '')

const formRef = ref(null)
const form = reactive({ id: null, contractNumber: '', subcontractorId: null, projectId: null, contractAmount: 0, startDate: '', endDate: '', status: '执行中', content: '', remark: '' })
const rules = { contractNumber: [{ required: true, message: '请输入合同编号', trigger: 'blur' }], subcontractorId: [{ required: true, message: '请选择分包商', trigger: 'change' }] }

async function fetchSubcontractorList() {
  try { const res = await getSubcontractorList({ page: 1, pageSize: 100 }); subcontractorList.value = res.list || res || [] } catch (error) { console.error(error) }
}

async function fetchProjectList() {
  try { const res = await getProjectList({ page: 1, pageSize: 100 }); projectList.value = res.list || res || [] } catch (error) { console.error(error) }
}

async function fetchData() {
  loading.value = true
  try {
    let res
    if (subcontractorId.value) {
      res = await getContractList({ subcontractorId: subcontractorId.value })
    } else {
      res = await getContractList({})
    }
    tableData.value = res.list || res || []
  } catch (error) {
    console.error(error)
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  Object.assign(form, { id: null, contractNumber: '', subcontractorId: subcontractorId.value, projectId: null, contractAmount: 0, startDate: '', endDate: '', status: '执行中', content: '', remark: '' })
  dialogVisible.value = true
}

function handleView(row) {
  currentContract.value = row
  viewDialogVisible.value = true
}

function handlePreview(row) {
  if (!row.filePath) {
    ElMessage.warning('该合同没有附件文件')
    return
  }
  
  // 使用相对路径，开发环境走 Vite 代理，生产环境走同源或 Nginx 反向代理
  const fileUrl = `/api/files/contract/file?path=${encodeURIComponent(row.filePath)}`
  
  window.open(fileUrl, '_blank')
}

function handleEdit(row) {
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该合同？', '提示', { type: 'warning' })
    await deleteContract(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) { if (error !== 'cancel') ElMessage.error('删除失败') }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (form.id) { await updateContract(form.id, form) } else { await createContract(form) }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    fetchData()
  } catch (error) { ElMessage.error('操作失败') }
  finally { submitLoading.value = false }
}

function formatMoney(value) {
  if (!value) return '¥0.00'
  return '¥' + Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

function getStatusType(status) {
  const map = { '执行中': 'primary', '已完成': 'success', '已终止': 'danger' }
  return map[status] || 'info'
}

onMounted(() => {
  fetchSubcontractorList()
  fetchProjectList()
  fetchData()
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
