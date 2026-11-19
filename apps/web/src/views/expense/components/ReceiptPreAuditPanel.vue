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
  <div class="data-card pre-audit">
    <div class="card-head">
      <div class="card-heading">
        <span class="card-title">AI 预审提示</span>
        <span class="card-sub">AI 提示仅供参考，不改变审批结果</span>
      </div>
      <el-button size="small" :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-alert v-if="error" type="warning" :closable="false" show-icon :title="error" />

    <div v-else-if="!loading && list.length === 0" class="pre-audit__empty">
      <el-empty description="暂无风险提示" :image-size="60" />
    </div>

    <el-table v-else v-loading="loading" :data="list" border size="small">
      <el-table-column prop="receiptNumber" label="票据编号" min-width="160" show-overflow-tooltip />
      <el-table-column prop="reporterName" label="报销人" width="110">
        <template #default="{ row }">{{ (row as ReceiptPreAuditVO).reporterName ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="金额" width="120" align="right">
        <template #default="{ row }">
          <span class="amount">{{ formatAmount((row as ReceiptPreAuditVO).amount) }}</span>
        </template>
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
      <template #empty>
        <el-empty description="暂无风险提示" :image-size="60" />
      </template>
    </el-table>
  </div>
</template>

<style scoped>
/* 预审卡片：与数据卡片保持一致的内边距与底部间距 */
.pre-audit {
  padding: 20px;
  margin-bottom: var(--ceams-space-4);
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

/* 空态：居中并保留上下 40px 留白 */
.pre-audit__empty {
  padding: 40px 0;
}

.pre-audit__warning {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 2px 0;
}

.pre-audit__warning-text {
  font-size: var(--ceams-font-size-xs);
  line-height: 1.6;
  color: var(--ceams-text-secondary);
}

.pre-audit__no-warning {
  font-size: var(--ceams-font-size-xs);
  color: var(--ceams-text-placeholder);
}

/* 金额：等宽数字 */
.amount {
  font-variant-numeric: tabular-nums;
  font-feature-settings: 'tnum';
  font-weight: 600;
  color: var(--ceams-text-primary);
}
</style>
