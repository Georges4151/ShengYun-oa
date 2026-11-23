<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import {
  ElButton,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElMessageBox,
  ElScrollbar,
  ElTree,
  ElTreeSelect
} from 'element-plus'
import { ceamsApi } from '@/services/http'
import type {
  DirectoryCreateRequest,
  DirectoryMoveRequest,
  DirectoryNodeVO,
  DirectoryUpdateRequest,
  Id,
  LibraryType
} from '@ceams/types'
import { apiErrorMessage } from '@/utils/error'

defineOptions({ name: 'DirectoryTree' })

const props = defineProps<{
  /** 目录树数据（后端已按可见性过滤） */
  data: DirectoryNodeVO[]
  loading: boolean
  /** 当前库类型 */
  libraryType: LibraryType
  /** 当前选中的目录 ID */
  selectedId: Id | null
  /** 项目库时的项目 ID（PROJECT 建目录必填） */
  projectId: Id | null
  /** 是否具备 doc:dir:manage（公开库建目录的唯一入口） */
  canManage: boolean
}>()

const emit = defineEmits<{
  select: [node: DirectoryNodeVO]
  refresh: []
}>()

/** 新建根目录按钮：公开库仅管理员；项目库需先选项目；个人 / 部门库普通用户可建。 */
const canCreateRoot = computed(() => {
  if (props.libraryType === 'PUBLIC') return props.canManage
  if (props.libraryType === 'PROJECT') return Boolean(props.projectId)
  return true
})

/** 节点操作（新建子目录 / 改名 / 移动 / 删除）：公开库仅管理员可操作，其余库由后端数据权限兜底。 */
const canOperate = computed(() => props.libraryType !== 'PUBLIC' || props.canManage)

function onNodeClick(node: DirectoryNodeVO): void {
  emit('select', node)
}

// ---------- 右键菜单 ----------
const menuVisible = ref(false)
const menuStyle = reactive<{ left: string; top: string }>({ left: '0px', top: '0px' })
const menuNode = ref<DirectoryNodeVO | null>(null)

function onNodeContextMenu(event: Event, node: DirectoryNodeVO): void {
  event.preventDefault()
  if (!canOperate.value) return
  const mouseEvent = event as MouseEvent
  menuNode.value = node
  menuStyle.left = `${mouseEvent.clientX}px`
  menuStyle.top = `${mouseEvent.clientY}px`
  menuVisible.value = true
}

function closeMenu(): void {
  menuVisible.value = false
}

onMounted(() => {
  window.addEventListener('click', closeMenu)
})

onBeforeUnmount(() => {
  window.removeEventListener('click', closeMenu)
})

// ---------- 新建目录 ----------
const createVisible = ref(false)
const creating = ref(false)
const createParent = ref<DirectoryNodeVO | null>(null)
const createForm = reactive<{ name: string; sortOrder: number | undefined }>({
  name: '',
  sortOrder: undefined
})

function openCreate(parent: DirectoryNodeVO | null): void {
  closeMenu()
  createParent.value = parent
  createForm.name = ''
  createForm.sortOrder = undefined
  createVisible.value = true
}

async function submitCreate(): Promise<void> {
  const name = createForm.name.trim()
  if (!name) {
    ElMessage.warning('请输入目录名称')
    return
  }
  const body: DirectoryCreateRequest = {
    name,
    parentId: createParent.value?.id ?? null,
    libraryType: props.libraryType,
    sortOrder: createForm.sortOrder,
    projectId: props.libraryType === 'PROJECT' ? props.projectId : undefined
  }
  creating.value = true
  try {
    await ceamsApi.doc.directories.create(body)
    ElMessage.success('目录已创建')
    createVisible.value = false
    emit('refresh')
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '创建目录失败'))
  } finally {
    creating.value = false
  }
}

// ---------- 改名 ----------
const renameVisible = ref(false)
const renaming = ref(false)
const renameTarget = ref<DirectoryNodeVO | null>(null)
const renameForm = reactive({ name: '' })

function openRename(node: DirectoryNodeVO): void {
  closeMenu()
  renameTarget.value = node
  renameForm.name = node.name
  renameVisible.value = true
}

async function submitRename(): Promise<void> {
  const target = renameTarget.value
  if (!target) return
  const name = renameForm.name.trim()
  if (!name) {
    ElMessage.warning('请输入目录名称')
    return
  }
  const body: DirectoryUpdateRequest = { name }
  renaming.value = true
  try {
    await ceamsApi.doc.directories.update(target.id, body)
    ElMessage.success('目录已改名')
    renameVisible.value = false
    emit('refresh')
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '目录改名失败'))
  } finally {
    renaming.value = false
  }
}

// ---------- 移动 ----------
const moveVisible = ref(false)
const moving = ref(false)
const moveTarget = ref<DirectoryNodeVO | null>(null)
const moveForm = reactive<{ parentId: Id | '' }>({ parentId: '' })

/** 目标父目录候选：排除自身与自身子树，避免移动到自身/子目录。 */
function filterTree(nodes: DirectoryNodeVO[], excludeId: Id): DirectoryNodeVO[] {
  return nodes
    .filter((node) => node.id !== excludeId)
    .map((node) => ({
      ...node,
      children: node.children ? filterTree(node.children, excludeId) : []
    }))
}

const moveCandidates = computed(() =>
  moveTarget.value ? filterTree(props.data, moveTarget.value.id) : props.data
)

function openMove(node: DirectoryNodeVO): void {
  closeMenu()
  moveTarget.value = node
  moveForm.parentId = ''
  moveVisible.value = true
}

