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
  ElTag
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ceamsApi } from '@/services/http'
import type { ElectronicSealVO, SealStatus, StorageType } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { sealStatusLabels, storageTypeLabels, toOptions } from '@/utils/labels'

defineOptions({ name: 'ElectronicSealList' })

const statusOptions = toOptions(sealStatusLabels)
const storageTypeOptions = toOptions(storageTypeLabels)

const query = reactive<{ name?: string; status?: SealStatus; storageType?: StorageType }>({})
const { page, size, total, resetPage } = usePage()
const list = ref<ElectronicSealVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<ElectronicSealVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  sealName: '',
  sealType: '',
  status: 'ACTIVE' as SealStatus,
  storageType: '' as StorageType | '',
  sealCode: '',
  description: ''
})

const rules: FormRules<typeof form> = {
  sealName: [{ required: true, message: '请输入印章名称', trigger: 'blur' }],
  sealType: [{ required: true, message: '请输入印章类型', trigger: 'blur' }]
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.seal.sealPage({
      page: page.value,
      size: size.value,
      name: query.name,
      status: query.status,
      storageType: query.storageType
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载电子印章失败'))
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
    sealName: '',
    sealType: '',
    status: 'ACTIVE',
    storageType: '',
    sealCode: '',
    description: ''
  })
  dialogVisible.value = true
}

function openEdit(row: ElectronicSealVO): void {
  editingId.value = row.id
  form.sealName = row.sealName
  form.sealType = row.sealType
  form.status = row.status
  form.storageType = row.storageType ?? ''
  form.sealCode = row.sealCode ?? ''
  form.description = row.description ?? ''
  dialogVisible.value = true
}

function openDetail(row: ElectronicSealVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.seal.sealUpdate(editingId.value, {
        sealName: form.sealName,
        sealType: form.sealType,
        sealCode: form.sealCode || null,
        description: form.description || null,
        status: form.status,
        storageType: form.storageType || undefined
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.seal.sealCreate({
        sealName: form.sealName,
        sealType: form.sealType,
        sealCode: form.sealCode || null,
        description: form.description || null,
        storageType: form.storageType || undefined
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存失败'))
  }
}

async function onRemove(row: ElectronicSealVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除电子印章「${row.sealName}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.seal.sealRemove(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '删除失败'))
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="toolbar">
      <el-input v-model="query.name" placeholder="印章名称" clearable style="width: 180px" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-select v-model="query.storageType" placeholder="存储类型" clearable style="width: 130px">
        <el-option v-for="opt in storageTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="openCreate">新增印章</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="sealName" label="印章名称" min-width="150" />
      <el-table-column prop="sealType" label="印章类型" width="120" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ sealStatusLabels[(row as ElectronicSealVO).status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="存储类型" width="110">
        <template #default="{ row }">
          {{ (row as ElectronicSealVO).storageType ? storageTypeLabels[(row as ElectronicSealVO).storageType!] : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="sealCode" label="印章编码" width="130" />
      <el-table-column prop="usageCount" label="使用次数" width="100" />
      <el-table-column prop="lastUsedTime" label="最近使用时间" width="180" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row as ElectronicSealVO)">详情</el-button>
          <el-button link type="primary" size="small" @click="openEdit(row as ElectronicSealVO)">编辑</el-button>
          <el-button link type="danger" size="small" @click="onRemove(row as ElectronicSealVO)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑电子印章' : '新增电子印章'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="印章名称" prop="sealName">
          <el-input v-model="form.sealName" placeholder="请输入印章名称" />
        </el-form-item>
        <el-form-item label="印章类型" prop="sealType">
          <el-input v-model="form.sealType" placeholder="如：公章 / 合同章 / 财务章" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" :disabled="!editingId" style="width: 100%">
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="存储类型">
          <el-select v-model="form.storageType" placeholder="请选择" style="width: 100%">
            <el-option v-for="opt in storageTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="印章编码">
          <el-input v-model="form.sealCode" placeholder="印章编码（可选）" />
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

    <el-dialog v-model="detailVisible" title="电子印章详情" width="560px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="印章名称">{{ detail.sealName }}</el-descriptions-item>
        <el-descriptions-item label="印章类型">{{ detail.sealType }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ sealStatusLabels[detail.status] }}</el-descriptions-item>
        <el-descriptions-item label="存储类型">{{ detail.storageType ? storageTypeLabels[detail.storageType] : '-' }}</el-descriptions-item>
        <el-descriptions-item label="印章编码">{{ detail.sealCode ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="使用次数">{{ detail.usageCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="最近使用时间">{{ detail.lastUsedTime ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="密码保护">{{ detail.passwordProtected ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detail.description ?? '-' }}</el-descriptions-item>
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
