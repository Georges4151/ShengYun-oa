<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>电子章管理</span>
          <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon>添加印章</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="印章名称" min-width="150" />
        <el-table-column prop="type" label="印章类型" width="120" />
        <el-table-column prop="owner" label="保管人" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }"><el-tag :type="row.status === '正常' ? 'success' : 'danger'">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑印章' : '添加印章'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="印章名称" prop="name"><el-input v-model="form.name" placeholder="请输入印章名称" /></el-form-item>
        <el-form-item label="印章类型" prop="type">
          <el-select v-model="form.type" style="width: 100%;">
            <el-option label="公章" value="公章" />
            <el-option label="财务章" value="财务章" />
            <el-option label="合同章" value="合同章" />
            <el-option label="法人章" value="法人章" />
          </el-select>
        </el-form-item>
        <el-form-item label="保管人" prop="owner"><el-input v-model="form.owner" placeholder="请输入保管人" /></el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%;">
            <el-option label="正常" value="正常" />
            <el-option label="停用" value="停用" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSealList, createSeal, updateSeal, deleteSeal } from '@/api/electronicSeal'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, name: '', type: '公章', owner: '', status: '正常' })
const rules = { name: [{ required: true, message: '请输入印章名称', trigger: 'blur' }] }

async function fetchData() {
  loading.value = true
  try { const res = await getSealList(); tableData.value = res || [] } catch (error) { console.error(error) }
  finally { loading.value = false }
}

function handleAdd() {
  Object.assign(form, { id: null, name: '', type: '公章', owner: '', status: '正常' })
  dialogVisible.value = true
}

function handleEdit(row) {
  Object.assign(form, row)
  dialogVisible.value = true
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该印章？', '提示', { type: 'warning' })
    await deleteSeal(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) { if (error !== 'cancel') ElMessage.error('删除失败') }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (form.id) { await updateSeal(form.id, form) } else { await createSeal(form) }
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
