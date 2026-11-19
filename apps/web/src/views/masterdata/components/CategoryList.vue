<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  ElButton,
  ElDescriptions,
  ElDescriptionsItem,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElPagination,
  ElSelect,
  ElSwitch,
  ElTable,
  ElTableColumn,
  ElTag,
  ElTree,
  ElTreeSelect
} from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ceamsApi } from '@/services/http'
import type { CategoryType, CategoryVO } from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { apiErrorMessage } from '@/utils/error'
import { categoryTypeLabels, toOptions } from '@/utils/labels'

defineOptions({ name: 'CategoryList' })

const typeOptions = toOptions(categoryTypeLabels)
const treeProps = { label: 'name', children: 'children' }

// 查询条件
const query = reactive<{ type?: CategoryType; parentId?: string }>({})
const { page, size, total, resetPage } = usePage()
const list = ref<CategoryVO[]>([])
const tree = ref<CategoryVO[]>([])
const loading = ref(false)

// 编辑 / 详情弹窗
const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<string | null>(null)
const detail = ref<CategoryVO | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  name: '',
  type: 'MATERIAL' as CategoryType,
  code: '',
  parentId: '',
  sort: 0,
  enabled: true
})

// 弹窗父分类候选
const parentTree = ref<CategoryVO[]>([])

function filterTree(nodes: CategoryVO[], excludeId: string): CategoryVO[] {
  const result: CategoryVO[] = []
  for (const node of nodes) {
    if (node.id === excludeId) continue
    const children = node.children ? filterTree(node.children, excludeId) : undefined
    result.push(children ? { ...node, children } : { ...node })
  }
  return result
}

const parentOptions = computed<CategoryVO[]>(() => {
  if (!editingId.value) return parentTree.value
  return filterTree(parentTree.value, editingId.value)
})

async function loadParentTree(): Promise<void> {
  try {
    parentTree.value = await ceamsApi.masterdata.categoryTree({ type: form.type })
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载父分类失败'))
  }
}

watch(
  () => form.type,
  () => {
    if (dialogVisible.value && !editingId.value) {
      form.parentId = ''
      void loadParentTree()
    }
  }
)

const rules: FormRules<typeof form> = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择分类类型', trigger: 'change' }]
}

async function loadTree(): Promise<void> {
  try {
    tree.value = await ceamsApi.masterdata.categoryTree({ type: query.type })
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载分类树失败'))
  }
}

function collectNameMap(nodes: CategoryVO[], map: Map<string, string>): void {
  for (const node of nodes) {
    map.set(node.id, node.name)
    if (node.children && node.children.length) {
      collectNameMap(node.children, map)
    }
  }
}

const categoryNameMap = computed<Map<string, string>>(() => {
  const map = new Map<string, string>()
  collectNameMap(tree.value, map)
  return map
})

function parentCategoryName(row: CategoryVO): string {
  if (!row.parentId) return '-'
  return categoryNameMap.value.get(row.parentId) ?? row.parentId
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.masterdata.categoryPage({
      page: page.value,
      size: size.value,
      type: query.type,
      parentId: query.parentId
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载分类失败'))
  } finally {
    loading.value = false
  }
}

function onSearch(): void {
  resetPage()
  void load()
  void loadTree()
}

function openCreate(): void {
  editingId.value = null
  form.name = ''
  form.type = 'MATERIAL'
  form.code = ''
  form.parentId = ''
  form.sort = 0
  form.enabled = true
  dialogVisible.value = true
  void loadParentTree()
}

function openEdit(row: CategoryVO): void {
  editingId.value = row.id
  form.name = row.name
  form.type = row.type
  form.code = row.code ?? ''
  form.parentId = row.parentId ?? ''
  form.sort = row.sort ?? 0
  form.enabled = row.enabled ?? true
  dialogVisible.value = true
  void loadParentTree()
}

function openDetail(row: CategoryVO): void {
  detail.value = row
  detailVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await ceamsApi.masterdata.categoryUpdate(editingId.value, {
        name: form.name,
        code: form.code || undefined,
        parentId: form.parentId || null,
        sort: form.sort,
        enabled: form.enabled
      })
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.masterdata.categoryCreate({
        name: form.name,
        type: form.type,
        code: form.code || null,
        parentId: form.parentId || null,
        sort: form.sort
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
    await loadTree()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存失败'))
  }
}

async function onRemove(row: CategoryVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除分类「${row.name}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await ceamsApi.masterdata.categoryRemove(row.id)
    ElMessage.success('删除成功')
    await load()
    await loadTree()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '删除失败'))
  }
}

onMounted(() => {
  void load()
  void loadTree()
})
</script>

<template>
  <div class="category-page">
    <div class="toolbar">
      <el-select v-model="query.type" placeholder="分类类型" clearable style="width: 180px">
        <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-tree-select
        v-model="query.parentId"
        :data="tree"
        :props="{ label: 'name', children: 'children' }"
        node-key="id"
        check-strictly
        clearable
        filterable
        placeholder="父级分类"
        style="width: 200px"
      />
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="openCreate">新增分类</el-button>
    </div>

    <div class="body">
      <div class="tree-pane">
        <h3>分类树</h3>
        <el-tree :data="tree" :props="treeProps" node-key="id" default-expand-all />
      </div>
      <div class="list-pane">
        <el-table v-loading="loading" :data="list" border>
          <el-table-column prop="name" label="名称" min-width="160" />
          <el-table-column label="类型" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ categoryTypeLabels[(row as CategoryVO).type] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="code" label="编码" width="140" />
          <el-table-column label="父级分类" width="180">
            <template #default="{ row }">{{ parentCategoryName(row as CategoryVO) }}</template>
          </el-table-column>
          <el-table-column prop="sort" label="排序" width="80" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
                {{ row.enabled ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openDetail(row as CategoryVO)">详情</el-button>
              <el-button link type="primary" size="small" @click="openEdit(row as CategoryVO)">编辑</el-button>
              <el-button link type="danger" size="small" @click="onRemove(row as CategoryVO)">删除</el-button>
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
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑分类' : '新增分类'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" :disabled="!!editingId" style="width: 100%">
            <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="编码">
          <el-input v-model="form.code" placeholder="编码（可选）" />
        </el-form-item>
        <el-form-item label="父级分类">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            filterable
            default-expand-all
            placeholder="不选择则为一级分类"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item v-if="editingId" label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="分类详情" width="520px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ categoryTypeLabels[detail.type] }}</el-descriptions-item>
        <el-descriptions-item label="编码">{{ detail.code ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="父级分类">{{ parentCategoryName(detail as CategoryVO) }}</el-descriptions-item>
        <el-descriptions-item label="排序">{{ detail.sort ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.enabled ? '启用' : '停用' }}</el-descriptions-item>
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
.body {
  display: flex;
  gap: 16px;
}
.tree-pane {
  width: 280px;
  flex-shrink: 0;
}
.tree-pane h3 {
  margin: 0 0 8px;
  font-size: 14px;
}
.list-pane {
  flex: 1;
  min-width: 0;
}
.pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
