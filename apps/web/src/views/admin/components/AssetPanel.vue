<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ElButton,
  ElDatePicker,
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
  ElTag,
  ElTreeSelect
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { formatDate, formatMoney } from '@ceams/utils'
import { ceamsApi } from '@/services/http'
import type {
  AssetVO,
  AssetCategory,
  AssetCreateRequest,
  AssetInventoryItem,
  AssetStatus,
  DeptVO,
  Id,
  UserVO
} from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { hasPermission } from '@/composables/usePermission'
import { apiErrorMessage } from '@/utils/error'
import { assetCategoryLabels, assetStatusLabels, toOptions } from '@/utils/labels'

defineOptions({ name: 'AssetPanel' })

const MONEY_PATTERN = /^\d+(\.\d{1,2})?$/

function moneyValidator(
  _rule: unknown,
  value: unknown,
  callback: (error?: Error) => void
): void {
  const text = String(value ?? '').trim()
  if (!text) {
    callback()
    return
  }
  callback(MONEY_PATTERN.test(text) ? undefined : new Error('请输入非负金额，最多两位小数'))
}

const canManage = computed(() => hasPermission('admin:asset:manage'))
const categoryOptions = toOptions(assetCategoryLabels)
const statusOptions = toOptions(assetStatusLabels)

function statusTagType(status: AssetStatus): 'success' | 'warning' | 'info' | 'danger' {
  switch (status) {
    case 'IN_STOCK':
      return 'success'
    case 'BORROWED':
      return 'warning'
    case 'REPAIRING':
      return 'info'
    default:
      return 'danger'
  }
}

// ---------- 下拉数据 ----------
const users = ref<UserVO[]>([])
const depts = ref<DeptVO[]>([])
const userNameMap = computed(() => new Map(users.value.map((item) => [item.id, item.name])))

function keeperName(id: Id | null): string {
  if (!id) return '-'
  return userNameMap.value.get(id) ?? id
}

async function loadOptions(): Promise<void> {
  try {
    const [userRes, deptRes] = await Promise.all([
      ceamsApi.iam.userPage({ page: 1, size: 200 }),
      ceamsApi.iam.deptTree()
    ])
    users.value = userRes.records
    depts.value = deptRes
  } catch {
    // 下拉数据加载失败不阻断资产台账
  }
}

// ---------- 台账列表 ----------
const query = reactive<{ keyword: string; status: AssetStatus | ''; deptId: Id | '' }>({
  keyword: '',
  status: '',
  deptId: ''
})
const { page, size, total, resetPage } = usePage()
const list = ref<AssetVO[]>([])
const loading = ref(false)
const selection = ref<AssetVO[]>([])

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.admin.assetPage({
      page: page.value,
      size: size.value,
      keyword: query.keyword.trim() || undefined,
      status: query.status || undefined,
      deptId: query.deptId || undefined
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载资产台账失败'))
  } finally {
    loading.value = false
  }
}

function onSearch(): void {
  resetPage()
  void load()
}

function onSelectionChange(rows: AssetVO[]): void {
  selection.value = rows
}

// ---------- 新建 / 编辑 ----------
const formVisible = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<Id | null>(null)
const form = reactive({
  name: '',
  category: 'ELECTRONIC' as AssetCategory,
  specification: '',
  purchaseDate: '',
  purchasePrice: '',
  deptId: '',
  keeperId: '',
  location: '',
  depreciationMonths: '',
  remark: ''
})

const formRules: FormRules<typeof form> = {
  name: [{ required: true, message: '请输入资产名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择资产分类', trigger: 'change' }],
  purchasePrice: [{ validator: moneyValidator, trigger: 'blur' }]
}

function openCreate(): void {
  editingId.value = null
  Object.assign(form, {
    name: '',
    category: 'ELECTRONIC',
    specification: '',
    purchaseDate: '',
    purchasePrice: '',
    deptId: '',
    keeperId: '',
    location: '',
    depreciationMonths: '',
    remark: ''
  })
  formVisible.value = true
}

function openEdit(row: AssetVO): void {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    category: row.category ?? 'ELECTRONIC',
    specification: row.specification ?? '',
    purchaseDate: row.purchaseDate ?? '',
    purchasePrice: row.purchasePrice === null ? '' : String(row.purchasePrice),
    deptId: row.deptId ?? '',
    keeperId: row.keeperId ?? '',
    location: row.location ?? '',
    depreciationMonths: row.depreciationMonths === null ? '' : String(row.depreciationMonths),
    remark: row.remark ?? ''
  })
  formVisible.value = true
}

async function submitForm(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const payload: AssetCreateRequest = {
    name: form.name.trim(),
    category: form.category,
    specification: form.specification.trim() || undefined,
    purchaseDate: form.purchaseDate || undefined,
    purchasePrice: form.purchasePrice ? Number(form.purchasePrice) : undefined,
    deptId: form.deptId || undefined,
    keeperId: form.keeperId || undefined,
    location: form.location.trim() || undefined,
    depreciationMonths: form.depreciationMonths ? Number(form.depreciationMonths) : undefined,
    remark: form.remark.trim() || undefined
  }

  try {
    if (editingId.value) {
      await ceamsApi.admin.assetUpdate(editingId.value, { ...payload, id: editingId.value })
      ElMessage.success('资产信息已更新')
    } else {
      await ceamsApi.admin.assetCreate(payload)
      ElMessage.success('资产已入库')
    }
    formVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存资产失败'))
  }
}

