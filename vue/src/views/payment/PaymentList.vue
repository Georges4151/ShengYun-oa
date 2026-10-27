<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>账户管理</span>
          <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon>添加账户</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="accountName" label="账户名称" min-width="150" />
        <el-table-column prop="accountNumber" label="账号" width="180" />
        <el-table-column prop="bankName" label="开户行" width="150" />
        <el-table-column prop="balance" label="余额" width="150">
          <template #default="{ row }"><span class="text-primary">{{ formatMoney(row.balance) }}</span></template>
        </el-table-column>
        <el-table-column prop="accountType" label="账户类型" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }"><el-tag :type="row.status === '正常' ? 'success' : 'danger'">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑账户' : '添加账户'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="账户名称" prop="accountName"><el-input v-model="form.accountName" placeholder="请输入账户名称" /></el-form-item>
        <el-form-item label="账号" prop="accountNumber"><el-input v-model="form.accountNumber" placeholder="请输入账号" /></el-form-item>
        <el-form-item label="开户行" prop="bankName"><el-input v-model="form.bankName" placeholder="请输入开户行" /></el-form-item>
        <el-form-item label="账户类型" prop="accountType">
          <el-select v-model="form.accountType" style="width: 100%;">
            <el-option label="银行账户" value="银行账户" />
            <el-option label="现金账户" value="现金账户" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="初始余额" prop="balance"><el-input-number v-model="form.balance" :min="0" :precision="2" style="width: 100%;" /></el-form-item>
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
import { getPaymentAccountList, createPaymentAccount, updatePaymentAccount, deletePaymentAccount } from '@/api/payment'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, accountName: '', accountNumber: '', bankName: '', accountType: '银行账户', balance: 0 })
const rules = { accountName: [{ required: true, message: '请输入账户名称', trigger: 'blur' }] }

async function fetchData() {
  loading.value = true
  try { const res = await getPaymentAccountList({}); tableData.value = res.list || res || [] } catch (error) { console.error(error) }
  finally { loading.value = false }
}

function handleAdd() {
  Object.assign(form, { id: null, accountName: '', accountNumber: '', bankName: '', accountType: '银行账户', balance: 0 })
  dialogVisible.value = true
}

function handleEdit(row) {
  Object.assign(form, row)
  dialogVisible.value = true
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该账户？', '提示', { type: 'warning' })
    await deletePaymentAccount(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) { if (error !== 'cancel') ElMessage.error('删除失败') }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (form.id) { await updatePaymentAccount(form.id, form) } else { await createPaymentAccount(form) }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    fetchData()
  } catch (error) { ElMessage.error('操作失败') }
  finally { submitLoading.value = false }
}

function formatMoney(value) {
  if (!value) return '¥0.00'
  return '¥' + Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.text-primary { color: #409eff; font-weight: bold; }
</style>
