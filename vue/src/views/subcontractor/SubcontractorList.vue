<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>分包商列表</span>
          <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon>添加分包商</el-button>
        </div>
      </template>
      <div class="filter-container">
        <el-form :inline="true" :model="queryParams">
          <el-form-item label="分包商名称">
            <el-input v-model="queryParams.name" placeholder="请输入名称" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleQuery"><el-icon><Search /></el-icon>搜索</el-button>
            <el-button @click="handleReset"><el-icon><Refresh /></el-icon>重置</el-button>
          </el-form-item>
        </el-form>
      </div>
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="分包商名称" min-width="150" />
        <el-table-column prop="contactPerson" label="联系人" width="100" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="qualification" label="资质等级" width="100" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleViewContracts(row)">查看合同</el-button>
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
import { getSubcontractorList, deleteSubcontractor } from '@/api/subcontractor'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const queryParams = reactive({ page: 1, pageSize: 10, name: '' })

async function fetchData() {
  loading.value = true
  try { const res = await getSubcontractorList(queryParams); tableData.value = res.list || res || []; total.value = res.total || tableData.value.length } catch (error) { console.error(error) }
  finally { loading.value = false }
}

function handleQuery() { queryParams.page = 1; fetchData() }
function handleReset() { queryParams.name = ''; handleQuery() }
function handleAdd() { router.push('/subcontractor/add') }
function handleEdit(row) { router.push(`/subcontractor/edit/${row.id}`) }
function handleViewContracts(row) { router.push({ path: '/subcontractor/contract', query: { id: row.id, name: row.name } }) }

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该分包商？', '提示', { type: 'warning' })
    await deleteSubcontractor(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) { if (error !== 'cancel') ElMessage.error('删除失败') }
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.filter-container { margin-bottom: 20px; }
.pagination-container { margin-top: 20px; display: flex; justify-content: flex-end; }
</style>
