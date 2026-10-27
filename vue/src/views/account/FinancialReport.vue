<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>财务报表</span>
          <div>
            <el-button @click="handleExport">
              <el-icon><Download /></el-icon>导出报表
            </el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="queryParams" class="filter-container">
        <el-form-item label="报表年度">
          <el-date-picker
            v-model="queryParams.year"
            type="year"
            placeholder="选择年度"
            value-format="YYYY"
            @change="fetchData"
          />
        </el-form-item>
        <el-form-item label="关联项目">
          <el-select v-model="queryParams.projectId" placeholder="全部项目" clearable style="width: 200px;" @change="fetchData">
            <el-option v-for="item in projectList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-row :gutter="20" class="summary-row">
        <el-col :span="6">
          <el-card shadow="hover">
            <el-statistic title="总收入" :value="summary.totalIncome" :precision="2" prefix="¥">
              <template #suffix>
                <span class="text-success">元</span>
              </template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <el-statistic title="总支出" :value="summary.totalExpense" :precision="2" prefix="¥">
              <template #suffix>
                <span class="text-danger">元</span>
              </template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <el-statistic title="净利润" :value="summary.netProfit" :precision="2" prefix="¥">
              <template #suffix>
                <span :class="summary.netProfit >= 0 ? 'text-success' : 'text-danger'">元</span>
              </template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <el-statistic title="交易笔数" :value="summary.transactionCount">
              <template #suffix>
                <span>笔</span>
              </template>
            </el-statistic>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="chart-row">
        <el-col :span="12">
          <div ref="lineChartRef" class="chart-container"></div>
        </el-col>
        <el-col :span="12">
          <div ref="pieChartRef" class="chart-container"></div>
        </el-col>
      </el-row>

      <el-table :data="monthlyData" border stripe style="margin-top: 20px;">
        <el-table-column prop="month" label="月份" width="100" />
        <el-table-column prop="income" label="收入">
          <template #default="{ row }">
            <span class="text-success">{{ formatMoney(row.income) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="expense" label="支出">
          <template #default="{ row }">
            <span class="text-danger">{{ formatMoney(row.expense) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="profit" label="利润">
          <template #default="{ row }">
            <span :class="row.profit >= 0 ? 'text-success' : 'text-danger'">
              {{ formatMoney(row.profit) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="count" label="交易笔数" width="100" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { getAccountSummary, getMonthlyStats } from '@/api/account'
import { getProjectList } from '@/api/project'

const lineChartRef = ref(null)
const pieChartRef = ref(null)
let lineChart = null
let pieChart = null

const projectList = ref([])

const queryParams = reactive({
  year: new Date().getFullYear().toString(),
  projectId: null
})

const summary = ref({
  totalIncome: 0,
  totalExpense: 0,
  netProfit: 0,
  transactionCount: 0
})

const monthlyData = ref([])

async function fetchProjectList() {
  try {
    const res = await getProjectList({ page: 1, pageSize: 100 })
    projectList.value = res.list || res || []
  } catch (error) {
    console.error('获取项目列表失败:', error)
  }
}

async function fetchData() {
  try {
    const [summaryRes, monthlyRes] = await Promise.all([
      getAccountSummary(queryParams),
      getMonthlyStats(queryParams.year)
    ])
    
    summary.value = summaryRes || {}
    monthlyData.value = monthlyRes || generateMonthlyData()
    
    initCharts()
  } catch (error) {
    console.error('获取报表数据失败:', error)
    monthlyData.value = generateMonthlyData()
    initCharts()
  }
}

function generateMonthlyData() {
  return Array.from({ length: 12 }, (_, i) => ({
    month: `${i + 1}月`,
    income: Math.floor(Math.random() * 100000),
    expense: Math.floor(Math.random() * 80000),
    profit: 0,
    count: Math.floor(Math.random() * 50) + 10
  })).map(item => ({
    ...item,
    profit: item.income - item.expense
  }))
}

function initCharts() {
  initLineChart()
  initPieChart()
}

function initLineChart() {
  if (!lineChartRef.value) return
  
  lineChart = echarts.init(lineChartRef.value)
  
  const option = {
    title: {
      text: '月度收支趋势',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['收入', '支出', '利润'],
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
      data: monthlyData.value.map(item => item.month)
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '收入',
        type: 'line',
        data: monthlyData.value.map(item => item.income),
        itemStyle: { color: '#67c23a' }
      },
      {
        name: '支出',
        type: 'line',
        data: monthlyData.value.map(item => item.expense),
        itemStyle: { color: '#f56c6c' }
      },
      {
        name: '利润',
        type: 'line',
        data: monthlyData.value.map(item => item.profit),
        itemStyle: { color: '#409eff' }
      }
    ]
  }
  
  lineChart.setOption(option)
}

function initPieChart() {
  if (!pieChartRef.value) return
  
  pieChart = echarts.init(pieChartRef.value)
  
  const option = {
    title: {
      text: '支出分类占比',
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
        radius: ['40%', '70%'],
        data: [
          { value: 35000, name: '人工费', itemStyle: { color: '#409eff' } },
          { value: 28000, name: '材料费', itemStyle: { color: '#67c23a' } },
          { value: 15000, name: '设备费', itemStyle: { color: '#e6a23c' } },
          { value: 8000, name: '管理费', itemStyle: { color: '#f56c6c' } },
          { value: 5000, name: '其他', itemStyle: { color: '#909399' } }
        ]
      }
    ]
  }
  
  pieChart.setOption(option)
}

function handleResize() {
  lineChart?.resize()
  pieChart?.resize()
}

function handleExport() {
  // 导出报表逻辑
}

function formatMoney(value) {
  if (!value) return '¥0.00'
  return '¥' + Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

onMounted(() => {
  fetchProjectList()
  fetchData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  lineChart?.dispose()
  pieChart?.dispose()
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

.summary-row {
  margin-bottom: 20px;
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
