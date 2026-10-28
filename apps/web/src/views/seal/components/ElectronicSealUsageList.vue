<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  ElButton,
  ElInput,
  ElPagination,
  ElTable,
  ElTableColumn
} from 'element-plus'
import { ceamsApi } from '@/services/http'
import type { ElectronicSealUsageVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'

defineOptions({ name: 'ElectronicSealUsageList' })

const query = reactive<{ sealId?: string; userId?: string }>({})
const { page, size, total, resetPage } = usePage()
const list = ref<ElectronicSealUsageVO[]>([])
const loading = ref(false)

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.seal.usagePage({
      page: page.value,
      size: size.value,
      sealId: query.sealId,
      userId: query.userId
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    list.value = []
    total.value = 0
    // eslint-disable-next-line no-console
    console.error(apiErrorMessage(err, '加载使用审计失败'))
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
      <el-input v-model="query.sealId" placeholder="印章 ID" clearable style="width: 200px" />
      <el-input v-model="query.userId" placeholder="用户 ID" clearable style="width: 200px" />
      <el-button type="primary" @click="onSearch">查询</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="sealId" label="印章 ID" width="180" show-overflow-tooltip />
      <el-table-column prop="sealName" label="印章名称" width="140" />
      <el-table-column prop="userName" label="使用人" width="110" />
      <el-table-column prop="purpose" label="用途" min-width="150" show-overflow-tooltip />
      <el-table-column prop="documentType" label="文件类型" width="110" />
      <el-table-column prop="documentPath" label="文件路径" min-width="160" show-overflow-tooltip />
      <el-table-column prop="usageTime" label="使用时间" width="180" />
      <el-table-column prop="ipAddress" label="IP 地址" width="140" />
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
