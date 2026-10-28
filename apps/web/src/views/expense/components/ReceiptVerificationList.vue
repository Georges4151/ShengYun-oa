<script setup lang="ts">
import { ref } from 'vue'
import {
  ElButton,
  ElInput,
  ElPagination,
  ElTable,
  ElTableColumn
} from 'element-plus'
import { ceamsApi } from '@/services/http'
import type { ReceiptVerificationVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { paymentMethodLabels } from '@/utils/labels'

defineOptions({ name: 'ReceiptVerificationList' })

const receiptId = ref('')
const allRecords = ref<ReceiptVerificationVO[]>([])
const list = ref<ReceiptVerificationVO[]>([])
const loading = ref(false)
const { page, size, total, resetPage } = usePage()

function applyPage(): void {
  const start = (page.value - 1) * size.value
  list.value = allRecords.value.slice(start, start + size.value)
}

async function load(): Promise<void> {
  const id = receiptId.value.trim()
  if (!id) {
    allRecords.value = []
    total.value = 0
    list.value = []
    return
  }
  loading.value = true
  try {
    const res = await ceamsApi.expense.receiptVerifications(id)
    allRecords.value = res
    total.value = res.length
    resetPage()
    applyPage()
  } catch (err) {
    allRecords.value = []
    total.value = 0
    list.value = []
    // eslint-disable-next-line no-console
    console.error(apiErrorMessage(err, '加载核验记录失败'))
  } finally {
    loading.value = false
  }
}

function onSearch(): void {
  resetPage()
  void load()
}

function onPageChange(): void {
  applyPage()
}

function onSizeChange(): void {
  resetPage()
  applyPage()
}
</script>

<template>
  <div>
    <div class="toolbar">
      <el-input v-model="receiptId" placeholder="票据 ID" clearable style="width: 240px" @keyup.enter="onSearch" />
      <el-button type="primary" @click="onSearch">查询</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="receiptId" label="票据 ID" width="180" />
      <el-table-column prop="paymentAccountId" label="付款账户 ID" width="180" />
      <el-table-column prop="amount" label="核验金额" width="120" />
      <el-table-column label="支付方式" width="110">
        <template #default="{ row }">
          {{ (row as ReceiptVerificationVO).paymentMethod ? paymentMethodLabels[(row as ReceiptVerificationVO).paymentMethod!] : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="verifiedBy" label="核验人 ID" width="180" />
      <el-table-column prop="verificationTime" label="核验时间" width="180" />
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="180" />
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      class="pagination"
      @size-change="onSizeChange"
      @current-change="onPageChange"
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
