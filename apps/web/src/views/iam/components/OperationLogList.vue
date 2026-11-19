<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  ElButton,
  ElDatePicker,
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
    <div class="toolbar">
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
      <el-button type="primary" @click="onSearch">查询</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="userId" label="用户 ID" width="180" />
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column label="操作类型" width="120">
        <template #default="{ row }">
          <el-tag size="small">{{ operationTypeLabels[(row as OperationLogVO).operationType] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="operationDescription" label="操作描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="targetType" label="目标类型" width="120" />
      <el-table-column prop="targetId" label="目标 ID" width="180" />
      <el-table-column prop="ipAddress" label="IP 地址" width="140" />
      <el-table-column prop="createTime" label="操作时间" width="180" />
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
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
