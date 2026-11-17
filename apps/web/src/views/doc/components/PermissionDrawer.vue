<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import {
  ElButton,
  ElCheckbox,
  ElCheckboxGroup,
  ElDrawer,
  ElForm,
  ElFormItem,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag,
  ElTreeSelect
} from 'element-plus'
import { ceamsApi } from '@/services/http'
import type {
  DeptVO,
  DocAction,
  DocPermissionVO,
  DocSubjectType,
  Id,
  PermissionGrantRequest,
  RoleVO,
  UserVO
} from '@ceams/types'
import { apiErrorMessage } from '@/utils/error'
import { docActionLabels, docSubjectTypeLabels } from '@/utils/labels'

defineOptions({ name: 'PermissionDrawer' })

const props = defineProps<{
  modelValue: boolean
  fileId: Id | null
  /** 文件名（仅展示） */
  fileName: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const ACTION_OPTIONS: DocAction[] = ['PREVIEW', 'DOWNLOAD', 'EDIT', 'DELETE']

// ---------- 主体候选 ----------
const users = ref<UserVO[]>([])
const roles = ref<RoleVO[]>([])
const depts = ref<DeptVO[]>([])
const userLoading = ref(false)

const form = reactive<{ subjectType: DocSubjectType; subjectId: Id | '' }>({
  subjectType: 'USER',
  subjectId: ''
})
/** 复选框组按 Element Plus 的 string[] 收口，提交时再断言为 DocAction[]。 */
const actions = ref<string[]>(['PREVIEW', 'DOWNLOAD'])

async function searchUsers(keyword: string): Promise<void> {
  userLoading.value = true
  try {
    const res = await ceamsApi.iam.userPage({
      page: 1,
      size: 20,
      keyword: keyword.trim() || undefined
    })
    users.value = res.records
  } catch {
    // 远程搜索失败不阻断授权表单
  } finally {
    userLoading.value = false
  }
}

async function loadSubjectOptions(): Promise<void> {
  try {
    const [userRes, roleRes, deptRes] = await Promise.all([
      ceamsApi.iam.userPage({ page: 1, size: 20 }),
      ceamsApi.iam.rolePage({ page: 1, size: 100 }),
      ceamsApi.iam.deptTree()
    ])
    users.value = userRes.records
    roles.value = roleRes.records
    depts.value = deptRes
  } catch {
    // 候选加载失败时仍可提交（手动选择已加载的数据）
  }
}

/** 部门树扁平化为 ID -> 名称，用于授权列表展示。 */
const deptNameMap = computed(() => {
  const map = new Map<Id, string>()
  const walk = (nodes: DeptVO[]): void => {
    nodes.forEach((node) => {
      map.set(node.id, node.name)
      if (node.children && node.children.length > 0) walk(node.children)
    })
  }
  walk(depts.value)
  return map
})

const roleNameMap = computed(() => {
  const map = new Map<Id, string>()
  roles.value.forEach((item) => map.set(item.id, item.name))
  return map
})
const userNameMap = computed(() => {
  const map = new Map<Id, string>()
  users.value.forEach((item) => map.set(item.id, item.name))
  return map
})

watch(
  () => form.subjectType,
  () => {
    form.subjectId = ''
  }
)

function subjectLabel(item: DocPermissionVO): string {
  const prefix = docSubjectTypeLabels[item.subjectType] ?? item.subjectType
  if (item.subjectType === 'DEPT') return `${prefix}：${deptNameMap.value.get(item.subjectId) ?? item.subjectId}`
  if (item.subjectType === 'ROLE') return `${prefix}：${roleNameMap.value.get(item.subjectId) ?? item.subjectId}`
  return `${prefix}：${userNameMap.value.get(item.subjectId) ?? item.subjectId}`
}

// ---------- 已有授权 ----------
const permissions = ref<DocPermissionVO[]>([])
const listLoading = ref(false)
const submitting = ref(false)

async function loadPermissions(): Promise<void> {
  if (!props.fileId) return
  listLoading.value = true
  try {
    permissions.value = await ceamsApi.doc.permissions.list(props.fileId)
  } catch (err) {
    permissions.value = []
    ElMessage.error(apiErrorMessage(err, '加载授权列表失败'))
  } finally {
    listLoading.value = false
  }
}

watch(
  () => [props.modelValue, props.fileId] as const,
  ([value]) => {
    if (!value) return
    form.subjectId = ''
    actions.value = ['PREVIEW', 'DOWNLOAD']
    if (users.value.length === 0 || depts.value.length === 0 || roles.value.length === 0) {
      void loadSubjectOptions()
    }
    void loadPermissions()
  },
  { immediate: true }
)

async function submitGrant(): Promise<void> {
  if (!props.fileId) return
  if (!form.subjectId) {
    ElMessage.warning('请选择授权主体')
    return
  }
  if (actions.value.length === 0) {
    ElMessage.warning('请至少选择一个动作')
    return
  }
  const body: PermissionGrantRequest = {
    subjectType: form.subjectType,
    subjectId: form.subjectId,
    actions: [...actions.value] as DocAction[]
  }
  submitting.value = true
  try {
    await ceamsApi.doc.permissions.grant(props.fileId, body)
    ElMessage.success('授权已保存')
    await loadPermissions()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存授权失败'))
  } finally {
    submitting.value = false
  }
}

async function onRevoke(item: DocPermissionVO): Promise<void> {
  if (!props.fileId) return
  const confirmed = await ElMessageBox.confirm(
    `确认移除「${subjectLabel(item)}」的授权？`,
    '移除授权',
    { type: 'warning' }
  ).catch(() => false)
  if (!confirmed) return
  try {
    await ceamsApi.doc.permissions.revoke(props.fileId, item.id)
    ElMessage.success('授权已移除')
    await loadPermissions()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '移除授权失败'))
  }
}
</script>

