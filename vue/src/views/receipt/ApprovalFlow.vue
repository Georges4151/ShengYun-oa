<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>审批流程</span>
        </div>
      </template>

      <div class="filter-container">
        <el-form :inline="true" :model="queryParams">
          <el-form-item label="审批状态">
            <el-select v-model="queryParams.status" placeholder="全部状态" clearable @change="fetchData">
              <el-option label="待审批" value="pending" />
              <el-option label="已通过" value="approved" />
              <el-option label="已拒绝" value="rejected" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="type" label="票据类型" width="100" />
        <el-table-column prop="reporterName" label="报销人" width="100" />
        <el-table-column prop="amount" label="金额" width="120">
          <template #default="{ row }">
            {{ formatMoney(row.amount) }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
        <el-table-column prop="uploadTime" label="提交时间" width="160" />
        <el-table-column prop="verificationStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.verificationStatus)">
              {{ getStatusText(row.verificationStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">查看</el-button>
            <template v-if="row.verificationStatus === 'pending'">
              <el-button type="success" link @click="handleApprove(row)">通过</el-button>
              <el-button type="danger" link @click="handleReject(row)">拒绝</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" title="审批意见" width="500px">
      <el-form :model="approvalForm" label-width="80px">
        <el-form-item label="审批意见">
          <el-input
            v-model="approvalForm.comment"
            type="textarea"
            :rows="3"
            placeholder="请输入审批意见（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :type="approvalForm.status === 'approved' ? 'success' : 'danger'" :loading="submitLoading" @click="submitApproval">
          {{ approvalForm.status === 'approved' ? '确认通过' : '确认拒绝' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPendingReceipts, processApproval } from '@/api/receipt'

const router = useRouter()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const submitLoading = ref(false)

const queryParams = reactive({
  page: 1,
  pageSize: 10,
  status: ''
})

const approvalForm = reactive({
  receiptId: null,
  status: '',
  comment: ''
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getPendingReceipts(queryParams)
    tableData.value = res.list || res || []
    total.value = res.total || tableData.value.length
  } catch (error) {
    console.error('获取审批列表失败:', error)
  } finally {
    loading.value = false
  }
}

function handleView(row) {
  router.push(`/receipt/detail/${row.id}`)
}

function handleApprove(row) {
  approvalForm.receiptId = row.id
  approvalForm.status = 'approved'
  approvalForm.comment = ''
  dialogVisible.value = true
}

function handleReject(row) {
  approvalForm.receiptId = row.id
  approvalForm.status = 'rejected'
  approvalForm.comment = ''
  dialogVisible.value = true
}

async function submitApproval() {
  submitLoading.value = true
  try {
    await processApproval(approvalForm)
    ElMessage.success(approvalForm.status === 'approved' ? '已通过' : '已拒绝')
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    submitLoading.value = false
  }
}

function formatMoney(value) {
  if (!value) return '¥0.00'
  return '¥' + Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

function getStatusType(status) {
  const map = {
    'pending': 'warning',
    'approved': 'success',
    'rejected': 'danger'
  }
  return map[status] || 'info'
}

function getStatusText(status) {
  const map = {
    'pending': '待审批',
    'approved': '已通过',
    'rejected': '已拒绝'
  }
  return map[status] || status
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-container {
  margin-bottom: 20px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