// ---------- 领用 ----------
const borrowVisible = ref(false)
const borrowAsset = ref<AssetVO | null>(null)
const borrowFormRef = ref<FormInstance>()
const borrowForm = reactive({ borrowDate: '', expectedReturnDate: '', remark: '' })
const borrowRules: FormRules<typeof borrowForm> = {
  borrowDate: [{ required: true, message: '请选择领用日期', trigger: 'change' }]
}

function openBorrow(row: AssetVO): void {
  borrowAsset.value = row
  Object.assign(borrowForm, {
    borrowDate: formatDate(new Date(), ''),
    expectedReturnDate: '',
    remark: ''
  })
  borrowVisible.value = true
}

async function submitBorrow(): Promise<void> {
  if (!borrowFormRef.value || !borrowAsset.value) return
  const valid = await borrowFormRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    await ceamsApi.admin.assetBorrow(borrowAsset.value.id, {
      borrowDate: borrowForm.borrowDate,
      expectedReturnDate: borrowForm.expectedReturnDate || undefined,
      remark: borrowForm.remark.trim() || undefined
    })
    ElMessage.success('领用申请已提交，请在待办中心跟踪审批')
    borrowVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '提交领用申请失败'))
  }
}

// ---------- 归还 / 报废 ----------
async function onReturn(row: AssetVO): Promise<void> {
  const confirmed = await ElMessageBox.confirm(
    `确认归还资产「${row.name}」？`,
    '归还确认',
    { type: 'warning' }
  ).catch(() => false)
  if (!confirmed) return
  try {
    await ceamsApi.admin.assetReturn(row.id)
    ElMessage.success('资产已归还')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '资产归还失败'))
  }
}

async function onScrap(row: AssetVO): Promise<void> {
  const confirmed = await ElMessageBox.confirm(
    `确认报废资产「${row.name}」？报废后不可恢复。`,
    '报废确认',
    { type: 'warning' }
  ).catch(() => false)
  if (!confirmed) return
  try {
    await ceamsApi.admin.assetScrap(row.id)
    ElMessage.success('资产已报废')
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '资产报废失败'))
  }
}

// ---------- 盘点 ----------
interface InventoryRow {
  assetId: Id
  assetNo: string
  name: string
  location: string
  keeperId: Id | ''
  status: '' | 'IN_STOCK' | 'REPAIRING'
}

const inventoryVisible = ref(false)
const inventoryRows = ref<InventoryRow[]>([])
const inventorySubmitting = ref(false)

function openInventory(): void {
  if (selection.value.length === 0) {
    ElMessage.warning('请先勾选需要盘点的资产')
    return
  }
  inventoryRows.value = selection.value.map((item) => ({
    assetId: item.id,
    assetNo: item.assetNo,
    name: item.name,
    location: item.location ?? '',
    keeperId: item.keeperId ?? '',
    status: item.status === 'IN_STOCK' ? 'IN_STOCK' : ''
  }))
  inventoryVisible.value = true
}

async function submitInventory(): Promise<void> {
  if (inventoryRows.value.length === 0) return
  inventorySubmitting.value = true
  try {
    const items: AssetInventoryItem[] = inventoryRows.value.map((row) => ({
      assetId: row.assetId,
      location: row.location.trim() || undefined,
      keeperId: row.keeperId || undefined,
      status: row.status === '' ? undefined : row.status
    }))
    const changed = await ceamsApi.admin.assetInventory({ items })
    ElMessage.success(`盘点完成，更新 ${changed} 项资产`)
    inventoryVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '盘点提交失败'))
  } finally {
    inventorySubmitting.value = false
  }
}

onMounted(() => {
  void loadOptions()
  void load()
})
</script>

