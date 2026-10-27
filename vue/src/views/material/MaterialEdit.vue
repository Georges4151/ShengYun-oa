<template>
  <div class="page-container">
    <el-card v-loading="pageLoading">
      <template #header><div class="card-header"><span>编辑材料</span></div></template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width: 800px;">
        <el-form-item label="材料名称" prop="name"><el-input v-model="form.name" placeholder="请输入材料名称" /></el-form-item>
        <el-form-item label="规格型号" prop="specification"><el-input v-model="form.specification" placeholder="请输入规格型号" /></el-form-item>
        <el-form-item label="单位" prop="unit"><el-input v-model="form.unit" placeholder="请输入单位" /></el-form-item>
        <el-form-item label="数量" prop="quantity"><el-input-number v-model="form.quantity" :min="0" :precision="2" style="width: 100%;" /></el-form-item>
        <el-form-item label="单价" prop="unitPrice"><el-input-number v-model="form.unitPrice" :min="0" :precision="2" style="width: 100%;" /></el-form-item>
        <el-form-item label="关联项目" prop="projectId">
          <el-select v-model="form.projectId" placeholder="请选择项目" style="width: 100%;">
            <el-option v-for="item in projectList" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" /></el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">保存</el-button>
          <el-button @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMaterialDetail, updateMaterial } from '@/api/material'
import { getProjectList } from '@/api/project'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const loading = ref(false)
const pageLoading = ref(false)
const projectList = ref([])

const form = reactive({ id: null, name: '', specification: '', unit: '', quantity: 0, unitPrice: 0, projectId: null, remark: '' })
const rules = { name: [{ required: true, message: '请输入材料名称', trigger: 'blur' }] }

async function fetchProjectList() {
  try { const res = await getProjectList({ page: 1, pageSize: 100 }); projectList.value = res.list || res || [] } catch (error) { console.error(error) }
}

async function fetchData() {
  const id = route.params.id
  if (!id) return
  pageLoading.value = true
  try { const res = await getMaterialDetail(id); Object.assign(form, res) } catch (error) { ElMessage.error('获取材料信息失败') }
  finally { pageLoading.value = false }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try { await updateMaterial(form.id, form); ElMessage.success('保存成功'); router.push('/material/list') } catch (error) { ElMessage.error('保存失败') }
  finally { loading.value = false }
}

function handleCancel() { router.push('/material/list') }

onMounted(() => { fetchProjectList(); fetchData() })
</script>
