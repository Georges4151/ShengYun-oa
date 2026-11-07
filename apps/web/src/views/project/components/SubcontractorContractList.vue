<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  ElAlert,
  ElButton,
  ElDatePicker,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElPagination,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ceamsApi } from '@/services/http'
import type {
  ContractRiskHintVO,
  ContractRiskItemVO,
  ContractRiskLevel,
  ContractStatus,
  SubcontractorContractVO
} from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { contractStatusLabels, toOptions } from '@/utils/labels'

defineOptions({ name: 'SubcontractorContractList' })

/** ElTag 支持的语义色 */
type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'

/** 综合风险等级 -> 标签文案与颜色（HIGH 红 / MEDIUM 橙 / LOW 灰 / NONE 绿） */
const riskLevelMeta: Record<ContractRiskLevel, { label: string; type: TagType }> = {
  HIGH: { label: '高风险', type: 'danger' },
  MEDIUM: { label: '中风险', type: 'warning' },
  LOW: { label: '低风险', type: 'info' },
  NONE: { label: '无风险', type: 'success' }
}

/** 单条风险等级 -> 标签文案与颜色 */
const riskItemLevelMeta: Record<ContractRiskItemVO['level'], { label: string; type: TagType }> = {
  HIGH: { label: '高', type: 'danger' },
  MEDIUM: { label: '中', type: 'warning' },
  LOW: { label: '低', type: 'info' }
}

const statusOptions = toOptions(contractStatusLabels)

const query = reactive<{ subcontractorId?: string; projectId?: string }>({})
const { page, size, total, resetPage } = usePage()
const list = ref<SubcontractorContractVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<SubcontractorContractVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<{
  subcontractorId: string
  projectId: string
  contractName: string
  contractNumber: string
  contractAmount: string
  startDate: string
  endDate: string
  status?: ContractStatus
  description: string
}>({
  subcontractorId: '',
  projectId: '',
  contractName: '',
  contractNumber: '',
  contractAmount: '',
  startDate: '',
  endDate: '',
  status: undefined,
  description: ''
})

const rules: FormRules<typeof form> = {
  subcontractorId: [{ required: true, message: '请输入分包商 ID', trigger: 'blur' }],
  contractName: [{ required: true, message: '请输入合同名称', trigger: 'blur' }],
  contractNumber: [{ required: true, message: '请输入合同编号', trigger: 'blur' }],
  contractAmount: [{ required: true, message: '请输入合同金额', trigger: 'blur' }]
}

// ---------- 合同要点与风险提示 ----------
const riskVisible = ref(false)
const riskLoading = ref(false)
const riskHint = ref<ContractRiskHintVO | null>(null)
const riskError = ref('')

