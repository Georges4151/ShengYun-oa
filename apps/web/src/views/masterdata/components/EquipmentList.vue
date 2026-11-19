<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  ElButton,
  ElDatePicker,
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
import type { EquipmentStatus, EquipmentVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { equipmentStatusLabels, toOptions } from '@/utils/labels'

defineOptions({ name: 'EquipmentList' })

const statusOptions = toOptions(equipmentStatusLabels)

const query = reactive<{ keyword?: string; status?: EquipmentStatus }>({})
const { page, size, total, resetPage } = usePage()
const list = ref<EquipmentVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<EquipmentVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  name: '',
  type: '',
  model: '',
  status: 'IDLE' as EquipmentStatus,
  purchaseDate: '',
  purchasePrice: ''
})

const rules: FormRules<typeof form> = {
  name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  type: [{ required: true, message: '请输入设备类型', trigger: 'blur' }],
  status: [{ required: true, message: '请选择设备状态', trigger: 'change' }]
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.masterdata.equipmentPage({
      page: page.value,
      size: size.value,
      keyword: query.keyword,
      status: query.status
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载设备失败'))
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
  Object.assign(form, { name: '', type: '', model: '', status: 'IDLE', purchaseDate: '', purchasePrice: '' })
  dialogVisible.value = true
}

function openEdit(row: EquipmentVO): void {
  editingId.value = row.id
  form.name = row.name
  form.type = row.type
  form.model = row.model ?? ''
  form.status = row.status
  form.purchaseDate = row.purchaseDate ?? ''
  form.purchasePrice = row.purchasePrice ?? ''
  dialogVisible.value = true
}

function openDetail(row: EquipmentVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.masterdata.equipmentUpdate(editingId.value, {
        name: form.name,
        type: form.type,
        model: form.model || undefined,
        status: form.status,
        purchaseDate: form.purchaseDate || undefined,
        purchasePrice: form.purchasePrice || undefined
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.masterdata.equipmentCreate({
        name: form.name,
        type: form.type,
        model: form.model || null,
        status: form.status,
        purchaseDate: form.purchaseDate || null,
        purchasePrice: form.purchasePrice || null
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存失败'))
  }
}

async function onRemove(row: EquipmentVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除设备「${row.name}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.masterdata.equipmentRemove(row.id)
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
      <el-input v-model="query.keyword" placeholder="关键字（名称/型号）" clearable style="width: 220px" />
      <el-select v-model="query.status" placeholder="设备状态" clearable style="width: 160px">
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="openCreate">新增设备</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="name" label="设备名称" min-width="160" />
      <el-table-column prop="type" label="类型" width="140" />
      <el-table-column prop="model" label="型号" width="140" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag size="small">{{ equipmentStatusLabels[(row as EquipmentVO).status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="purchaseDate" label="购置日期" width="120" />
      <el-table-column prop="purchasePrice" label="购置价格" width="120" />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row as EquipmentVO)">详情</el-button>
          <el-button link type="primary" size="small" @click="openEdit(row as EquipmentVO)">编辑</el-button>
          <el-button link type="danger" size="small" @click="onRemove(row as EquipmentVO)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑设备' : '新增设备'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="设备名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入设备名称" />
        </el-form-item>
        <el-form-item label="设备类型" prop="type">
          <el-input v-model="form.type" placeholder="如：挖掘机 / 起重机" />
        </el-form-item>
        <el-form-item label="型号">
          <el-input v-model="form.model" placeholder="型号（可选）" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="购置日期">
          <el-date-picker v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="购置价格">
          <el-input v-model="form.purchasePrice" placeholder="金额（字符串，如 500000.00）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="设备详情" width="520px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="设备名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ detail.type }}</el-descriptions-item>
        <el-descriptions-item label="型号">{{ detail.model ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ equipmentStatusLabels[detail.status] }}</el-descriptions-item>
        <el-descriptions-item label="购置日期">{{ detail.purchaseDate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="购置价格">{{ detail.purchasePrice ?? '-' }}</el-descriptions-item>
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
