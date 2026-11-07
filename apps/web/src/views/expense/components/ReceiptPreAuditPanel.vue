<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElAlert, ElButton, ElEmpty, ElTable, ElTableColumn, ElTag } from 'element-plus'
import type { PreAuditRiskLevel, PreAuditWarningLevel, ReceiptPreAuditVO } from '@ceams/types'
import { formatDate } from '@ceams/utils'
import { ceamsApi } from '@/services/http'
import { apiErrorMessage } from '@/utils/error'

defineOptions({ name: 'ReceiptPreAuditPanel' })

/** ElTag 支持的语义色 */
type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'

/** 综合风险等级 -> 标签文案与颜色（HIGH 红 / MEDIUM 橙 / LOW 蓝 / NONE 绿） */
const riskMeta: Record<PreAuditRiskLevel, { label: string; type: TagType }> = {
  HIGH: { label: '高风险', type: 'danger' },
  MEDIUM: { label: '中风险', type: 'warning' },
  LOW: { label: '低风险', type: 'primary' },
  NONE: { label: '无风险', type: 'success' }
}

/** 单条预警级别 -> 标签颜色 */
const warningMeta: Record<PreAuditWarningLevel, { label: string; type: TagType }> = {
  HIGH: { label: '高', type: 'danger' },
  MEDIUM: { label: '中', type: 'warning' }
}

const list = ref<ReceiptPreAuditVO[]>([])
const loading = ref(false)
const error = ref('')

function formatAmount(amount?: number | null): string {
  if (typeof amount !== 'number' || Number.isNaN(amount)) return '-'
  return amount.toFixed(2)
}

/** 预审提示加载失败只降级提示，不阻断票据列表 */
async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    list.value = await ceamsApi.ai.preAuditList({ limit: 20 })
  } catch (err) {
    list.value = []
    error.value = apiErrorMessage(err, 'AI 预审提示加载失败，不影响票据列表')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="pre-audit">
    <div class="pre-audit__header">
      <div class="pre-audit__heading">
        <span class="pre-audit__title">AI 预审提示</span>
        <span class="pre-audit__note">AI 提示仅供参考，不改变审批结果</span>
      </div>
      <el-button size="small" :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-alert v-if="error" type="warning" :closable="false" show-icon :title="error" />

    <el-empty v-else-if="!loading && list.length === 0" description="暂无风险提示" :image-size="60" />

    <el-table v-else v-loading="loading" :data="list" border size="small">
      <el-table-column prop="receiptNumber" label="票据编号" min-width="160" show-overflow-tooltip />
      <el-table-column prop="reporterName" label="报销人" width="110">
        <template #default="{ row }">{{ (row as ReceiptPreAuditVO).reporterName ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="金额" width="110">
        <template #default="{ row }">{{ formatAmount((row as ReceiptPreAuditVO).amount) }}</template>
      </el-table-column>
      <el-table-column label="票据日期" width="110">
        <template #default="{ row }">
          {{ formatDate((row as ReceiptPreAuditVO).receiptDate) }}
        </template>
      </el-table-column>
      <el-table-column label="风险等级" width="100">
        <template #default="{ row }">
          <el-tag size="small" effect="plain" :type="riskMeta[(row as ReceiptPreAuditVO).riskLevel].type">
            {{ riskMeta[(row as ReceiptPreAuditVO).riskLevel].label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提示" min-width="320">
        <template #default="{ row }">
          <div v-if="(row as ReceiptPreAuditVO).warnings.length === 0" class="pre-audit__no-warning">-</div>
          <div v-for="(warning, index) in (row as ReceiptPreAuditVO).warnings" :key="index" class="pre-audit__warning">
            <el-tag size="small" effect="plain" :type="warningMeta[warning.level].type">
              {{ warningMeta[warning.level].label }}
            </el-tag>
            <span class="pre-audit__warning-text">{{ warning.message }}</span>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.pre-audit {
  margin-bottom: 16px;
  padding: 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}
.pre-audit__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.pre-audit__heading {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 8px;
}
.pre-audit__title {
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}
.pre-audit__note {
  color: #909399;
  font-size: 12px;
}
.pre-audit__warning {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 2px 0;
}
.pre-audit__warning-text {
  color: #606266;
  font-size: 12px;
  line-height: 1.6;
}
.pre-audit__no-warning {
  color: #909399;
  font-size: 12px;
}
</style>