async function openRiskHints(row: SubcontractorContractVO): Promise<void> {
  riskHint.value = null
  riskError.value = ''
  riskVisible.value = true
  riskLoading.value = true
  try {
    riskHint.value = await ceamsApi.ai.contractRiskHints(row.id)
  } catch (err) {
    riskError.value = apiErrorMessage(err, '风险提示加载失败')
    ElMessage.error(riskError.value)
  } finally {
    riskLoading.value = false
  }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.project.contractPage({
      page: page.value,
      size: size.value,
      subcontractorId: query.subcontractorId,
      projectId: query.projectId
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载分包合同失败'))
  } finally {
    loading.value = false
  }
}

function onSearch(): void {
  resetPage()
  void load()
}

function openCreate(): void {
  editingId.value = null
  Object.assign(form, {
    subcontractorId: '',
    projectId: '',
    contractName: '',
    contractNumber: '',
    contractAmount: '',
    startDate: '',
    endDate: '',
    status: undefined,
    description: ''
  })
  dialogVisible.value = true
}

function openEdit(row: SubcontractorContractVO): void {
  editingId.value = row.id
  form.subcontractorId = row.subcontractorId
  form.projectId = row.projectId ?? ''
  form.contractName = row.contractName
  form.contractNumber = row.contractNumber
  form.contractAmount = row.contractAmount
  form.startDate = row.startDate ?? ''
  form.endDate = row.endDate ?? ''
  form.status = row.status
  form.description = row.description ?? ''
  dialogVisible.value = true
}

function openDetail(row: SubcontractorContractVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.project.contractUpdate(editingId.value, {
        projectId: form.projectId || undefined,
        contractName: form.contractName,
        contractAmount: form.contractAmount,
        startDate: form.startDate || undefined,
        endDate: form.endDate || undefined,
        status: form.status,
        description: form.description || undefined
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.project.contractCreate({
        subcontractorId: form.subcontractorId,
        projectId: form.projectId || null,
        contractName: form.contractName,
        contractNumber: form.contractNumber,
        contractAmount: form.contractAmount,
        startDate: form.startDate || null,
        endDate: form.endDate || null,
        status: form.status,
        description: form.description || null
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存失败'))
  }
}

async function onRemove(row: SubcontractorContractVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除分包合同「${row.contractName}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.project.contractRemove(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '删除失败'))
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="toolbar">
      <el-input v-model="query.subcontractorId" placeholder="分包商 ID（可选）" clearable style="width: 200px" />
      <el-input v-model="query.projectId" placeholder="项目 ID（可选）" clearable style="width: 200px" />
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="openCreate">新增合同</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="contractName" label="合同名称" min-width="180" />
      <el-table-column prop="contractNumber" label="合同编号" width="160" />
      <el-table-column prop="subcontractorId" label="分包商 ID" width="180" />
      <el-table-column prop="projectId" label="项目 ID" width="180" />
      <el-table-column prop="contractAmount" label="合同金额" width="120" />
      <el-table-column prop="paidAmount" label="已付金额" width="120" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.status" size="small">{{ contractStatusLabels[(row as SubcontractorContractVO).status!] }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row as SubcontractorContractVO)">详情</el-button>
          <el-button link type="primary" size="small" @click="openRiskHints(row as SubcontractorContractVO)">风险提示</el-button>
          <el-button link type="primary" size="small" @click="openEdit(row as SubcontractorContractVO)">编辑</el-button>
          <el-button link type="danger" size="small" @click="onRemove(row as SubcontractorContractVO)">删除</el-button>
        </template>
      </el-table-column>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑分包合同' : '新增分包合同'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="分包商 ID" prop="subcontractorId">
          <el-input v-model="form.subcontractorId" :disabled="!!editingId" placeholder="请输入分包商 ID" />
        </el-form-item>
        <el-form-item label="项目 ID">
          <el-input v-model="form.projectId" placeholder="项目 ID（可选）" />
        </el-form-item>
        <el-form-item label="合同名称" prop="contractName">
          <el-input v-model="form.contractName" placeholder="请输入合同名称" />
        </el-form-item>
        <el-form-item label="合同编号" prop="contractNumber">
          <el-input v-model="form.contractNumber" :disabled="!!editingId" placeholder="请输入合同编号" />
        </el-form-item>
        <el-form-item label="合同金额" prop="contractAmount">
          <el-input v-model="form.contractAmount" placeholder="金额（字符串）" />
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="合同状态">
          <el-select v-model="form.status" clearable placeholder="请选择" style="width: 100%">
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="描述（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="分包合同详情" width="600px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="合同名称">{{ detail.contractName }}</el-descriptions-item>
        <el-descriptions-item label="合同编号">{{ detail.contractNumber }}</el-descriptions-item>
        <el-descriptions-item label="分包商 ID">{{ detail.subcontractorId }}</el-descriptions-item>
        <el-descriptions-item label="项目 ID">{{ detail.projectId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="合同金额">{{ detail.contractAmount }}</el-descriptions-item>
        <el-descriptions-item label="已付金额">{{ detail.paidAmount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ detail.status ? contractStatusLabels[detail.status] : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="开始日期">{{ detail.startDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="结束日期">{{ detail.endDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="文件名">{{ detail.fileName ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detail.description ?? '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="riskVisible" title="合同要点与风险提示" width="760px">
      <div v-loading="riskLoading" class="risk-body">
        <el-alert v-if="riskError" type="warning" :closable="false" show-icon :title="riskError" />
        <template v-if="riskHint">
          <div class="risk-section">
            <div class="risk-section__title">条款要点</div>
            <el-descriptions v-if="riskHint.clausePoints.length" :column="2" border size="small">
              <el-descriptions-item
                v-for="(point, index) in riskHint.clausePoints"
                :key="index"
                :label="point.label"
              >
                {{ point.value }}
              </el-descriptions-item>
            </el-descriptions>
            <div v-else class="risk-empty">暂无可提取的条款要点</div>
          </div>

          <div class="risk-section">
            <div class="risk-section__title">
              风险提示
              <el-tag
                v-if="riskHint.riskLevel !== 'NONE'"
                size="small"
                effect="plain"
                :type="riskLevelMeta[riskHint.riskLevel].type"
              >
                {{ riskLevelMeta[riskHint.riskLevel].label }}
              </el-tag>
            </div>
            <div v-if="riskHint.riskLevel === 'NONE' || riskHint.risks.length === 0" class="risk-empty">
              未发现明显风险
            </div>
            <el-table v-else :data="riskHint.risks" border size="small">
              <el-table-column label="等级" width="80">
                <template #default="{ row }">
                  <el-tag size="small" effect="plain" :type="riskItemLevelMeta[(row as ContractRiskItemVO).level].type">
                    {{ riskItemLevelMeta[(row as ContractRiskItemVO).level].label }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="title" label="风险" width="180" show-overflow-tooltip />
              <el-table-column prop="detail" label="说明" min-width="260" />
            </el-table>
          </div>

          <div v-if="riskHint.aiNote" class="risk-section">
            <div class="risk-section__title">AI 说明，请核实</div>
            <div class="risk-note">{{ riskHint.aiNote }}</div>
          </div>

          <el-alert class="risk-disclaimer" type="info" :closable="false" show-icon :title="riskHint.disclaimer" />
        </template>
      </div>
      <template #footer>
        <el-button @click="riskVisible = false">关闭</el-button>
      </template>
    </el-dialog>
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
.risk-body {
  min-height: 120px;
}
.risk-section {
  margin-bottom: 16px;
}
.risk-section__title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}
.risk-empty {
  color: #909399;
  font-size: 12px;
}
.risk-note {
  padding: 8px 12px;
  border-radius: 4px;
  background: #f5f7fa;
  color: #606266;
  font-size: 12px;
  line-height: 1.7;
  white-space: pre-wrap;
}
.risk-disclaimer {
  margin-top: 4px;
}
</style>
