<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import {
  ElButton,
  ElDatePicker,
  ElDialog,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ceamsApi } from '@/services/http'
import type { ElectronicSealVO, SealAuthorizationVO, UserVO } from '@ceams/types'
import { apiErrorMessage } from '@/utils/error'

defineOptions({ name: 'SealAuthorizationDialog' })

const props = defineProps<{
  modelValue: boolean
  seal: ElectronicSealVO | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const list = ref<SealAuthorizationVO[]>([])
const loading = ref(false)
const users = ref<UserVO[]>([])
const userLoading = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  userId: '',
  startTime: '',
  endTime: '',
  remark: ''
})

const rules: FormRules<typeof form> = {
  userId: [{ required: true, message: '请选择被授权人', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择生效时间', trigger: 'change' }],
  endTime: [
    { required: true, message: '请选择失效时间', trigger: 'change' },
    {
      // 日期选择器 value-format 为可排序的 ISO 字符串，可直接按字符串比较
      validator: (_rule, value: string, callback) => {
        if (value && form.startTime && value <= form.startTime) {
          callback(new Error('失效时间必须晚于生效时间'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ]
}

/** 兼容后端 ISO8601（含 T）与空格分隔两种时间字符串 */
function toTimestamp(value: string): number {
  return new Date(value.replace(' ', 'T')).getTime()
}

interface AuthorizationStatus {
  label: string
  type: 'success' | 'warning' | 'info'
}

/** 按当前时间与启用标记计算授权状态 */
function authorizationStatus(row: SealAuthorizationVO): AuthorizationStatus {
  if (!row.enabled) return { label: '已停用', type: 'info' }
  const now = Date.now()
  if (now < toTimestamp(row.startTime)) return { label: '未开始', type: 'warning' }
  if (now > toTimestamp(row.endTime)) return { label: '已过期', type: 'info' }
  return { label: '生效中', type: 'success' }
}

async function loadList(): Promise<void> {
  if (!props.seal) return
  loading.value = true
  try {
    list.value = await ceamsApi.seal.listSealAuthorizations(props.seal.id)
  } catch (err) {
    list.value = []
    ElMessage.error(apiErrorMessage(err, '加载印章授权失败'))
  } finally {
    loading.value = false
  }
}

async function loadUsers(): Promise<void> {
  userLoading.value = true
  try {
    const res = await ceamsApi.iam.userPage({ page: 1, size: 100, status: 'ACTIVE' })
    users.value = res.records
  } catch {
    // 用户候选加载失败不阻断已有授权展示
  } finally {
    userLoading.value = false
  }
}

function resetForm(): void {
  Object.assign(form, { userId: '', startTime: '', endTime: '', remark: '' })
  formRef.value?.clearValidate()
}

async function submit(): Promise<void> {
  if (!props.seal || !formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (!form.endTime || !form.startTime || form.endTime <= form.startTime) {
    ElMessage.warning('失效时间必须晚于生效时间')
    return
  }

  submitting.value = true
  try {
    await ceamsApi.seal.createSealAuthorization({
      sealId: props.seal.id,
      userId: form.userId,
      startTime: form.startTime,
      endTime: form.endTime,
      remark: form.remark || null
    })
    ElMessage.success('授权已新增')
    resetForm()
    await loadList()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '新增授权失败'))
  } finally {
    submitting.value = false
  }
}

async function onRevoke(row: SealAuthorizationVO): Promise<void> {
  const confirmed = await ElMessageBox.confirm(
    `确定撤销对「${row.userName ?? row.userId}」的印章授权吗？`,
    '提示',
    { type: 'warning' }
  ).catch(() => false)
  if (!confirmed) return
  try {
    await ceamsApi.seal.revokeSealAuthorization(row.id)
    ElMessage.success('授权已撤销')
    await loadList()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '撤销授权失败'))
  }
}

watch(
  () => [props.modelValue, props.seal?.id] as const,
  ([value]) => {
    if (!value) return
    resetForm()
    void loadList()
    if (users.value.length === 0) void loadUsers()
  },
  { immediate: true }
)
</script>

<template>
  <el-dialog v-model="visible" title="印章授权" width="860px">
    <div class="seal-tip">当前印章：{{ seal?.sealName ?? '-' }}</div>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" class="auth-form">
      <el-form-item label="被授权人" prop="userId">
        <el-select
          v-model="form.userId"
          filterable
          clearable
          :loading="userLoading"
          placeholder="请选择用户"
          style="width: 100%"
        >
          <el-option
            v-for="item in users"
            :key="item.id"
            :label="`${item.name}（${item.username}）`"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="生效时间" prop="startTime">
        <el-date-picker
          v-model="form.startTime"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          placeholder="选择生效时间"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="失效时间" prop="endTime">
        <el-date-picker
          v-model="form.endTime"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          placeholder="选择失效时间"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" maxlength="255" placeholder="备注（可选）" />
      </el-form-item>
      <el-form-item class="auth-form-actions">
        <el-button type="primary" :loading="submitting" @click="submit">新增授权</el-button>
      </el-form-item>
    </el-form>

    <el-table v-if="loading || list.length > 0" v-loading="loading" :data="list" border size="small">
      <el-table-column label="被授权人" min-width="130">
        <template #default="{ row }">{{ (row as SealAuthorizationVO).userName ?? (row as SealAuthorizationVO).userId }}</template>
      </el-table-column>
      <el-table-column prop="startTime" label="生效时间" width="170" />
      <el-table-column prop="endTime" label="失效时间" width="170" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="authorizationStatus(row as SealAuthorizationVO).type">
            {{ authorizationStatus(row as SealAuthorizationVO).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ (row as SealAuthorizationVO).remark ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" size="small" @click="onRevoke(row as SealAuthorizationVO)">撤销</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty
      v-else
      description="暂无授权记录"
      :image-size="60"
    />

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.seal-tip {
  margin-bottom: 12px;
  color: #606266;
  font-size: 13px;
}
.auth-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 16px;
}
.auth-form-actions {
  grid-column: 1 / -1;
}
</style>
