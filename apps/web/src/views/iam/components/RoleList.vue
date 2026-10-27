<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  ElButton,
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
  ElTag,
  ElTree
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ceamsApi } from '@/services/http'
import type { PermissionVO, RoleStatus, RoleVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { toOptions, roleStatusLabels } from '@/utils/labels'

defineOptions({ name: 'RoleList' })

const statusOptions = toOptions(roleStatusLabels)
const treeProps = { label: 'name', children: 'children' }

const query = reactive<{ keyword?: string }>({})
const { page, size, total, resetPage } = usePage()
const list = ref<RoleVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<RoleVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  code: '',
  name: '',
  description: '',
  status: 'ACTIVE' as RoleStatus
})

const rules: FormRules<typeof form> = {
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

// 赋权弹窗
const assignVisible = ref(false)
const assignRoleId = ref<string | null>(null)
const permissionTreeData = ref<PermissionVO[]>([])
const treeRef = ref<InstanceType<typeof ElTree> | null>(null)

async function loadPermissionTree(): Promise<void> {
  try {
    permissionTreeData.value = await ceamsApi.iam.permissionTree()
  } catch {
    // 权限树加载失败不阻断主流程
  }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.iam.rolePage({
      page: page.value,
      size: size.value,
      keyword: query.keyword
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载角色失败'))
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
  Object.assign(form, { code: '', name: '', description: '', status: 'ACTIVE' })
  dialogVisible.value = true
}

function openEdit(row: RoleVO): void {
  editingId.value = row.id
  form.code = row.code
  form.name = row.name
  form.description = row.description ?? ''
  form.status = row.status
  dialogVisible.value = true
}

function openDetail(row: RoleVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.iam.roleUpdate(editingId.value, {
        name: form.name,
        description: form.description || undefined,
        status: form.status
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.iam.roleCreate({
        code: form.code,
        name: form.name,
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

async function onRemove(row: RoleVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除角色「${row.name}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.iam.roleRemove(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '删除失败'))
  }
}

function openAssignPermissions(row: RoleVO): void {
  assignRoleId.value = row.id
  assignVisible.value = true
  void loadPermissionTree()
}

async function submitAssignPermissions(): Promise<void> {
  if (!assignRoleId.value) return
  const keys = treeRef.value?.getCheckedKeys() ?? []
  const permissionIds = keys.map((k) => String(k))
  try {
    await ceamsApi.iam.roleAssignPermissions(assignRoleId.value, { permissionIds })
    ElMessage.success('赋权成功')
    assignVisible.value = false
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '赋权失败'))
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="关键字（编码/名称）" clearable style="width: 220px" />
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="openCreate">新增角色</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="code" label="编码" width="160" />
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ roleStatusLabels[(row as RoleVO).status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row as RoleVO)">详情</el-button>
          <el-button link type="primary" size="small" @click="openEdit(row as RoleVO)">编辑</el-button>
          <el-button link type="primary" size="small" @click="openAssignPermissions(row as RoleVO)">赋权</el-button>
          <el-button link type="danger" size="small" @click="onRemove(row as RoleVO)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑角色' : '新增角色'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" :disabled="!!editingId" placeholder="如：ADMIN" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
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

    <el-dialog v-model="assignVisible" title="分配权限" width="480px">
      <el-tree
        ref="treeRef"
        :data="permissionTreeData"
        :props="treeProps"
        node-key="id"
        show-checkbox
        default-expand-all
      />
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssignPermissions">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="角色详情" width="500px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="编码">{{ detail.code }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ roleStatusLabels[detail.status] }}</el-descriptions-item>
        <el-descriptions-item label="描述">{{ detail.description ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime ?? '-' }}</el-descriptions-item>
      </el-descriptions>
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
</style>
