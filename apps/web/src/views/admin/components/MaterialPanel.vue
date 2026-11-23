<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ElAlert,
  ElButton,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElOption,
  ElPagination,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { formatMoney } from '@ceams/utils'
import { ceamsApi } from '@/services/http'
import type {
  AdminMaterialVO,
  Id,
  AdminMaterialApplyVO,
  MaterialApplyStatus,
  MaterialStatus
} from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { hasPermission } from '@/composables/usePermission'
import { apiErrorMessage } from '@/utils/error'
import {
  materialApplyStatusLabels,
  materialStatusLabels,
  toOptions
} from '@/utils/labels'

defineOptions({ name: 'MaterialPanel' })

const canManage = computed(() => hasPermission('admin:material:manage'))
const statusOptions = toOptions(materialStatusLabels)

function applyTagType(status: MaterialApplyStatus): 'success' | 'warning' | 'info' | 'danger' {
  switch (status) {
    case 'APPROVED':
    case 'ISSUED':
      return 'success'
    case 'APPROVING':
      return 'warning'
    case 'REJECTED':
      return 'danger'
    default:
      return 'info'
  }
}

// ---------- 低库存预警 ----------
const alerts = ref<AdminMaterialVO[]>([])
const alertsLoading = ref(false)

async function loadAlerts(): Promise<void> {
  alertsLoading.value = true
  try {
    alerts.value = await ceamsApi.admin.materialAlerts()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载低库存预警失败'))
  } finally {
    alertsLoading.value = false
  }
}

function shortageText(ratio: string | null): string {
  if (ratio === null) return '-'
  return `${(Number(ratio) * 100).toFixed(1)}%`
}

/** 库存不高于安全库存时高亮标记。 */
function isLowStock(row: AdminMaterialVO): boolean {
  const safety = Number(row.safetyStock ?? 0)
  return safety > 0 && Number(row.stockQty ?? 0) <= safety
}

function lowStockClass({ row }: { row: AdminMaterialVO }): string {
  return isLowStock(row) ? 'row-low-stock' : ''
}

// ---------- 易耗品列表 ----------
const query = reactive<{ keyword: string; status: MaterialStatus | '' }>({
  keyword: '',
  status: ''
})
const { page, size, total, resetPage } = usePage()
const list = ref<AdminMaterialVO[]>([])
const loading = ref(false)

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.admin.materialPage({
      page: page.value,
      size: size.value,
      keyword: query.keyword.trim() || undefined,
      status: query.status || undefined
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载易耗品列表失败'))
  } finally {
    loading.value = false
  }
}

function onSearch(): void {
  resetPage()
  void load()
}

// ---------- 新建 / 编辑 ----------
const formVisible = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<Id | null>(null)
const form = reactive({
  code: '',
  name: '',
  specification: '',
  unit: '',
  stockQty: 0,
  safetyStock: 0,
  unitPrice: ''
})
const formRules: FormRules<typeof form> = {
  code: [{ required: true, message: '请输入物料编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入物料名称', trigger: 'blur' }]
}

function openCreate(): void {
  editingId.value = null
  Object.assign(form, {
    code: '',
    name: '',
    specification: '',
    unit: '',
    stockQty: 0,
    safetyStock: 0,
    unitPrice: ''
  })
  formVisible.value = true
}

function openEdit(row: AdminMaterialVO): void {
  editingId.value = row.id
  Object.assign(form, {
    code: row.code,
    name: row.name,
    specification: row.specification ?? '',
    unit: row.unit ?? '',
    stockQty: row.stockQty === null ? 0 : Number(row.stockQty),
    safetyStock: row.safetyStock === null ? 0 : Number(row.safetyStock),
    unitPrice: row.unitPrice === null ? '' : String(row.unitPrice)
  })
  formVisible.value = true
}

