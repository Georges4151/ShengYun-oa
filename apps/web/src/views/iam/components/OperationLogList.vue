<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  ElButton,
  ElDatePicker,
  ElEmpty,
  ElInput,
  ElOption,
  ElPagination,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus'
import { ceamsApi } from '@/services/http'
import type { OperationLogVO, OperationType } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { operationTypeLabels, toOptions } from '@/utils/labels'

defineOptions({ name: 'OperationLogList' })

const typeOptions = toOptions(operationTypeLabels)

/**
 * 操作类型标签配色（纯视觉映射，不改动枚举值）：
 * 新增/导入成功色、修改/审批警告色、删除危险色、登录登出主色，其余中性。
 */
function operationTagType(type: OperationType): 'success' | 'warning' | 'danger' | 'primary' | 'info' {
  switch (type) {
    case 'CREATE':
    case 'IMPORT':
      return 'success'
    case 'UPDATE':
    case 'APPROVE':
      return 'warning'
    case 'DELETE':
      return 'danger'
    case 'LOGIN':
    case 'LOGOUT':
      return 'primary'
    default:
      return 'info'
  }
}

const query = reactive<{ userId?: string; operationType?: OperationType }>({})
const dateRange = ref<[string, string] | null>(null)
const { page, size, total, resetPage } = usePage()
const list = ref<OperationLogVO[]>([])
const loading = ref(false)

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.iam.logPage({
      page: page.value,
      size: size.value,
      userId: query.userId,
      operationType: query.operationType,
      startTime: dateRange.value?.[0],
      endTime: dateRange.value?.[1]
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    const message = apiErrorMessage(err, '加载操作日志失败')
    // 只读列表，失败时用控制台提示并清空，避免重复弹窗
    list.value = []
    total.value = 0
    // eslint-disable-next-line no-console
    console.error(message)
  } finally {
    loading.value = false
  }
}

function onSearch(): void {
  resetPage()
  void load()
}

onMounted(load)
</script>

<template>
  <div>
    <!-- 筛选卡片：控件 12px 间距，查询按钮靠右 -->
    <div class="data-card filter-card">
      <el-input v-model="query.userId" placeholder="用户 ID（可选）" clearable style="width: 200px" />
      <el-select v-model="query.operationType" placeholder="操作类型" clearable style="width: 160px">
        <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        style="width: 280px"
      />
      <div class="filter-actions">
        <el-button type="primary" @click="onSearch">查询</el-button>
      </div>
    </div>

    <!-- 表格卡片：标题与条数 -->
    <div class="data-card table-card">
      <div class="card-head">
        <div class="card-heading">
          <span class="card-title">操作日志</span>
          <span class="card-sub">共 {{ total }} 条</span>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="userId" label="用户 ID" width="180" show-overflow-tooltip />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column label="操作类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="operationTagType((row as OperationLogVO).operationType)">
              {{ operationTypeLabels[(row as OperationLogVO).operationType] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operationDescription" label="操作描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="targetType" label="目标类型" width="120" />
        <el-table-column prop="targetId" label="目标 ID" width="180" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP 地址" width="140" />
        <el-table-column prop="createTime" label="操作时间" width="180" class-name="time-col" />
        <template #empty>
          <el-empty description="暂无操作日志" />
        </template>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="onSearch"
        @current-change="load"
      />
    </div>
  </div>
</template>

<style scoped>
/* 筛选卡片：控件 12px 间距，查询按钮靠右 */
.filter-card {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--ceams-space-3);
  padding: 16px 20px;
  margin-bottom: var(--ceams-space-4);
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: var(--ceams-space-2);
  margin-left: auto;
}

/* 表格卡片：20px 内边距 */
.table-card {
  padding: 20px;
}

.card-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--ceams-space-3);
  margin-bottom: var(--ceams-space-4);
}

.card-heading {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--ceams-space-2);
}

.card-title {
  font-size: var(--ceams-font-size-md);
  font-weight: 600;
  color: var(--ceams-text-primary);
}

.card-sub {
  font-size: var(--ceams-font-size-xs);
  color: var(--ceams-text-secondary);
}

/* 时间列：等宽数字、左对齐，避免逐行错位 */
:deep(.time-col .cell) {
  font-variant-numeric: tabular-nums;
  font-feature-settings: 'tnum';
  text-align: left;
}

/* 分页右对齐，距表格 16px */
.pagination {
  margin-top: var(--ceams-space-4);
  justify-content: flex-end;
}

/* 空态：居中灰色 + 40px 留白 */
:deep(.el-table__empty-block) {
  padding: 40px 0;
  min-height: 0;
}

:deep(.el-table__empty-text) {
  color: var(--ceams-text-secondary);
}
</style>
