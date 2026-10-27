<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>账目列表</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>添加账目
          </el-button>
        </div>
      </template>

      <div class="filter-container">
        <el-form :inline="true" :model="queryParams">
          <el-form-item label="关联项目">
            <el-select v-model="queryParams.projectId" placeholder="请选择项目" clearable style="width: 200px;">
              <el-option v-for="item in projectList" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="账目类型">
            <el-select v-model="queryParams.type" placeholder="请选择类型" clearable>
              <el-option label="收入" value="收入" />
              <el-option label="支出" value="支出" />
            </el-select>
          </el-form-item>
          <el-form-item label="日期范围">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              @change="handleDateChange"
            />
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

      <el-table v-loading="loading" :data="tableData" border stripe show-summary :summary-method="getSummaries">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="projectName" label="关联项目" width="150" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === '收入' ? 'success' : 'danger'">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="120">
          <template #default="{ row }">
            <span :class="row.type === '收入' ? 'text-success' : 'text-danger'">
              {{ row.type === '收入' ? '+' : '-' }}{{ formatMoney(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="transactionDate" label="交易日期" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
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
import { getAccountList, deleteAccount } from '@/api/account'
import { getProjectList } from '@/api/project'

const router = useRouter()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const projectList = ref([])
const dateRange = ref([])

const queryParams = reactive({
  page: 1,
  pageSize: 10,
  projectId: null,
  type: '',
  startDate: '',
  endDate: ''
})

async function fetchProjectList() {
  try {
    const res = await getProjectList({ page: 1, pageSize: 100 })
    projectList.value = res.list || res || []
  } catch (error) {
    console.error('获取项目列表失败:', error)
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getAccountList(queryParams)
    tableData.value = res.list || res || []
    total.value = res.total || tableData.value.length
  } catch (error) {
    console.error('获取账目列表失败:', error)
  } finally {
    loading.value = false
  }
}

function handleDateChange(val) {
  if (val) {
    queryParams.startDate = val[0]
    queryParams.endDate = val[1]
  } else {
    queryParams.startDate = ''
    queryParams.endDate = ''
  }
}

function handleQuery() {
  queryParams.page = 1
  fetchData()
}

function handleReset() {
  queryParams.projectId = null
  queryParams.type = ''
  queryParams.startDate = ''
  queryParams.endDate = ''
  dateRange.value = []
  handleQuery()
}

function handleAdd() {
  router.push('/account/add')
}

function handleEdit(row) {
  router.push(`/account/edit/${row.id}`)
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定要删除该账目吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteAccount(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

function getSummaries({ columns, data }) {
  const sums = []
  columns.forEach((column, index) => {
    if (index === 0) {
      sums[index] = '合计'
      return
    }
    if (column.property === 'amount') {
      const income = data.filter(item => item.type === '收入').reduce((sum, item) => sum + (Number(item.amount) || 0), 0)
      const expense = data.filter(item => item.type === '支出').reduce((sum, item) => sum + (Number(item.amount) || 0), 0)
      sums[index] = `收入: ${formatMoney(income)} | 支出: ${formatMoney(expense)}`
    } else {
      sums[index] = ''
    }
  })
  return sums
}

function formatMoney(value) {
  if (!value) return '¥0.00'
  return '¥' + Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

function getStatusType(status) {
  const map = {
    '已审核': 'success',
    '待审核': 'warning',
    '已拒绝': 'danger',
    '草稿': 'info'
  }
  return map[status] || 'info'
}

onMounted(() => {
  fetchProjectList()
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

.text-success {
  color: #67c23a;
}

.text-danger {
  color: #f56c6c;
}
</style>
