<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>项目成本分析</span>
          <el-button @click="handleBack">返回列表</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="queryParams" class="filter-container">
        <el-form-item label="选择项目">
          <el-select v-model="queryParams.projectId" placeholder="请选择项目" style="width: 300px;" @change="fetchData">
            <el-option v-for="item in projectList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-row :gutter="20" class="stats-row" v-if="costData.projectName">
        <el-col :span="6">
          <el-statistic title="项目名称" :value="costData.projectName" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="预算金额" :value="costData.budget || 0" :precision="2" prefix="¥" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="已支出金额" :value="costData.spent || 0" :precision="2" prefix="¥" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="剩余金额" :value="costData.remaining || 0" :precision="2" prefix="¥" />
        </el-col>
      </el-row>

      <el-row :gutter="20" class="chart-row">
        <el-col :span="12">
          <div ref="pieChartRef" class="chart-container"></div>
        </el-col>
        <el-col :span="12">
          <div ref="barChartRef" class="chart-container"></div>
        </el-col>
      </el-row>

      <el-table :data="costData.details || []" border stripe style="margin-top: 20px;">
        <el-table-column prop="category" label="费用类别" width="150" />
        <el-table-column prop="budget" label="预算金额" width="150">
          <template #default="{ row }">
            {{ formatMoney(row.budget) }}
          </template>
        </el-table-column>
        <el-table-column prop="actual" label="实际支出" width="150">
          <template #default="{ row }">
            {{ formatMoney(row.actual) }}
          </template>
        </el-table-column>
        <el-table-column prop="difference" label="差异" width="150">
          <template #default="{ row }">
            <span :class="row.difference >= 0 ? 'text-success' : 'text-danger'">
              {{ formatMoney(row.difference) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="percentage" label="使用比例" width="150">
          <template #default="{ row }">
            <el-progress :percentage="row.percentage || 0" :status="row.percentage > 100 ? 'exception' : ''" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import * as echarts from 'echarts'
import { getProjectList, getProjectCostAnalysis } from '@/api/project'

const router = useRouter()
const route = useRoute()

const pieChartRef = ref(null)
const barChartRef = ref(null)
let pieChart = null
let barChart = null

const projectList = ref([])
const queryParams = reactive({
  projectId: null
})

const costData = ref({
  projectName: '',
  budget: 0,
  spent: 0,
  remaining: 0,
  details: []
})

async function fetchProjectList() {
  try {
    const res = await getProjectList({ page: 1, pageSize: 100 })
    projectList.value = res.list || res || []
    
    if (route.params.id) {
      queryParams.projectId = parseInt(route.params.id)
      fetchData()
    } else if (projectList.value.length > 0) {
      queryParams.projectId = projectList.value[0].id
      fetchData()
    }
  } catch (error) {
    console.error('获取项目列表失败:', error)
  }
}

async function fetchData() {
  if (!queryParams.projectId) return
  
  try {
    const res = await getProjectCostAnalysis(queryParams.projectId)
    costData.value = res || {}
    initCharts()
  } catch (error) {
    console.error('获取成本分析失败:', error)
  }
}

function initCharts() {
  initPieChart()
  initBarChart()
}

function initPieChart() {
  if (!pieChartRef.value) return
  
  pieChart = echarts.init(pieChartRef.value)
  
  const data = (costData.value.details || []).map(item => ({
    name: item.category,
    value: item.actual || 0
  }))
  
  const option = {
    title: {
      text: '成本构成',
      left: 'center'
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: ¥{c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        type: 'pie',
        radius: '50%',
        data: data.length > 0 ? data : [{ name: '暂无数据', value: 1 }],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }
  
  pieChart.setOption(option)
}

function initBarChart() {
  if (!barChartRef.value) return
  
  barChart = echarts.init(barChartRef.value)
  
  const details = costData.value.details || []
  
  const option = {
    title: {
      text: '预算与实际对比',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: ['预算', '实际'],
      top: 30
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: details.map(item => item.category)
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '预算',
        type: 'bar',
        data: details.map(item => item.budget || 0),
        itemStyle: { color: '#409eff' }
      },
      {
        name: '实际',
        type: 'bar',
        data: details.map(item => item.actual || 0),
        itemStyle: { color: '#67c23a' }
      }
    ]
  }
  
  barChart.setOption(option)
}

function handleResize() {
  pieChart?.resize()
  barChart?.resize()
}

function handleBack() {
  router.push('/project/list')
}

function formatMoney(value) {
  if (!value) return '¥0.00'
  return '¥' + Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

onMounted(() => {
  fetchProjectList()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  pieChart?.dispose()
  barChart?.dispose()
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

.stats-row {
  margin-bottom: 20px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 4px;
}

.chart-row {
  margin-top: 20px;
}

.chart-container {
  height: 350px;
}

.text-success {
  color: #67c23a;
}

.text-danger {
  color: #f56c6c;
}
</style>
