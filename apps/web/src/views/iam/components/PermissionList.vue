<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ElButton,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElSelect,
  ElTag,
  ElTree
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ApiError } from '@ceams/api-client'
import { ceamsApi } from '@/services/http'
import { ERROR_CODES } from '@ceams/types'
import type { PermissionCreatePayload, PermissionType, PermissionVO } from '@ceams/types'
import { permissionTypeLabels, toOptions } from '@/utils/labels'
import { apiErrorMessage } from '@/utils/error'

defineOptions({ name: 'PermissionList' })

const treeProps = { label: 'name', children: 'children' }
const typeOptions = toOptions(permissionTypeLabels)
/** 权限码契约：2-100 位字母/数字/冒号/下划线/短横线 */
const CODE_PATTERN = /^[A-Za-z0-9:_-]{2,100}$/

const tree = ref<PermissionVO[]>([])
const mine = ref<string[]>([])
const loading = ref(false)

/** 扁平化后的父权限下拉项（label 已按层级缩进，便于区分父子关系） */
interface ParentOption {
  id: string
  label: string
}

interface PermissionForm {
  /** 空字符串表示作为根节点，提交时统一转为 null */
  parentId: string
  code: string
  name: string
  type: PermissionType
  path: string
  icon: string
  sortOrder: number
}

const dialogVisible = ref(false)
const editingId = ref<string | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<PermissionForm>({
  parentId: '',
  code: '',
  name: '',
  type: 'MENU',
  path: '',
  icon: '',
  sortOrder: 0
})

const rules: FormRules<PermissionForm> = {
  code: [
    { required: true, message: '请输入权限码', trigger: 'blur' },
    { pattern: CODE_PATTERN, message: '权限码需为 2-100 位字母、数字、冒号、下划线或短横线', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入权限名称', trigger: 'blur' },
    { max: 100, message: '名称不能超过 100 个字符', trigger: 'blur' }
  ],
  type: [{ required: true, message: '请选择权限类型', trigger: 'change' }],
  path: [{ max: 255, message: '路由路径不能超过 255 个字符', trigger: 'blur' }],
  icon: [{ max: 100, message: '图标不能超过 100 个字符', trigger: 'blur' }],
  sortOrder: [
    {
      validator: (_rule: unknown, value: unknown, callback: (error?: string | Error) => void) => {
        if (typeof value === 'number' && Number.isInteger(value)) callback()
        else callback(new Error('排序值必须为整数'))
      },
      trigger: 'change'
    }
  ]
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const [treeData, mineCodes] = await Promise.all([ceamsApi.iam.permissionTree(), ceamsApi.iam.permissionMine()])
    tree.value = treeData
    mine.value = mineCodes
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载权限失败'))
  } finally {
    loading.value = false
  }
}

function flattenPermissions(nodes: PermissionVO[], depth = 0, result: ParentOption[] = []): ParentOption[] {
  for (const node of nodes) {
    result.push({ id: node.id, label: `${'　'.repeat(depth)}${node.name}（${node.code}）` })
    if (node.children?.length) flattenPermissions(node.children, depth + 1, result)
  }
  return result
}

function collectSubtreeIds(node: PermissionVO, ids: Set<string> = new Set<string>()): Set<string> {
  ids.add(node.id)
  for (const child of node.children ?? []) collectSubtreeIds(child, ids)
  return ids
}

function findPermission(nodes: PermissionVO[], id: string): PermissionVO | null {
  for (const node of nodes) {
    if (node.id === id) return node
    if (node.children?.length) {
      const found = findPermission(node.children, id)
      if (found) return found
    }
  }
  return null
}

/** 编辑时排除自身及其后代，避免把节点挂到自己下面形成环 */
const parentOptions = computed<ParentOption[]>(() => {
  const all = flattenPermissions(tree.value)
  if (!editingId.value) return all
  const current = findPermission(tree.value, editingId.value)
  if (!current) return all
  const excluded = collectSubtreeIds(current)
  return all.filter((item) => !excluded.has(item.id))
})

