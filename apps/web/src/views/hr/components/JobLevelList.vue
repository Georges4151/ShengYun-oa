<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  ElButton,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElOption,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ceamsApi } from '@/services/http'
import type { HrJobLevelStatus, JobLevelCreateRequest, JobLevelUpdateRequest, JobLevelVO } from '@ceams/types'
import { apiErrorMessage } from '@/utils/error'
import { hrJobLevelStatusLabels, toOptions } from '@/utils/labels'

defineOptions({ name: 'JobLevelList' })

const statusOptions = toOptions(hrJobLevelStatusLabels)

const list = ref<JobLevelVO[]>([])
const loading = ref(false)

async function load(): Promise<void> {
  loading.value = true
  try {
    list.value = await ceamsApi.hr.jobLevelList()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载职级字典失败'))
  } finally {
    loading.value = false
  }
}

function statusTagType(status: HrJobLevelStatus): 'success' | 'info' {
  return status === 'ENABLED' ? 'success' : 'info'
}

// ---------- 新增 / 编辑 ----------
const dialogVisible = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  code: '',
  name: '',
  jobFamily: '',
  rankNo: undefined as number | undefined,
  sortOrder: undefined as number | undefined,
  status: 'ENABLED' as HrJobLevelStatus,
  remark: ''
})

const rules: FormRules<typeof form> = {
  code: [{ required: true, message: '请输入职级编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入职级名称', trigger: 'blur' }]
}

function resetForm(): void {
  Object.assign(form, {
    code: '',
    name: '',
    jobFamily: '',
    rankNo: undefined,
    sortOrder: undefined,
    status: 'ENABLED',
    remark: ''
  })
}

function openCreate(): void {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: JobLevelVO): void {
  editingId.value = row.id
  form.code = row.code
  form.name = row.name
  form.jobFamily = row.jobFamily ?? ''
  form.rankNo = row.rankNo ?? undefined
  form.sortOrder = row.sortOrder ?? undefined
  form.status = row.status
  form.remark = row.remark ?? ''
  dialogVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      const payload: JobLevelUpdateRequest = {
        name: form.name,
        jobFamily: form.jobFamily || undefined,
        rankNo: form.rankNo,
        sortOrder: form.sortOrder,
        status: form.status,
        remark: form.remark || undefined
      }
      await ceamsApi.hr.jobLevelUpdate(editingId.value, payload)
      ElMessage.success('更新成功')
    } else {
      const payload: JobLevelCreateRequest = {
        code: form.code,
        name: form.name,
        jobFamily: form.jobFamily || undefined,
        rankNo: form.rankNo,
        sortOrder: form.sortOrder,
        status: form.status,
        remark: form.remark || undefined
      }
      await ceamsApi.hr.jobLevelCreate(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存职级失败'))
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="list-page">
    <div class="data-card">
      <div class="card-head">
        <div class="card-heading">
          <span class="card-title">职级字典</span>
          <span class="card-sub">共 {{ list.length }} 条</span>
        </div>
        <div class="card-actions">
          <el-button type="primary" @click="openCreate">新增职级</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="code" label="编码" width="140" />
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column label="序列" min-width="120">
          <template #default="{ row }">{{ (row as JobLevelVO).jobFamily || '-' }}</template>
        </el-table-column>
        <el-table-column label="职级序号" width="100" align="right">
          <template #default="{ row }">
            {{ (row as JobLevelVO).rankNo ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column label="排序" width="90" align="right">
          <template #default="{ row }">
            {{ (row as JobLevelVO).sortOrder ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType((row as JobLevelVO).status)">
              {{ hrJobLevelStatusLabels[(row as JobLevelVO).status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="180">
          <template #default="{ row }">{{ (row as JobLevelVO).remark || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row as JobLevelVO)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑职级' : '新增职级'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" :disabled="!!editingId" placeholder="请输入职级编码" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入职级名称" />
        </el-form-item>
        <el-form-item label="序列">
          <el-input v-model="form.jobFamily" placeholder="序列（可选）" />
        </el-form-item>
        <el-form-item label="职级序号">
          <el-input-number v-model="form.rankNo" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option
              v-for="opt in statusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 表格卡片：20px 内边距 */
.data-card {
  padding: 20px;
}

/* 卡片头部：左侧标题与条数，右侧主操作 */
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ceams-space-3, 12px);
  margin-bottom: var(--ceams-space-4, 16px);
}

.card-heading {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--ceams-space-2, 8px);
}

.card-title {
  font-size: var(--ceams-font-size-md, 16px);
  font-weight: 600;
  color: var(--ceams-text-primary);
}

.card-sub {
  font-size: var(--ceams-font-size-xs, 12px);
  color: var(--ceams-text-secondary);
}

.card-actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: var(--ceams-space-2, 8px);
}

/* 空态居中灰色 */
:deep(.el-table__empty-block) {
  padding: 40px 0;
  min-height: 0;
}

:deep(.el-table__empty-text) {
  color: var(--ceams-text-secondary);
}

/* 弹窗：标题加粗，内容区 20px 24px，底部操作条靠右并带顶部细边框 */
:deep(.el-dialog__title) {
  font-weight: 600;
}

:deep(.el-dialog__body) {
  padding: 20px 24px;
}

:deep(.el-dialog__footer) {
  padding: 12px 24px 18px;
  border-top: 1px solid var(--ceams-border-divider);
  text-align: right;
}
</style>
