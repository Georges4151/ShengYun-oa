<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>角色管理</span>
          <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon>添加角色</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="角色名称" width="150" />
        <el-table-column prop="code" label="角色编码" width="150" />
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="primary" link @click="handlePermission(row)">权限</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '添加角色'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="角色名称" prop="name"><el-input v-model="form.name" placeholder="请输入角色名称" /></el-form-item>
        <el-form-item label="角色编码" prop="code"><el-input v-model="form.code" placeholder="请输入角色编码" /></el-form-item>
        <el-form-item label="描述" prop="description"><el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permissionVisible" title="权限配置" width="500px">
      <el-tree ref="treeRef" :data="permissionTree" show-checkbox node-key="id" :default-checked-keys="checkedKeys" :props="{ label: 'name', children: 'children' }" />
      <template #footer>
        <el-button @click="permissionVisible = false">取消</el-button>
        <el-button type="primary" :loading="permissionLoading" @click="handleSavePermission">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRoleList, createRole, updateRole, deleteRole, getRolePermissions, updateRolePermissions } from '@/api/user'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const submitLoading = ref(false)
const permissionVisible = ref(false)
const permissionLoading = ref(false)
const treeRef = ref(null)
const currentRoleId = ref(null)
const checkedKeys = ref([])

const formRef = ref(null)
const form = reactive({ id: null, name: '', code: '', description: '' })
const rules = { name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }] }

const permissionTree = ref([
  { id: 1, name: '项目管理', children: [{ id: 11, name: '项目列表' }, { id: 12, name: '添加项目' }, { id: 13, name: '编辑项目' }, { id: 14, name: '删除项目' }] },
  { id: 2, name: '账户管理', children: [{ id: 21, name: '账户列表' }, { id: 22, name: '添加账户' }, { id: 23, name: '编辑账户' }, { id: 24, name: '删除账户' }] },
  { id: 3, name: '票据管理', children: [{ id: 31, name: '票据列表' }, { id: 32, name: '上传票据' }, { id: 33, name: '验证票据' }, { id: 34, name: '审批票据' }] },
  { id: 4, name: '用户管理', children: [{ id: 41, name: '用户列表' }, { id: 42, name: '添加用户' }, { id: 43, name: '编辑用户' }, { id: 44, name: '删除用户' }] },
  { id: 5, name: '系统设置', children: [{ id: 51, name: '基础设置' }, { id: 52, name: '日志管理' }] }
])

async function fetchData() {
  loading.value = true
  try { const res = await getRoleList(); tableData.value = res || [] } catch (error) { console.error(error) }
  finally { loading.value = false }
}

function handleAdd() {
  Object.assign(form, { id: null, name: '', code: '', description: '' })
  dialogVisible.value = true
}

function handleEdit(row) {
  Object.assign(form, row)
  dialogVisible.value = true
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该角色？', '提示', { type: 'warning' })
    await deleteRole(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) { if (error !== 'cancel') ElMessage.error('删除失败') }
}

async function handlePermission(row) {
  currentRoleId.value = row.id
  try { const res = await getRolePermissions(row.id); checkedKeys.value = res || [] } catch (error) { checkedKeys.value = [] }
  permissionVisible.value = true
}

async function handleSavePermission() {
  const keys = treeRef.value.getCheckedKeys()
  permissionLoading.value = true
  try { await updateRolePermissions(currentRoleId.value, keys); ElMessage.success('保存成功'); permissionVisible.value = false } catch (error) { ElMessage.error('保存失败') }
  finally { permissionLoading.value = false }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (form.id) { await updateRole(form.id, form) } else { await createRole(form) }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    fetchData()
  } catch (error) { ElMessage.error('操作失败') }
  finally { submitLoading.value = false }
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