<template>
  <div>
    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="资产编号 / 名称"
        clearable
        style="width: 220px"
        @keyup.enter="onSearch"
      />
      <el-select v-model="query.status" placeholder="资产状态" clearable style="width: 140px">
        <el-option
          v-for="opt in statusOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <el-tree-select
        v-model="query.deptId"
        :data="depts"
        :props="{ label: 'name', children: 'children' }"
        node-key="id"
        check-strictly
        clearable
        placeholder="使用部门"
        style="width: 180px"
      />
      <el-button type="primary" @click="onSearch">查询</el-button>
      <template v-if="canManage">
        <el-button @click="openCreate">新建资产</el-button>
        <el-button :disabled="selection.length === 0" @click="openInventory">批量盘点</el-button>
      </template>
    </div>

    <el-table
      v-loading="loading"
      :data="list"
      border
      @selection-change="onSelectionChange"
    >
      <el-table-column v-if="canManage" type="selection" width="46" />
      <el-table-column prop="assetNo" label="资产编号" width="160" />
      <el-table-column prop="name" label="资产名称" min-width="150" />
      <el-table-column label="分类" width="110">
        <template #default="{ row }">
          {{ assetCategoryLabels[(row as AssetVO).category ?? 'OTHER'] }}
        </template>
      </el-table-column>
      <el-table-column prop="specification" label="规格型号" min-width="130">
        <template #default="{ row }">{{ (row as AssetVO).specification ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="购置日期" width="120">
        <template #default="{ row }">{{ (row as AssetVO).purchaseDate ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="购置金额" width="120" align="right">
        <template #default="{ row }">{{ formatMoney((row as AssetVO).purchasePrice) }}</template>
      </el-table-column>
      <el-table-column label="存放地点" min-width="130">
        <template #default="{ row }">{{ (row as AssetVO).location ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="保管人" width="110">
        <template #default="{ row }">{{ keeperName((row as AssetVO).keeperId) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="statusTagType((row as AssetVO).status)">
            {{ assetStatusLabels[(row as AssetVO).status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <template v-if="canManage">
            <el-button link type="primary" size="small" @click="openEdit(row as AssetVO)">
              编辑
            </el-button>
            <el-button
              v-if="(row as AssetVO).status === 'IN_STOCK'"
              link
              type="primary"
              size="small"
              @click="openBorrow(row as AssetVO)"
            >
              领用
            </el-button>
            <el-button
              v-if="(row as AssetVO).status === 'BORROWED'"
              link
              type="warning"
              size="small"
              @click="onReturn(row as AssetVO)"
            >
              归还
            </el-button>
            <el-button
              v-if="(row as AssetVO).status === 'IN_STOCK'"
              link
              type="danger"
              size="small"
              @click="onScrap(row as AssetVO)"
            >
              报废
            </el-button>
          </template>
          <span v-else>-</span>
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

    <!-- 新建 / 编辑资产 -->
    <el-dialog
      v-model="formVisible"
      :title="editingId ? '编辑资产' : '新建资产'"
      width="620px"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="资产名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入资产名称" />
        </el-form-item>
        <el-form-item label="资产分类" prop="category">
          <el-select v-model="form.category" style="width: 100%">
            <el-option
              v-for="opt in categoryOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="规格型号">
          <el-input v-model="form.specification" placeholder="规格型号（可选）" />
        </el-form-item>
        <el-form-item label="购置日期">
          <el-date-picker
            v-model="form.purchaseDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="购置金额" prop="purchasePrice">
          <el-input v-model="form.purchasePrice" placeholder="购置金额（元，可空）" />
        </el-form-item>
        <el-form-item label="使用部门">
          <el-tree-select
            v-model="form.deptId"
            :data="depts"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            placeholder="请选择使用部门"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="保管人">
          <el-select
            v-model="form.keeperId"
            placeholder="请选择保管人"
            clearable
            filterable
            style="width: 100%"
          >
            <el-option v-for="item in users" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="存放地点">
          <el-input v-model="form.location" placeholder="存放地点（可选）" />
        </el-form-item>
        <el-form-item label="折旧月数">
          <el-input v-model="form.depreciationMonths" placeholder="折旧月数（可选）" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 领用申请 -->
    <el-dialog v-model="borrowVisible" title="资产领用" width="520px">
      <el-form ref="borrowFormRef" :model="borrowForm" :rules="borrowRules" label-width="100px">
        <el-form-item label="领用资产">
          <span>{{ borrowAsset?.assetNo }} · {{ borrowAsset?.name }}</span>
        </el-form-item>
        <el-form-item label="领用人">
          <span>当前登录用户</span>
        </el-form-item>
        <el-form-item label="领用日期" prop="borrowDate">
          <el-date-picker
            v-model="borrowForm.borrowDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="预计归还">
          <el-date-picker
            v-model="borrowForm.expectedReturnDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="borrowForm.remark"
            type="textarea"
            :rows="2"
            placeholder="领用说明（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="borrowVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBorrow">提交审批</el-button>
      </template>
    </el-dialog>

    <!-- 批量盘点 -->
    <el-dialog v-model="inventoryVisible" title="批量盘点" width="760px">
      <el-table :data="inventoryRows" border size="small">
        <el-table-column prop="assetNo" label="资产编号" width="150" />
        <el-table-column prop="name" label="资产名称" min-width="140" />
        <el-table-column label="盘点地点" min-width="160">
          <template #default="{ row }">
            <el-input v-model="row.location" placeholder="盘点地点" />
          </template>
        </el-table-column>
        <el-table-column label="保管人" width="170">
          <template #default="{ row }">
            <el-select v-model="row.keeperId" placeholder="保持原保管人" clearable filterable>
              <el-option
                v-for="item in users"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="盘点状态" width="150">
          <template #default="{ row }">
            <el-select
              v-model="row.status"
              :disabled="!row.status"
              placeholder="保持原状态"
            >
              <el-option label="在库" value="IN_STOCK" />
              <el-option label="维修中" value="REPAIRING" />
            </el-select>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="inventoryVisible = false">取消</el-button>
        <el-button type="primary" :loading="inventorySubmitting" @click="submitInventory">
          提交盘点
        </el-button>
      </template>
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
