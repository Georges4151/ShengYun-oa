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
  ElPagination,
  ElTable,
  ElTableColumn
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ceamsApi } from '@/services/http'
import type { MaterialVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'

defineOptions({ name: 'MaterialList' })

const query = reactive<{ keyword?: string; categoryId?: string }>({})
const { page, size, total, resetPage } = usePage()
const list = ref<MaterialVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<MaterialVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  materialCode: '',
  materialName: '',
  specification: '',
  unit: '',
  currentPrice: '',
  supplierId: '',
  categoryId: ''
})

const rules: FormRules<typeof form> = {
  materialCode: [{ required: true, message: '请输入材料编码', trigger: 'blur' }],
  materialName: [{ required: true, message: '请输入材料名称', trigger: 'blur' }],
  unit: [{ required: true, message: '请输入计量单位', trigger: 'blur' }]
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.masterdata.materialPage({
      page: page.value,
      size: size.value,
      keyword: query.keyword,
      categoryId: query.categoryId
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载材料失败'))
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
    materialCode: '',
    materialName: '',
    specification: '',
    unit: '',
    currentPrice: '',
    supplierId: '',
    categoryId: ''
  })
  dialogVisible.value = true
}

function openEdit(row: MaterialVO): void {
  editingId.value = row.id
  form.materialCode = row.materialCode
  form.materialName = row.materialName
  form.specification = row.specification ?? ''
  form.unit = row.unit
  form.currentPrice = row.currentPrice ?? ''
  form.supplierId = row.supplierId ?? ''
  form.categoryId = row.categoryId ?? ''
  dialogVisible.value = true
}

function openDetail(row: MaterialVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.masterdata.materialUpdate(editingId.value, {
        materialName: form.materialName,
        specification: form.specification || undefined,
        unit: form.unit,
        currentPrice: form.currentPrice || undefined,
        supplierId: form.supplierId || undefined,
        categoryId: form.categoryId || undefined
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.masterdata.materialCreate({
        materialCode: form.materialCode,
        materialName: form.materialName,
        specification: form.specification || null,
        unit: form.unit,
        currentPrice: form.currentPrice || undefined,
        supplierId: form.supplierId || null,
        categoryId: form.categoryId || null
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存失败'))
  }
}

async function onRemove(row: MaterialVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除材料「${row.materialName}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.masterdata.materialRemove(row.id)
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
      <el-input v-model="query.keyword" placeholder="关键字（编码/名称）" clearable style="width: 220px" />
      <el-input v-model="query.categoryId" placeholder="分类 ID（可选）" clearable style="width: 200px" />
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="openCreate">新增材料</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="materialCode" label="编码" width="140" />
      <el-table-column prop="materialName" label="材料名称" min-width="160" />
      <el-table-column prop="specification" label="规格" min-width="140" />
      <el-table-column prop="unit" label="单位" width="80" />
      <el-table-column prop="currentPrice" label="当前价" width="120" />
      <el-table-column prop="supplierId" label="供应商 ID" width="180" />
      <el-table-column prop="categoryId" label="分类 ID" width="180" />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row as MaterialVO)">详情</el-button>
          <el-button link type="primary" size="small" @click="openEdit(row as MaterialVO)">编辑</el-button>
          <el-button link type="danger" size="small" @click="onRemove(row as MaterialVO)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑材料' : '新增材料'" width="540px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="材料编码" prop="materialCode">
          <el-input v-model="form.materialCode" :disabled="!!editingId" placeholder="请输入材料编码" />
        </el-form-item>
        <el-form-item label="材料名称" prop="materialName">
          <el-input v-model="form.materialName" placeholder="请输入材料名称" />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="form.specification" placeholder="规格型号（可选）" />
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-input v-model="form.unit" placeholder="如：吨 / 米 / 个" />
        </el-form-item>
        <el-form-item label="当前价">
          <el-input v-model="form.currentPrice" placeholder="金额（字符串，如 1280.50）" />
        </el-form-item>
        <el-form-item label="供应商 ID">
          <el-input v-model="form.supplierId" placeholder="往来单位 ID（可选）" />
        </el-form-item>
        <el-form-item label="分类 ID">
          <el-input v-model="form.categoryId" placeholder="分类 ID（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="材料详情" width="540px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="编码">{{ detail.materialCode }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ detail.materialName }}</el-descriptions-item>
        <el-descriptions-item label="规格">{{ detail.specification ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="单位">{{ detail.unit }}</el-descriptions-item>
        <el-descriptions-item label="当前价">{{ detail.currentPrice ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="供应商 ID">{{ detail.supplierId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="分类 ID">{{ detail.categoryId ?? '-' }}</el-descriptions-item>
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