<template>
  <el-drawer v-model="visible" title="文档授权" size="560px">
    <div class="file-tip">文件：{{ fileName || fileId }}</div>

    <el-form :model="form" label-width="90px">
      <el-form-item label="主体类型" required>
        <el-select v-model="form.subjectType" style="width: 100%">
          <el-option
            v-for="(label, value) in docSubjectTypeLabels"
            :key="value"
            :label="label"
            :value="value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="授权主体" required>
        <el-select
          v-if="form.subjectType === 'USER'"
          v-model="form.subjectId"
          filterable
          remote
          clearable
          :remote-method="searchUsers"
          :loading="userLoading"
          placeholder="输入姓名搜索用户"
          style="width: 100%"
        >
          <el-option
            v-for="item in users"
            :key="item.id"
            :label="`${item.name}（${item.username}）`"
            :value="item.id"
          />
        </el-select>
        <el-select
          v-else-if="form.subjectType === 'ROLE'"
          v-model="form.subjectId"
          filterable
          clearable
          placeholder="请选择角色"
          style="width: 100%"
        >
          <el-option
            v-for="item in roles"
            :key="item.id"
            :label="`${item.name}（${item.code}）`"
            :value="item.id"
          />
        </el-select>
        <el-tree-select
          v-else
          v-model="form.subjectId"
          :data="depts"
          :props="{ label: 'name', children: 'children' }"
          node-key="id"
          check-strictly
          clearable
          placeholder="请选择部门"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="授权动作" required>
        <el-checkbox-group v-model="actions">
          <el-checkbox v-for="action in ACTION_OPTIONS" :key="action" :value="action">
            {{ docActionLabels[action] }}
          </el-checkbox>
        </el-checkbox-group>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="submitGrant">保存授权</el-button>
      </el-form-item>
    </el-form>

    <div class="list-title">已有授权</div>
    <el-table v-loading="listLoading" :data="permissions" border size="small">
      <el-table-column label="主体" min-width="160">
        <template #default="{ row }">{{ subjectLabel(row as DocPermissionVO) }}</template>
      </el-table-column>
      <el-table-column label="动作" min-width="180">
        <template #default="{ row }">
          <el-tag
            v-for="action in (row as DocPermissionVO).actions"
            :key="action"
            size="small"
            class="action-tag"
          >
            {{ docActionLabels[action as DocAction] ?? action }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" size="small" @click="onRevoke(row as DocPermissionVO)">
            移除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-drawer>
</template>

<style scoped>
.file-tip {
  margin-bottom: 12px;
  color: #606266;
  font-size: 13px;
}
.list-title {
  margin: 8px 0 12px;
  font-weight: 600;
  color: #303133;
}
.action-tag {
  margin-right: 4px;
}
</style>