function openCreate(): void {
  editingId.value = null
  Object.assign(form, {
    parentId: '',
    code: '',
    name: '',
    type: 'MENU' as PermissionType,
    path: '',
    icon: '',
    sortOrder: 0
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

function openEdit(row: PermissionVO): void {
  editingId.value = row.id
  Object.assign(form, {
    parentId: row.parentId ?? '',
    code: row.code,
    name: row.name,
    type: row.type,
    path: row.path ?? '',
    icon: row.icon ?? '',
    sortOrder: row.sort ?? 0
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

function resetForm(): void {
  editingId.value = null
  form.parentId = ''
  form.code = ''
  form.name = ''
  form.type = 'MENU'
  form.path = ''
  form.icon = ''
  form.sortOrder = 0
  formRef.value?.clearValidate()
}

async function submit(): Promise<void> {
  if (!formRef.value || submitting.value) return
  // 去除首尾空白后再校验，避免用户无意的空格导致正则或长度校验失败
  form.code = form.code.trim()
  form.name = form.name.trim()
  form.path = form.path.trim()
  form.icon = form.icon.trim()

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    // 契约约定：根节点统一提交 parentId = null；非 MENU 不提交路由路径
    const payload: PermissionCreatePayload = {
      code: form.code,
      name: form.name,
      type: form.type,
      parentId: form.parentId || null,
      path: form.type === 'MENU' ? form.path || null : null,
      icon: form.icon || null,
      sortOrder: form.sortOrder
    }
    if (editingId.value) {
      await ceamsApi.iam.permissionUpdate(editingId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.iam.permissionCreate(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    resetForm()
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, editingId.value ? '更新失败' : '创建失败'))
  } finally {
    submitting.value = false
  }
}

async function onRemove(row: PermissionVO): Promise<void> {
  if (row.children?.length) {
    ElMessage.warning('该权限存在子节点，请先删除子权限')
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除权限「${row.name}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.iam.permissionRemove(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (err) {
    // 后端以状态冲突表示「存在子节点」，转为友好提示而非当作异常崩溃
    if (err instanceof ApiError && err.message === ERROR_CODES.BIZ_STATE_CONFLICT) {
      ElMessage.warning('该权限存在子权限，请先删除子权限后再试')
      return
    }
    ElMessage.error(apiErrorMessage(err, '删除失败'))
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="permission-page-wrap">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">新建权限</el-button>
    </div>

    <div class="permission-page">
      <div class="tree-pane">
        <h3>权限树</h3>
        <el-tree :data="tree" :props="treeProps" node-key="id" default-expand-all>
          <template #default="{ data }">
            <span class="perm-node">
              <span class="perm-main">
                <span class="perm-name">{{ (data as PermissionVO).name }}</span>
                <el-tag size="small" type="info">{{ permissionTypeLabels[(data as PermissionVO).type] }}</el-tag>
                <span class="perm-code">{{ (data as PermissionVO).code }}</span>
              </span>
              <span class="perm-actions">
                <el-button link type="primary" size="small" @click.stop="openEdit(data as PermissionVO)">编辑</el-button>
                <el-button link type="danger" size="small" @click.stop="onRemove(data as PermissionVO)">删除</el-button>
              </span>
            </span>
          </template>
        </el-tree>
        <div v-if="loading" class="empty">加载中…</div>
        <div v-else-if="tree.length === 0" class="empty">暂无权限数据</div>
      </div>

      <div class="mine-pane">
        <h3>我的权限码（{{ mine.length }}）</h3>
        <div class="tags">
          <el-tag v-for="code in mine" :key="code" class="tag">{{ code }}</el-tag>
          <span v-if="mine.length === 0" class="empty">暂无权限码</span>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑权限' : '新建权限'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="父权限" prop="parentId">
          <el-select v-model="form.parentId" placeholder="无（作为根节点）" style="width: 100%">
            <el-option label="无（作为根节点）" value="" />
            <el-option v-for="opt in parentOptions" :key="opt.id" :label="opt.label" :value="opt.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="权限码" prop="code">
          <el-input v-model="form.code" maxlength="100" show-word-limit placeholder="如：system:user:create" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" maxlength="100" show-word-limit placeholder="请输入权限名称" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.type === 'MENU'" label="路由路径" prop="path">
          <el-input v-model="form.path" maxlength="255" show-word-limit placeholder="如：/iam/permissions" />
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-input v-model="form.icon" maxlength="100" show-word-limit placeholder="图标（可选）" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number
            v-model="form.sortOrder"
            :precision="0"
            :step="1"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="submitting" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}
.permission-page {
  display: flex;
  gap: 24px;
}
.tree-pane {
  flex: 1;
  min-width: 0;
}
.mine-pane {
  width: 360px;
  flex-shrink: 0;
}
h3 {
  margin: 0 0 12px;
  font-size: 14px;
}
.perm-node {
  display: flex;
  flex: 1;
  min-width: 0;
  align-items: center;
  gap: 8px;
}
.perm-main {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}
.perm-name {
  font-size: 14px;
}
.perm-code {
  font-size: 12px;
  color: #9ca3af;
}
.perm-actions {
  margin-left: auto;
  flex-shrink: 0;
}
.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.tag {
  font-family: monospace;
}
.empty {
  color: #9ca3af;
  font-size: 13px;
}
</style>
