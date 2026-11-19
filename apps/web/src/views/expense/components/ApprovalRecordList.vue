<script setup lang="ts">
import { ref } from 'vue'
import {
  ElButton,
  ElEmpty,
  ElInput,
  ElPagination,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus'
import { ceamsApi } from '@/services/http'
import type { ApprovalRecordVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { approvalRecordStatusLabels } from '@/utils/labels'

defineOptions({ name: 'ApprovalRecordList' })

// ---------------- 状态标签配色（仅视觉映射，不改动状态值） ----------------
type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'

/** 审批记录状态 -> 标签颜色：待审批警告、通过成功、驳回危险 */
function approvalRecordTagType(status: string): TagType {
  switch (status) {
    case 'APPROVED':
      return 'success'
    case 'REJECTED':
      return 'danger'
    default:
      return 'warning'
  }
}

const receiptId = ref('')
const allRecords = ref<ApprovalRecordVO[]>([])
const list = ref<ApprovalRecordVO[]>([])
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
    const res = await ceamsApi.expense.receiptApprovals(id)
    allRecords.value = res
    total.value = res.length
    resetPage()
    applyPage()
  } catch (err) {
    allRecords.value = []
    total.value = 0
    list.value = []
    // eslint-disable-next-line no-console
    console.error(apiErrorMessage(err, '加载审批记录失败'))
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
    <!-- 筛选卡片：控件 12px 间距，查询按钮靠右 -->
    <div class="data-card filter-card">
      <el-input v-model="receiptId" placeholder="票据 ID" clearable style="width: 240px" @keyup.enter="onSearch" />
      <div class="filter-actions">
        <el-button type="primary" @click="onSearch">查询</el-button>
      </div>
    </div>

    <!-- 表格卡片：头部标题，表格四周留 20px -->
    <div class="data-card table-card">
      <div class="card-head">
        <div class="card-heading">
          <span class="card-title">审批记录</span>
          <span class="card-sub">共 {{ total }} 条</span>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="receiptId" label="票据 ID" width="180" />
        <el-table-column prop="approvalLevel" label="审批级别" width="100" />
        <el-table-column prop="approverRole" label="审批角色" width="140" />
        <el-table-column prop="approverName" label="审批人" width="120" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="approvalRecordTagType((row as ApprovalRecordVO).status)">
              {{ approvalRecordStatusLabels[(row as ApprovalRecordVO).status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="approvalComment" label="审批意见" min-width="180" show-overflow-tooltip />
        <el-table-column prop="approvalTime" label="审批时间" width="180" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <template #empty>
          <el-empty description="暂无审批记录" />
        </template>
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
  </div>
</template>

<style scoped>
/* 筛选卡片：控件 12px 间距，按钮组靠右 */
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
  gap: var(--ceams-space-2);
  align-items: center;
  margin-left: auto;
}

/* 表格卡片：头部标题，表格四周 20px 内边距 */
.table-card {
  padding: 20px;
}

.card-head {
  display: flex;
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

.pagination {
  margin-top: var(--ceams-space-4);
  justify-content: flex-end;
}
</style>
