<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  ElButton,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElEmpty,
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
import type { RoleVO, UserStatus, UserVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { toOptions, userStatusLabels } from '@/utils/labels'

defineOptions({ name: 'UserList' })

const statusOptions = toOptions(userStatusLabels)

const query = reactive<{ keyword?: string; status?: UserStatus }>({})
const { page, size, total, resetPage } = usePage()
const list = ref<UserVO[]>([])
const loading = ref(false)

// 角色来源（用于新建 / 编辑 / 分配角色）
const roleOptions = ref<RoleVO[]>([])

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<UserVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  username: '',
  name: '',
  password: '',
  email: '',
  phone: '',
  status: 'ACTIVE' as UserStatus,
  roleIds: [] as string[]
})

const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

// 分配角色弹窗
const assignVisible = ref(false)
const assignUserId = ref<string | null>(null)
const assignRoleIds = ref<string[]>([])

async function loadRoles(): Promise<void> {
  try {
    const res = await ceamsApi.iam.rolePage({ page: 1, size: 100 })
    roleOptions.value = res.records
  } catch {
    // 角色来源加载失败不阻断主流程
  }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.iam.userPage({
      page: page.value,
      size: size.value,
      keyword: query.keyword,
      status: query.status
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载用户失败'))
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
    username: '',
    name: '',
    password: '',
    email: '',
    phone: '',
    status: 'ACTIVE',
    roleIds: []
  })
  dialogVisible.value = true
  void loadRoles()
}

function openEdit(row: UserVO): void {
  editingId.value = row.id
  form.username = row.username
  form.name = row.name
  form.password = ''
  form.email = row.email ?? ''
  form.phone = row.phone ?? ''
  form.status = row.status
  form.roleIds = row.roles?.map((r) => r.id) ?? []
  dialogVisible.value = true
  void loadRoles()
}

function openDetail(row: UserVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.iam.userUpdate(editingId.value, {
        name: form.name,
        email: form.email || undefined,
        phone: form.phone || undefined,
        status: form.status
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.iam.userCreate({
        username: form.username,
        name: form.name,
        password: form.password || undefined,
        email: form.email || null,
        phone: form.phone || null,
        status: form.status,
        roleIds: form.roleIds
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存失败'))
  }
}

async function onRemove(row: UserVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除用户「${row.username}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.iam.userRemove(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '删除失败'))
  }
}

function openAssignRoles(row: UserVO): void {
  assignUserId.value = row.id
  assignRoleIds.value = row.roles?.map((r) => r.id) ?? []
  assignVisible.value = true
  void loadRoles()
}

async function submitAssignRoles(): Promise<void> {
  if (!assignUserId.value) return
  try {
    await ceamsApi.iam.userAssignRoles(assignUserId.value, { roleIds: assignRoleIds.value })
    ElMessage.success('角色分配成功')
    assignVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '角色分配失败'))
  }
}

async function onResetPassword(row: UserVO): Promise<void> {
  try {
    const { value } = await ElMessageBox.prompt('请输入新密码（至少 6 位）', '重置密码', {
      inputType: 'password',
      inputValidator: (v) => (v && v.length >= 6) || '密码至少 6 位'
    })
    await ceamsApi.iam.userResetPassword(row.id, { password: value })
    ElMessage.success('密码重置成功')
  } catch (err) {
    if (err === 'cancel' || err === 'close') return
    ElMessage.error(apiErrorMessage(err, '密码重置失败'))
  }
}

onMounted(() => {
  void load()
  void loadRoles()
})
</script>

<template>
  <div>
    <!-- 筛选卡片：控件 12px 间距，查询按钮靠右 -->
    <div class="data-card filter-card">
      <el-input v-model="query.keyword" placeholder="关键字（用户名/姓名）" clearable style="width: 220px" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <div class="filter-actions">
        <el-button type="primary" @click="onSearch">查询</el-button>
      </div>
    </div>

    <!-- 表格卡片：标题、条数与主操作，表格四周 20px -->
    <div class="data-card table-card">
      <div class="card-head">
        <div class="card-heading">
          <span class="card-title">用户列表</span>
          <span class="card-sub">共 {{ total }} 条</span>
        </div>
        <div class="card-actions">
          <el-button type="primary" @click="openCreate">新增用户</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" border>
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="(row as UserVO).status === 'ACTIVE' ? 'success' : 'info'">
              {{ userStatusLabels[(row as UserVO).status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.roles?.map((r: RoleVO) => r.name).join('、') || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row as UserVO)">详情</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row as UserVO)">编辑</el-button>
            <el-button link type="primary" size="small" @click="openAssignRoles(row as UserVO)">分配角色</el-button>
            <el-button link type="warning" size="small" @click="onResetPassword(row as UserVO)">重置密码</el-button>
            <el-button link type="danger" size="small" @click="onRemove(row as UserVO)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无用户数据" />
        </template>
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
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑用户' : '新增用户'" width="540px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!editingId" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item v-if="!editingId" label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="初始密码（可选）" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="邮箱（可选）" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="手机号（可选）" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple style="width: 100%" placeholder="请选择角色">
            <el-option v-for="r in roleOptions" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignVisible" title="分配角色" width="440px">
      <el-select v-model="assignRoleIds" multiple style="width: 100%" placeholder="请选择角色">
        <el-option v-for="r in roleOptions" :key="r.id" :label="r.name" :value="r.id" />
      </el-select>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssignRoles">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="用户详情" width="540px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="用户名">{{ detail.username }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detail.email ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detail.phone ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ userStatusLabels[detail.status] }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          {{ detail.roles?.map((r: RoleVO) => r.name).join('、') || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime ?? '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 筛选卡片：控件 12px 间距，查询按钮靠右 */
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
  align-items: center;
  gap: var(--ceams-space-2);
  margin-left: auto;
}

/* 表格卡片：20px 内边距 */
.table-card {
  padding: 20px;
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

.card-actions {
  display: flex;
  align-items: center;
  gap: var(--ceams-space-2);
}

/* 分页右对齐，距表格 16px */
.pagination {
  margin-top: var(--ceams-space-4);
  justify-content: flex-end;
}

/* 空态：居中灰色 + 40px 留白 */
:deep(.el-table__empty-block) {
  padding: 40px 0;
  min-height: 0;
}

:deep(.el-table__empty-text) {
  color: var(--ceams-text-secondary);
}
</style>
