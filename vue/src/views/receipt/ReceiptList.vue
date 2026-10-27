<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>票据列表</span>
          <el-button type="primary" @click="handleUpload">
            <el-icon><Upload /></el-icon>上传票据
          </el-button>
        </div>
      </template>

      <div class="filter-container">
        <el-form :inline="true" :model="queryParams">
          <el-form-item label="票据类型">
            <el-select v-model="queryParams.type" placeholder="请选择类型" clearable>
              <el-option label="发票" value="发票" />
              <el-option label="收据" value="收据" />
              <el-option label="银行回单" value="银行回单" />
              <el-option label="其他" value="其他" />
            </el-select>
          </el-form-item>
          <el-form-item label="审核状态">
            <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
              <el-option label="待审核" value="pending" />
              <el-option label="已通过" value="approved" />
              <el-option label="已拒绝" value="rejected" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleQuery">
              <el-icon><Search /></el-icon>搜索
            </el-button>
            <el-button @click="handleReset">
              <el-icon><Refresh /></el-icon>重置
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="type" label="票据类型" width="100" />
        <el-table-column prop="receiptNumber" label="票据编号" width="150" />
        <el-table-column prop="amount" label="金额" width="120">
          <template #default="{ row }">
            {{ formatMoney(row.amount) }}
          </template>
        </el-table-column>
        <el-table-column prop="reporterName" label="报销人" width="100" />
        <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
        <el-table-column prop="verificationStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.verificationStatus)">
              {{ getStatusText(row.verificationStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="uploadTime" label="上传时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">查看</el-button>
            <el-button type="primary" link @click="handleVerify(row)" v-if="row.verificationStatus === 'pending'">验证</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReceiptList, deleteReceipt } from '@/api/receipt'

const router = useRouter()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)

const queryParams = reactive({
  page: 1,
  pageSize: 10,
  type: '',
  status: ''
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getReceiptList(queryParams)
    tableData.value = res.list || res || []
    total.value = res.total || tableData.value.length
  } catch (error) {
    console.error('获取票据列表失败:', error)
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.page = 1
  fetchData()
}

function handleReset() {
  queryParams.type = ''
  queryParams.status = ''
  handleQuery()
}

function handleUpload() {
  router.push('/receipt/upload')
}

function handleView(row) {
  router.push(`/receipt/detail/${row.id}`)
}

function handleVerify(row) {
  router.push(`/receipt/verify/${row.id}`)
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定要删除该票据吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteReceipt(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
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
    'rejected': 'danger',
    'verified': 'success'
  }
  return map[status] || 'info'
}

function getStatusText(status) {
  const map = {
    'pending': '待审核',
    'approved': '已通过',
    'rejected': '已拒绝',
    'verified': '已验证'
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
