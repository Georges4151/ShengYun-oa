<template>
  <div class="page-container">
    <el-card>
      <template #header><div class="card-header"><span>银行对账</span></div></template>
      <div class="filter-container">
        <el-form :inline="true" :model="queryParams">
          <el-form-item label="账户">
            <el-select v-model="queryParams.accountId" placeholder="请选择账户" @change="fetchData">
              <el-option v-for="item in accountList" :key="item.id" :label="item.accountName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="对账月份">
            <el-date-picker v-model="queryParams.month" type="month" placeholder="选择月份" value-format="YYYY-MM" @change="fetchData" />
          </el-form-item>
        </el-form>
      </div>
      <el-row :gutter="20" class="summary-row">
        <el-col :span="8"><el-statistic title="账面余额" :value="reconciliationData.bookBalance || 0" :precision="2" prefix="¥" /></el-col>
        <el-col :span="8"><el-statistic title="银行余额" :value="reconciliationData.bankBalance || 0" :precision="2" prefix="¥" /></el-col>
        <el-col :span="8"><el-statistic title="差异金额" :value="reconciliationData.difference || 0" :precision="2" prefix="¥" /></el-col>
      </el-row>
      <el-table :data="reconciliationData.details || []" border stripe>
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column prop="bookAmount" label="账面金额" width="120">
          <template #default="{ row }">{{ formatMoney(row.bookAmount) }}</template>
        </el-table-column>
        <el-table-column prop="bankAmount" label="银行金额" width="120">
          <template #default="{ row }">{{ formatMoney(row.bankAmount) }}</template>
        </el-table-column>
        <el-table-column prop="difference" label="差异" width="120">
          <template #default="{ row }"><span :class="row.difference ? 'text-danger' : ''">{{ formatMoney(row.difference) }}</span></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }"><el-tag :type="row.status === '已对账' ? 'success' : 'warning'">{{ row.status }}</el-tag></template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getBankReconciliation } from '@/api/payment'
import { getPaymentAccountList } from '@/api/payment'

const accountList = ref([])
const queryParams = reactive({ accountId: null, month: '' })
const reconciliationData = ref({ bookBalance: 0, bankBalance: 0, difference: 0, details: [] })

async function fetchAccountList() {
  try { const res = await getPaymentAccountList({}); accountList.value = res.list || res || []; if (accountList.value.length > 0) { queryParams.accountId = accountList.value[0].id; fetchData() } } catch (error) { console.error(error) }
}

async function fetchData() {
  if (!queryParams.accountId) return
  try { const res = await getBankReconciliation(queryParams); reconciliationData.value = res || {} } catch (error) { console.error(error) }
}

function formatMoney(value) {
  if (!value) return '¥0.00'
  return '¥' + Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

onMounted(() => { fetchAccountList() })
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.filter-container { margin-bottom: 20px; }
.summary-row { margin-bottom: 20px; padding: 20px; background: #f5f7fa; border-radius: 4px; }
.text-danger { color: #f56c6c; }
</style>
