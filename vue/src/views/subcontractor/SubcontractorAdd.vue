<template>
  <div class="page-container">
    <el-card>
      <template #header><div class="card-header"><span>添加分包商</span></div></template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width: 800px;">
        <el-form-item label="分包商名称" prop="name"><el-input v-model="form.name" placeholder="请输入分包商名称" /></el-form-item>
        <el-form-item label="联系人" prop="contactPerson"><el-input v-model="form.contactPerson" placeholder="请输入联系人" /></el-form-item>
        <el-form-item label="联系电话" prop="phone"><el-input v-model="form.phone" placeholder="请输入联系电话" /></el-form-item>
        <el-form-item label="地址" prop="address"><el-input v-model="form.address" placeholder="请输入地址" /></el-form-item>
        <el-form-item label="资质等级" prop="qualification"><el-input v-model="form.qualification" placeholder="请输入资质等级" /></el-form-item>
        <el-form-item label="银行账户" prop="bankAccount"><el-input v-model="form.bankAccount" placeholder="请输入银行账户" /></el-form-item>
        <el-form-item label="开户行" prop="bankName"><el-input v-model="form.bankName" placeholder="请输入开户行" /></el-form-item>
        <el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" /></el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">提交</el-button>
          <el-button @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createSubcontractor } from '@/api/subcontractor'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({ name: '', contactPerson: '', phone: '', address: '', qualification: '', bankAccount: '', bankName: '', remark: '' })
const rules = { name: [{ required: true, message: '请输入分包商名称', trigger: 'blur' }] }

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try { await createSubcontractor(form); ElMessage.success('添加成功'); router.push('/subcontractor/list') } catch (error) { ElMessage.error('添加失败') }
  finally { loading.value = false }
}

function handleCancel() { router.push('/subcontractor/list') }
</script>