async function submitForm(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const unitPrice = form.unitPrice.trim() ? Number(form.unitPrice) : undefined

  try {
    if (editingId.value) {
      await ceamsApi.admin.materialUpdate(editingId.value, {
        id: editingId.value,
        code: form.code.trim(),
        name: form.name.trim(),
        specification: form.specification.trim() || undefined,
        unit: form.unit.trim() || undefined,
        safetyStock: form.safetyStock,
        unitPrice
      })
      ElMessage.success('易耗品信息已更新')
    } else {
      await ceamsApi.admin.materialCreate({
        code: form.code.trim(),
        name: form.name.trim(),
        specification: form.specification.trim() || undefined,
        unit: form.unit.trim() || undefined,
        stockQty: form.stockQty,
        safetyStock: form.safetyStock,
        unitPrice
      })
      ElMessage.success('易耗品已创建')
    }
    formVisible.value = false
    await Promise.all([load(), loadAlerts(), loadApplyMaterials()])
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存易耗品失败'))
  }
}

// ---------- 入库 ----------
const stockInVisible = ref(false)
const stockInMaterial = ref<AdminMaterialVO | null>(null)
const stockInForm = reactive({ quantity: 1, remark: '' })

function openStockIn(row: AdminMaterialVO): void {
  stockInMaterial.value = row
  Object.assign(stockInForm, { quantity: 1, remark: '' })
  stockInVisible.value = true
}

async function submitStockIn(): Promise<void> {
  if (!stockInMaterial.value) return
  if (!stockInForm.quantity || stockInForm.quantity <= 0) {
    ElMessage.warning('入库数量必须大于 0')
    return
  }
  try {
    await ceamsApi.admin.materialStockIn(stockInMaterial.value.id, {
      quantity: stockInForm.quantity,
      remark: stockInForm.remark.trim() || undefined
    })
    ElMessage.success('入库成功')
    stockInVisible.value = false
    await Promise.all([load(), loadAlerts()])
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '入库失败'))
  }
}

// ---------- 申领 ----------
const applyMaterials = ref<AdminMaterialVO[]>([])
const applyVisible = ref(false)
const applyFormRef = ref<FormInstance>()
const applyForm = reactive({ materialId: '', quantity: 1, reason: '' })
const applyRules: FormRules<typeof applyForm> = {
  materialId: [{ required: true, message: '请选择物料', trigger: 'change' }]
}

async function loadApplyMaterials(): Promise<void> {
  try {
    const res = await ceamsApi.admin.materialPage({ page: 1, size: 100, status: 'ENABLED' })
    applyMaterials.value = res.records
  } catch {
    // 申领下拉加载失败不阻断列表
  }
}

function openApply(): void {
  Object.assign(applyForm, { materialId: '', quantity: 1, reason: '' })
  applyVisible.value = true
}

async function submitApply(): Promise<void> {
  if (!applyFormRef.value) return
  const valid = await applyFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (!applyForm.quantity || applyForm.quantity <= 0) {
    ElMessage.warning('申领数量必须大于 0')
    return
  }
  try {
    await ceamsApi.admin.materialApply({
      materialId: applyForm.materialId,
      quantity: applyForm.quantity,
      reason: applyForm.reason.trim() || undefined
    })
    ElMessage.success('申领已提交，请在待办中心跟踪审批')
    applyVisible.value = false
    await loadMyApplies()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '提交申领失败'))
  }
}

// ---------- 我的申领 / 发放 ----------
const myApplies = ref<AdminMaterialApplyVO[]>([])
const myLoading = ref(false)

async function loadMyApplies(): Promise<void> {
  myLoading.value = true
  try {
    myApplies.value = await ceamsApi.admin.myMaterialApplies()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载我的申领失败'))
  } finally {
    myLoading.value = false
  }
}

async function onIssue(row: AdminMaterialApplyVO): Promise<void> {
  try {
    await ceamsApi.admin.materialIssue(row.id)
    ElMessage.success('发放成功')
    await Promise.all([loadMyApplies(), load(), loadAlerts()])
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '发放失败'))
  }
}

onMounted(() => {
  void loadAlerts()
  void load()
  void loadApplyMaterials()
  void loadMyApplies()
})
</script>

