<template>
  <div class="page-container">
    <el-card>
      <template #header><div class="card-header"><span>材料报表</span></div></template>
      <el-form :inline="true" :model="queryParams" class="filter-container">
        <el-form-item label="关联项目">
          <el-select v-model="queryParams.projectId" placeholder="全部项目" clearable @change="fetchData">
            <el-option v-for="item in projectList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-row :gutter="20" class="stats-row">
        <el-col :span="6"><el-statistic title="材料种类" :value="reportData.materialTypes || 0" /></el-col>
        <el-col :span="6"><el-statistic title="总数量" :value="reportData.totalQuantity || 0" /></el-col>
        <el-col :span="6"><el-statistic title="总金额" :value="reportData.totalAmount || 0" :precision="2" prefix="¥" /></el-col>
        <el-col :span="6"><el-statistic title="平均单价" :value="reportData.avgPrice || 0" :precision="2" prefix="¥" /></el-col>
      </el-row>
      <el-table :data="reportData.details || []" border stripe>
        <el-table-column prop="name" label="材料名称" />
        <el-table-column prop="specification" label="规格型号" width="120" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="quantity" label="数量" width="100" />
        <el-table-column prop="unitPrice" label="单价" width="100">
          <template #default="{ row }">{{ formatMoney(row.unitPrice) }}</template>
        </el-table-column>
        <el-table-column prop="totalPrice" label="总价" width="120">
          <template #default="{ row }">{{ formatMoney(row.quantity * row.unitPrice) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getMaterialReport } from '@/api/material'
import { getProjectList } from '@/api/project'

const projectList = ref([])
const queryParams = reactive({ projectId: null })
const reportData = ref({ materialTypes: 0, totalQuantity: 0, totalAmount: 0, avgPrice: 0, details: [] })

async function fetchProjectList() {
  try { const res = await getProjectList({ page: 1, pageSize: 100 }); projectList.value = res.list || res || [] } catch (error) { console.error(error) }
}

async function fetchData() {
  try { const res = await getMaterialReport(queryParams); reportData.value = res || {} } catch (error) { console.error(error) }
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
.stats-row { margin-bottom: 20px; padding: 20px; background: #f5f7fa; border-radius: 4px; }
</style>
