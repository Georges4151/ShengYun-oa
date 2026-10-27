<template>
  <div class="page-container">
    <el-card>
      <template #header><div class="card-header"><span>操作日志</span></div></template>
      <div class="filter-container">
        <el-form :inline="true" :model="queryParams">
          <el-form-item label="操作人">
            <el-input v-model="queryParams.operator" placeholder="请输入操作人" clearable />
          </el-form-item>
          <el-form-item label="操作类型">
            <el-select v-model="queryParams.type" placeholder="全部" clearable>
              <el-option label="登录" value="登录" />
              <el-option label="新增" value="新增" />
              <el-option label="修改" value="修改" />
              <el-option label="删除" value="删除" />
            </el-select>
          </el-form-item>
          <el-form-item label="时间范围">
            <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleQuery"><el-icon><Search /></el-icon>搜索</el-button>
            <el-button @click="handleReset"><el-icon><Refresh /></el-icon>重置</el-button>
          </el-form-item>
        </el-form>
      </div>
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column prop="type" label="操作类型" width="100">
          <template #default="{ row }"><el-tag :type="getTypeColor(row.type)">{{ row.type }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="module" label="操作模块" width="120" />
        <el-table-column prop="description" label="操作描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP地址" width="130" />
        <el-table-column prop="createTime" label="操作时间" width="160" />
      </el-table>
      <div class="pagination-container">
        <el-pagination v-model:current-page="queryParams.page" v-model:page-size="queryParams.pageSize" :total="total" layout="total, sizes, prev, pager, next" @size-change="fetchData" @current-change="fetchData" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getOperationLogs } from '@/api/system'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dateRange = ref([])

const queryParams = reactive({ page: 1, pageSize: 10, operator: '', type: '', startDate: '', endDate: '' })

async function fetchData() {
  if (dateRange.value && dateRange.value.length === 2) {
    queryParams.startDate = dateRange.value[0]
    queryParams.endDate = dateRange.value[1]
  } else {
    queryParams.startDate = ''
    queryParams.endDate = ''
  }

  loading.value = true
  try { const res = await getOperationLogs(queryParams); tableData.value = res.list || res || []; total.value = res.total || tableData.value.length } catch (error) { console.error(error) }
  finally { loading.value = false }
}

function handleQuery() { queryParams.page = 1; fetchData() }
function handleReset() { queryParams.operator = ''; queryParams.type = ''; dateRange.value = []; handleQuery() }

function getTypeColor(type) {
  const map = { '登录': 'primary', '新增': 'success', '修改': 'warning', '删除': 'danger' }
  return map[type] || 'info'
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.filter-container { margin-bottom: 20px; }
.pagination-container { margin-top: 20px; display: flex; justify-content: flex-end; }
</style>