<template>
  <div class="material-panel">
    <!-- 低库存预警卡片 -->
    <div class="data-card panel-card">
      <div class="card-header">
        <div class="card-heading">
          <span class="card-title">低库存预警</span>
          <span class="card-count">共 {{ alerts.length }} 项</span>
        </div>
      </div>
      <el-alert
        v-if="alerts.length === 0"
        title="当前没有低于安全库存的易耗品"
        type="success"
        :closable="false"
        show-icon
      />
      <el-table
        v-else
        v-loading="alertsLoading"
        :data="alerts"
        :row-class-name="lowStockClass"
        border
        size="small"
      >
        <el-table-column prop="code" label="物料编码" width="150" />
        <el-table-column prop="name" label="物料名称" min-width="140" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="stockQty" label="当前库存" width="100" align="right" />
        <el-table-column prop="safetyStock" label="安全库存" width="100" align="right" />
        <el-table-column label="缺口比例" width="110" align="right">
          <template #default="{ row }">{{ shortageText((row as AdminMaterialVO).shortageRatio) }}</template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 易耗品筛选区 -->
    <div class="filter-bar">
      <el-input
        v-model="query.keyword"
        placeholder="编码 / 名称"
        clearable
        style="width: 220px"
        @keyup.enter="onSearch"
      />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
        <el-option
          v-for="opt in statusOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <div class="filter-actions">
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button v-if="canManage" @click="openApply">申领</el-button>
      </div>
    </div>

    <!-- 易耗品台账卡片 -->
    <div class="data-card panel-card">
      <div class="card-header">
        <div class="card-heading">
          <span class="card-title">易耗品台账</span>
          <span class="card-count">共 {{ total }} 项</span>
        </div>
        <el-button v-if="canManage" type="primary" @click="openCreate">新建易耗品</el-button>
      </div>

      <el-table v-loading="loading" :data="list" :row-class-name="lowStockClass" border>
        <el-table-column prop="code" label="物料编码" width="150" />
        <el-table-column prop="name" label="物料名称" min-width="150" />
        <el-table-column prop="specification" label="规格型号" min-width="130">
          <template #default="{ row }">{{ (row as AdminMaterialVO).specification ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="80">
          <template #default="{ row }">{{ (row as AdminMaterialVO).unit ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="stockQty" label="库存数量" width="100" align="right">
          <template #default="{ row }">{{ (row as AdminMaterialVO).stockQty ?? 0 }}</template>
        </el-table-column>
        <el-table-column prop="safetyStock" label="安全库存" width="100" align="right">
          <template #default="{ row }">{{ (row as AdminMaterialVO).safetyStock ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="单价" width="110" align="right">
          <template #default="{ row }">{{ formatMoney((row as AdminMaterialVO).unitPrice) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="(row as AdminMaterialVO).status === 'ENABLED' ? 'success' : 'info'">
              {{ materialStatusLabels[(row as AdminMaterialVO).status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="canManage" label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row as AdminMaterialVO)">
              编辑
            </el-button>
            <el-button link type="primary" size="small" @click="openStockIn(row as AdminMaterialVO)">
              入库
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页右对齐，距表 16px -->
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

    <!-- 我的申领卡片 -->
    <div class="data-card panel-card">
      <div class="card-header">
        <div class="card-heading">
          <span class="card-title">我的申领</span>
          <span class="card-count">共 {{ myApplies.length }} 条</span>
        </div>
      </div>
      <el-table v-loading="myLoading" :data="myApplies" border>
        <el-table-column prop="materialCode" label="物料编码" width="140" />
        <el-table-column prop="materialName" label="物料名称" min-width="140" />
        <el-table-column prop="quantity" label="数量" width="90" align="right">
          <template #default="{ row }">
            {{ (row as AdminMaterialApplyVO).quantity ?? '-' }}{{ (row as AdminMaterialApplyVO).unit ?? '' }}
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="申领原因" min-width="160">
          <template #default="{ row }">{{ (row as AdminMaterialApplyVO).reason ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="applyTagType((row as AdminMaterialApplyVO).status)">
              {{ materialApplyStatusLabels[(row as AdminMaterialApplyVO).status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发放时间" width="170">
          <template #default="{ row }">{{ (row as AdminMaterialApplyVO).issuedAt ?? '-' }}</template>
        </el-table-column>
        <el-table-column v-if="canManage" label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="(row as AdminMaterialApplyVO).status === 'APPROVED'"
              link
              type="primary"
              size="small"
              @click="onIssue(row as AdminMaterialApplyVO)"
            >
              发放
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新建 / 编辑易耗品 -->
    <el-dialog
      v-model="formVisible"
      class="admin-dialog"
      :title="editingId ? '编辑易耗品' : '新建易耗品'"
      width="560px"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="物料编码" prop="code">
          <el-input v-model="form.code" placeholder="请输入物料编码" />
        </el-form-item>
        <el-form-item label="物料名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入物料名称" />
        </el-form-item>
        <el-form-item label="规格型号">
          <el-input v-model="form.specification" placeholder="规格型号（可选）" />
        </el-form-item>
        <el-form-item label="计量单位">
          <el-input v-model="form.unit" placeholder="如：个 / 箱（可选）" />
        </el-form-item>
        <el-form-item label="初始库存">
          <el-input-number v-model="form.stockQty" :min="0" :disabled="!!editingId" style="width: 100%" />
        </el-form-item>
        <el-form-item label="安全库存">
          <el-input-number v-model="form.safetyStock" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="单价">
          <el-input v-model="form.unitPrice" placeholder="单价（元，可空）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 入库 -->
    <el-dialog v-model="stockInVisible" class="admin-dialog" title="易耗品入库" width="460px">
      <el-form :model="stockInForm" label-width="90px">
        <el-form-item label="物料">
          <span>{{ stockInMaterial?.name }}</span>
        </el-form-item>
        <el-form-item label="入库数量">
          <el-input-number v-model="stockInForm.quantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="stockInForm.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockInVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStockIn">确定入库</el-button>
      </template>
    </el-dialog>

    <!-- 申领 -->
    <el-dialog v-model="applyVisible" class="admin-dialog" title="易耗品申领" width="500px">
      <el-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-width="90px">
        <el-form-item label="物料" prop="materialId">
          <el-select v-model="applyForm.materialId" filterable placeholder="请选择物料" style="width: 100%">
            <el-option
              v-for="item in applyMaterials"
              :key="item.id"
              :label="`${item.name}（库存 ${item.stockQty ?? 0}${item.unit ?? ''}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="申领数量">
          <el-input-number v-model="applyForm.quantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="申领原因">
          <el-input v-model="applyForm.reason" type="textarea" :rows="2" placeholder="申领原因（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApply">提交申领</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 模块容器：各卡片之间保持 16px 间距 */
.material-panel {
  display: flex;
  flex-direction: column;
  gap: var(--ceams-space-4);
}

/* 筛选卡片：16px 20px 内边距，控件 12px 间距 */
.filter-bar {
  padding: 16px 20px;
  margin-bottom: 0;
  gap: var(--ceams-space-3);
}

/* 筛选区操作按钮靠右 */
.filter-actions {
  display: flex;
  align-items: center;
  gap: var(--ceams-space-3);
  margin-left: auto;
}

/* 列表卡片：20px 内边距 */
.panel-card {
  padding: var(--ceams-space-5);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ceams-space-3);
  margin-bottom: var(--ceams-space-4);
}

.card-heading {
  display: flex;
  align-items: baseline;
  gap: var(--ceams-space-2);
  min-width: 0;
}

.card-title {
  font-size: var(--ceams-font-size-md);
  font-weight: 600;
  color: var(--ceams-text-primary);
}

.card-count {
  font-size: var(--ceams-font-size-sm);
  color: var(--ceams-text-secondary);
}

/* 分页右对齐，距表 16px */
.pagination {
  margin-top: var(--ceams-space-4);
  justify-content: flex-end;
}

/* 空态居中灰色、40px 留白 */
.panel-card :deep(.el-table__empty-text) {
  color: var(--ceams-text-placeholder);
}

.panel-card :deep(.el-table__empty-block) {
  padding: var(--ceams-space-10) 0;
  min-height: 0;
}

/* 低库存行高亮使用功能色令牌 */
:deep(.row-low-stock) {
  color: var(--ceams-color-danger);
}

:deep(.row-low-stock .el-table__cell) {
  background: var(--ceams-color-danger-light);
}

/* 弹窗内容 20px 24px */
:global(.admin-dialog .el-dialog__body) {
  padding: 20px 24px;
}
</style>
