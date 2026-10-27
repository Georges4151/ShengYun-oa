<template>
  <div class="page-container">
    <el-card>
      <template #header><div class="card-header"><span>申请审批</span></div></template>
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="sealName" label="印章名称" width="150" />
        <el-table-column prop="applicantName" label="申请人" width="100" />
        <el-table-column prop="purpose" label="使用目的" min-width="200" show-overflow-tooltip />
        <el-table-column prop="useTime" label="使用时间" width="160" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }"><el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'pending'">
              <el-button type="success" link @click="handleApprove(row)">通过</el-button>
              <el-button type="danger" link @click="handleReject(row)">拒绝</el-button>
            </template>
            <span v-else class="text-info">已处理</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="审批意见" width="400px">
      <el-input v-model="comment" type="textarea" :rows="3" placeholder="请输入审批意见（可选）" />
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :type="actionType" :loading="submitLoading" @click="submitApproval">{{ actionType === 'success' ? '确认通过' : '确认拒绝' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getPendingApplications, approveApplication, rejectApplication } from '@/api/electronicSeal'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const submitLoading = ref(false)
const comment = ref('')
const currentRow = ref(null)
const actionType = ref('success')

async function fetchData() {
  loading.value = true
  try { const res = await getPendingApplications(); tableData.value = res || [] } catch (error) { console.error(error) }
  finally { loading.value = false }
}

function handleApprove(row) {
  currentRow.value = row
  actionType.value = 'success'
  comment.value = ''
  dialogVisible.value = true
}

function handleReject(row) {
  currentRow.value = row
  actionType.value = 'danger'
  comment.value = ''
  dialogVisible.value = true
}

async function submitApproval() {
  submitLoading.value = true
  try {
    if (actionType.value === 'success') {
      await approveApplication(currentRow.value.id, comment.value)
      ElMessage.success('已通过')
    } else {
      await rejectApplication(currentRow.value.id, comment.value)
      ElMessage.success('已拒绝')
    }
    dialogVisible.value = false
    fetchData()
  } catch (error) { ElMessage.error('操作失败') }
  finally { submitLoading.value = false }
}

function getStatusType(status) {
  const map = { 'pending': 'warning', 'approved': 'success', 'rejected': 'danger' }
  return map[status] || 'info'
}

function getStatusText(status) {
  const map = { 'pending': '待审批', 'approved': '已通过', 'rejected': '已拒绝' }
  return map[status] || status
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.text-info { color: #909399; }
</style>
