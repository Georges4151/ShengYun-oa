<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>材料列表</span>
          <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon>添加材料</el-button>
        </div>
      </template>

      <div class="filter-container">
        <el-form :inline="true" :model="queryParams">
          <el-form-item label="材料名称">
            <el-input v-model="queryParams.name" placeholder="请输入材料名称" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="关联项目">
            <el-select v-model="queryParams.projectId" placeholder="请选择项目" clearable>
              <el-option v-for="item in projectList" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleQuery"><el-icon><Search /></el-icon>搜索</el-button>
            <el-button @click="handleReset"><el-icon><Refresh /></el-icon>重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="材料名称" min-width="150" />
        <el-table-column prop="specification" label="规格型号" width="120" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="quantity" label="数量" width="100" />
        <el-table-column prop="unitPrice" label="单价" width="100">
          <template #default="{ row }">{{ formatMoney(row.unitPrice) }}</template>
        </el-table-column>
        <el-table-column prop="totalPrice" label="总价" width="120">
          <template #default="{ row }">{{ formatMoney(row.quantity * row.unitPrice) }}</template>
        </el-table-column>
        <el-table-column prop="projectName" label="关联项目" width="150" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination v-model:current-page="queryParams.page" v-model:page-size="queryParams.pageSize" :total="total" layout="total, sizes, prev, pager, next" @size-change="fetchData" @current-change="fetchData" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMaterialList, deleteMaterial } from '@/api/material'
import { getProjectList } from '@/api/project'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const projectList = ref([])

const queryParams = reactive({ page: 1, pageSize: 10, name: '', projectId: null })

async function fetchProjectList() {
  try {
    const res = await getProjectList({ page: 1, pageSize: 100 })
    projectList.value = res.list || res || []
  } catch (error) { console.error(error) }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getMaterialList(queryParams)
    tableData.value = res.list || res || []
    total.value = res.total || tableData.value.length
  } catch (error) { console.error(error) }
  finally { loading.value = false }
}

function handleQuery() { queryParams.page = 1; fetchData() }
function handleReset() { queryParams.name = ''; queryParams.projectId = null; handleQuery() }
function handleAdd() { router.push('/material/add') }
function handleEdit(row) { router.push(`/material/edit/${row.id}`) }

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该材料？', '提示', { type: 'warning' })
    await deleteMaterial(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) { if (error !== 'cancel') ElMessage.error('删除失败') }
}

function formatMoney(value) {
  if (!value) return '¥0.00'
  return '¥' + Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

onMounted(() => { fetchProjectList(); fetchData() })
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.filter-container { margin-bottom: 20px; }
.pagination-container { margin-top: 20px; display: flex; justify-content: flex-end; }
</style>
