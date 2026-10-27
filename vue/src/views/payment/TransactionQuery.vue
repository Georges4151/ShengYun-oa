<template>
  <div class="page-container">
    <el-card>
      <template #header><div class="card-header"><span>收支查询</span></div></template>
      <div class="filter-container">
        <el-form :inline="true" :model="queryParams">
          <el-form-item label="账户">
            <el-select v-model="queryParams.accountId" placeholder="全部账户" clearable @change="fetchData">
              <el-option v-for="item in accountList" :key="item.id" :label="item.accountName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="交易类型">
            <el-select v-model="queryParams.type" placeholder="全部类型" clearable @change="fetchData">
              <el-option label="收入" value="收入" />
              <el-option label="支出" value="支出" />
            </el-select>
          </el-form-item>
          <el-form-item label="日期范围">
            <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" @change="handleDateChange" />
          </el-form-item>
        </el-form>
      </div>
      <el-table v-loading="loading" :data="tableData" border stripe show-summary :summary-method="getSummaries">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="accountName" label="账户" width="150" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }"><el-tag :type="row.type === '收入' ? 'success' : 'danger'">{{ row.type }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="120">
          <template #default="{ row }"><span :class="row.type === '收入' ? 'text-success' : 'text-danger'">{{ row.type === '收入' ? '+' : '-' }}{{ formatMoney(row.amount) }}</span></template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="transactionTime" label="交易时间" width="160" />
      </el-table>
      <div class="pagination-container">
        <el-pagination v-model:current-page="queryParams.page" v-model:page-size="queryParams.pageSize" :total="total" layout="total, sizes, prev, pager, next" @size-change="fetchData" @current-change="fetchData" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getTransactionList } from '@/api/payment'
import { getPaymentAccountList } from '@/api/payment'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const accountList = ref([])
const dateRange = ref([])

const queryParams = reactive({ page: 1, pageSize: 10, accountId: null, type: '', startDate: '', endDate: '' })

async function fetchAccountList() {
  try { const res = await getPaymentAccountList({}); accountList.value = res.list || res || [] } catch (error) { console.error(error) }
}

async function fetchData() {
  loading.value = true
  try { const res = await getTransactionList(queryParams); tableData.value = res.list || res || []; total.value = res.total || tableData.value.length } catch (error) { console.error(error) }
  finally { loading.value = false }
}

function handleDateChange(val) {
  if (val) { queryParams.startDate = val[0]; queryParams.endDate = val[1] } else { queryParams.startDate = ''; queryParams.endDate = '' }
  fetchData()
}

function getSummaries({ columns, data }) {
  const sums = []
  columns.forEach((column, index) => {
    if (index === 0) { sums[index] = '合计'; return }
    if (column.property === 'amount') {
      const income = data.filter(item => item.type === '收入').reduce((sum, item) => sum + (Number(item.amount) || 0), 0)
      const expense = data.filter(item => item.type === '支出').reduce((sum, item) => sum + (Number(item.amount) || 0), 0)
      sums[index] = `收入: ${formatMoney(income)} | 支出: ${formatMoney(expense)}`
    } else { sums[index] = '' }
  })
  return sums
}

function formatMoney(value) {
  if (!value) return '¥0.00'
  return '¥' + Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

onMounted(() => { fetchAccountList(); fetchData() })
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.filter-container { margin-bottom: 20px; }
.pagination-container { margin-top: 20px; display: flex; justify-content: flex-end; }
.text-success { color: #67c23a; }
.text-danger { color: #f56c6c; }
</style>