async function submitMove(): Promise<void> {
  const target = moveTarget.value
  if (!target) return
  const body: DirectoryMoveRequest = { parentId: moveForm.parentId === '' ? null : moveForm.parentId }
  moving.value = true
  try {
    await ceamsApi.doc.directories.move(target.id, body)
    ElMessage.success('目录已移动')
    moveVisible.value = false
    emit('refresh')
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '目录移动失败'))
  } finally {
    moving.value = false
  }
}

// ---------- 删除 ----------
async function onDelete(node: DirectoryNodeVO): Promise<void> {
  closeMenu()
  const confirmed = await ElMessageBox.confirm(
    `确认删除目录「${node.name}」？目录非空时不可删除。`,
    '删除确认',
    { type: 'warning' }
  ).catch(() => false)
  if (!confirmed) return
  try {
    await ceamsApi.doc.directories.remove(node.id)
    ElMessage.success('目录已删除')
    emit('refresh')
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '删除目录失败'))
  }
}
</script>

<template>
  <div class="directory-tree">
    <div class="tree-header">
      <span class="tree-title">目录</span>
      <el-button v-if="canCreateRoot" link type="primary" size="small" @click="openCreate(null)">
        新建根目录
      </el-button>
      <span v-else-if="libraryType === 'PROJECT' && !projectId" class="tree-hint">
        请先选择项目
      </span>
    </div>

    <el-scrollbar class="tree-body">
      <el-tree
        v-loading="loading"
        :data="data"
        node-key="id"
        default-expand-all
        highlight-current
        :expand-on-click-node="false"
        :props="{ label: 'name', children: 'children' }"
        empty-text="当前库暂无目录"
        @node-click="onNodeClick"
        @node-contextmenu="onNodeContextMenu"
      />
    </el-scrollbar>

    <!-- 右键菜单 -->
    <div
      v-if="menuVisible && menuNode"
      class="context-menu"
      :style="menuStyle"
      @click.stop
    >
      <el-button link type="primary" size="small" @click="openCreate(menuNode)">新建子目录</el-button>
      <el-button link type="primary" size="small" @click="openRename(menuNode)">改名</el-button>
      <el-button link type="primary" size="small" @click="openMove(menuNode)">移动</el-button>
      <el-button link type="danger" size="small" @click="onDelete(menuNode)">删除</el-button>
    </div>

    <!-- 新建目录 -->
    <el-dialog v-model="createVisible" class="doc-dialog" title="新建目录" width="460px">
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="上级目录">
          <span>{{ createParent ? createParent.name : '库根' }}</span>
        </el-form-item>
        <el-form-item label="目录名称" required>
          <el-input v-model="createForm.name" placeholder="请输入目录名称" maxlength="60" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="createForm.sortOrder" :min="0" :precision="0" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>

    <!-- 改名 -->
    <el-dialog v-model="renameVisible" class="doc-dialog" title="目录改名" width="460px">
      <el-form :model="renameForm" label-width="90px">
        <el-form-item label="目录名称" required>
          <el-input v-model="renameForm.name" placeholder="请输入目录名称" maxlength="60" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameVisible = false">取消</el-button>
        <el-button type="primary" :loading="renaming" @click="submitRename">确定</el-button>
      </template>
    </el-dialog>

    <!-- 移动 -->
    <el-dialog v-model="moveVisible" class="doc-dialog" title="移动目录" width="460px">
      <el-form :model="moveForm" label-width="90px">
        <el-form-item label="目标父目录">
          <el-tree-select
            v-model="moveForm.parentId"
            :data="moveCandidates"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            placeholder="留空表示移动到库根"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="moveVisible = false">取消</el-button>
        <el-button type="primary" :loading="moving" @click="submitMove">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.directory-tree {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 0;
}

/* 面板头部：标题与 12px 下间距 */
.tree-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 4px var(--ceams-space-3);
}

.tree-title {
  font-size: var(--ceams-font-size-base);
  font-weight: 600;
  color: var(--ceams-text-primary);
}

/* 项目库未选项目时的提示 */
.tree-hint {
  font-size: var(--ceams-font-size-sm);
  color: var(--ceams-text-placeholder);
}

.tree-body {
  flex: 1;
  min-height: 0;
}

/* 节点行高与圆角 */
.directory-tree :deep(.el-tree-node__content) {
  height: 34px;
  border-radius: var(--ceams-radius-sm);
}

/* 选中节点：浅蓝底 + 主色文字 */
.directory-tree :deep(.el-tree-node.is-current > .el-tree-node__content) {
  background-color: var(--ceams-color-primary-light);
  color: var(--ceams-color-primary);
  font-weight: 600;
}

.directory-tree :deep(.el-tree__empty-text) {
  color: var(--ceams-text-placeholder);
}

/* 右键菜单：白底圆角浮层 */
.context-menu {
  position: fixed;
  z-index: 3000;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 2px;
  min-width: 120px;
  padding: var(--ceams-space-1) var(--ceams-space-2);
  background: var(--ceams-bg-card);
  border: 1px solid var(--ceams-border-base);
  border-radius: var(--ceams-radius-md);
  box-shadow: var(--ceams-shadow-raised);
}

.context-menu :deep(.el-button + .el-button) {
  margin-left: 0;
}

.context-menu :deep(.el-button.is-link) {
  justify-content: flex-start;
}

/* 弹窗内容 20px 24px */
:global(.doc-dialog .el-dialog__body) {
  padding: 20px 24px;
}
</style>
