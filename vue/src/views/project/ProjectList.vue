<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>项目列表</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>添加项目
          </el-button>
        </div>
      </template>

      <div class="filter-container">
        <el-form :inline="true" :model="queryParams">
          <el-form-item label="项目名称">
            <el-input v-model="queryParams.name" placeholder="请输入项目名称" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="项目状态">
            <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
              <el-option label="进行中" value="进行中" />
              <el-option label="已完成" value="已完成" />
              <el-option label="暂停" value="暂停" />
              <el-option label="未开始" value="未开始" />
            </el-select>
          </el-form-item>
          <el-form-item label="客户名称">
            <el-input v-model="queryParams.clientName" placeholder="请输入客户名称" clearable @keyup.enter="handleQuery" />
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
        <el-table-column prop="name" label="项目名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="clientName" label="客户名称" width="120" />
        <el-table-column prop="location" label="项目地点" width="120" show-overflow-tooltip />
        <el-table-column prop="budget" label="预算金额" width="120">
          <template #default="{ row }">
            {{ formatMoney(row.budget) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" width="120" />
        <el-table-column prop="endDate" label="结束日期" width="120" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">查看</el-button>
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="primary" link @click="handleCostAnalysis(row)">成本分析</el-button>
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
          @size-change="handleQuery"
          @current-change="handleQuery"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProjectList, deleteProject } from '@/api/project'

const router = useRouter()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)

const queryParams = reactive({
  page: 1,
  pageSize: 10,
  name: '',
  status: '',
  clientName: ''
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getProjectList(queryParams)
    tableData.value = res.list || res || []
    total.value = res.total || tableData.value.length
  } catch (error) {
    console.error('获取项目列表失败:', error)
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.page = 1
  fetchData()
}

function handleReset() {
  queryParams.name = ''
  queryParams.status = ''
  queryParams.clientName = ''
  handleQuery()
}

function handleAdd() {
  router.push('/project/add')
}

function handleView(row) {
  router.push(`/project/edit/${row.id}`)
}

function handleEdit(row) {
  router.push(`/project/edit/${row.id}`)
}

function handleCostAnalysis(row) {
  router.push(`/project/cost-analysis/${row.id}`)
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定要删除该项目吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteProject(row.id)
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
    '进行中': 'primary',
    '已完成': 'success',
    '暂停': 'warning',
    '未开始': 'info'
  }
  return map[status] || 'info'
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
