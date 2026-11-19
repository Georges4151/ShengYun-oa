<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  ElBreadcrumb,
  ElBreadcrumbItem,
  ElButton,
  ElDialog,
  ElEmpty,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElPagination,
  ElRadioButton,
  ElRadioGroup,
  ElSelect,
  ElTable,
  ElTableColumn
} from 'element-plus'
import { formatDateTime } from '@ceams/utils'
import { apiBaseUrl, ceamsApi, fetchBlob } from '@/services/http'
import type {
  DirectoryNodeVO,
  DocFileVO,
  DocVersionVO,
  Id,
  LibraryType,
  ProjectVO,
  UserVO
} from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { hasPermission } from '@/composables/usePermission'
import { apiErrorMessage } from '@/utils/error'
import { docVersionSourceLabels, libraryTypeLabels } from '@/utils/labels'
import DirectoryTree from './components/DirectoryTree.vue'
import FileTable from './components/FileTable.vue'
import UploadDialog from './components/UploadDialog.vue'
import DocEditor from './components/DocEditor.vue'
import PermissionDrawer from './components/PermissionDrawer.vue'

defineOptions({ name: 'DocView' })

const route = useRoute()

const libraryOptions: Array<{ value: LibraryType; label: string }> = [
  { value: 'PERSONAL', label: libraryTypeLabels.PERSONAL },
  { value: 'DEPT', label: libraryTypeLabels.DEPT },
  { value: 'PROJECT', label: libraryTypeLabels.PROJECT },
  { value: 'PUBLIC', label: libraryTypeLabels.PUBLIC }
]

const canManageDir = computed(() => hasPermission('doc:dir:manage'))
const canUpload = computed(() => hasPermission('doc:file:upload'))
const canEditFile = computed(() => hasPermission('doc:file:edit'))
const canDeleteFile = computed(() => hasPermission('doc:file:delete'))
const canManagePermission = computed(() => hasPermission('doc:permission:manage'))

/** Office 扩展名（与后端编辑器支持的 6 种一致，其余走浏览器预览 / 下载）。 */
const OFFICE_EXTENSIONS = ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx']

function isOfficeFile(row: DocFileVO): boolean {
  return OFFICE_EXTENSIONS.includes((row.ext ?? '').toLowerCase())
}

// ---------- 库与目录 ----------
const libraryType = ref<LibraryType>('PERSONAL')
const projectId = ref<Id | ''>('')
const projects = ref<ProjectVO[]>([])
const tree = ref<DirectoryNodeVO[]>([])
const treeLoading = ref(false)
const currentDirId = ref<Id | null>(null)

/** 目录路径（用于面包屑）：DFS 查找当前节点及其祖先链。 */
function findPath(nodes: DirectoryNodeVO[], id: Id): DirectoryNodeVO[] {
  for (const node of nodes) {
    if (node.id === id) return [node]
    if (node.children && node.children.length > 0) {
      const childPath = findPath(node.children, id)
      if (childPath.length > 0) return [node, ...childPath]
    }
  }
  return []
}

const breadcrumb = computed(() =>
  currentDirId.value ? findPath(tree.value, currentDirId.value) : []
)

const currentDirName = computed(() => {
  const path = breadcrumb.value
  return path.length > 0 ? path[path.length - 1].name : ''
})

async function loadTree(): Promise<void> {
  treeLoading.value = true
  try {
    tree.value = await ceamsApi.doc.directories.tree({
      libraryType: libraryType.value,
      ownerId: libraryType.value === 'PROJECT' ? projectId.value || undefined : undefined
    })
    // 目录被删除或库切换后，清理失效的选中项
    if (currentDirId.value && findPath(tree.value, currentDirId.value).length === 0) {
      currentDirId.value = null
      files.value = []
      total.value = 0
    }
  } catch (err) {
    tree.value = []
    ElMessage.error(apiErrorMessage(err, '加载目录树失败'))
  } finally {
    treeLoading.value = false
  }
}

async function loadProjects(): Promise<void> {
  if (projects.value.length > 0) return
  try {
    const res = await ceamsApi.project.projectPage({ page: 1, size: 200 })
    projects.value = res.records
  } catch {
    // 项目下拉加载失败不阻断其他库
  }
}

function onLibraryChange(): void {
  currentDirId.value = null
  keyword.value = ''
  files.value = []
  total.value = 0
  resetPage()
  if (libraryType.value === 'PROJECT') {
    void loadProjects()
  } else {
    projectId.value = ''
  }
  void loadTree()
}

function onProjectChange(): void {
  currentDirId.value = null
  files.value = []
  total.value = 0
  resetPage()
  void loadTree()
}

function onSelectDir(node: DirectoryNodeVO): void {
  currentDirId.value = node.id
  resetPage()
  void loadFiles()
}

// ---------- 文件列表 ----------
const keyword = ref('')
const files = ref<DocFileVO[]>([])
const loading = ref(false)
const { page, size, total, resetPage } = usePage()
const userNames = ref<Record<string, string>>({})

async function loadUsers(): Promise<void> {
  try {
    const res = await ceamsApi.iam.userPage({ page: 1, size: 200 })
    const map: Record<string, string> = {}
    res.records.forEach((item: UserVO) => {
      map[item.id] = item.name
    })
    userNames.value = map
  } catch {
    // 上传人姓名加载失败时回退显示用户 ID
  }
}

async function loadFiles(): Promise<void> {
  if (!currentDirId.value) {
    files.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const res = await ceamsApi.doc.files.list({
      dirId: currentDirId.value,
      keyword: keyword.value.trim() || undefined,
      page: page.value,
      size: size.value
    })
    files.value = res.records
    total.value = res.total
  } catch (err) {
    files.value = []
    total.value = 0
    ElMessage.error(apiErrorMessage(err, '加载文件列表失败'))
  } finally {
    loading.value = false
  }
}

function onSearch(): void {
  resetPage()
  void loadFiles()
}

// ---------- 上传 ----------
const uploadVisible = ref(false)

function openUpload(): void {
  if (!currentDirId.value) {
    ElMessage.warning('请先选择左侧目录')
    return
  }
  uploadVisible.value = true
}

function onUploaded(): void {
  uploadVisible.value = false
  void loadFiles()
}

// ---------- 预览 / 下载 ----------
/** 预览：Office 走 OnlyOffice 只读（mode=view），其余用带 Authorization 的 fetch 取 Blob 后以 object URL 打开。 */
async function onPreview(row: DocFileVO): Promise<void> {
  if (isOfficeFile(row)) {
    editorFileId.value = row.id
    editorMode.value = 'view'
    editorVisible.value = true
    return
  }
  try {
    const blob = await fetchBlob(ceamsApi.doc.files.previewUrl(row.id))
    const url = URL.createObjectURL(blob)
    window.open(url, '_blank')
    window.setTimeout(() => URL.revokeObjectURL(url), 60_000)
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '预览失败'))
  }
}

async function onDownload(row: DocFileVO): Promise<void> {
  try {
    const token = await ceamsApi.doc.files.issueDownloadToken(row.id)
    window.open(`${apiBaseUrl}${ceamsApi.doc.files.downloadUrl(row.id, token)}`, '_blank')
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '下载失败'))
  }
}

// ---------- 在线编辑 ----------
const editorVisible = ref(false)
const editorFileId = ref<Id | null>(null)
const editorMode = ref<'edit' | 'view'>('view')

function onEdit(row: DocFileVO): void {
  editorFileId.value = row.id
  editorMode.value = 'edit'
  editorVisible.value = true
}

// ---------- 版本 ----------
const versionVisible = ref(false)
const versionLoading = ref(false)
const versionFile = ref<DocFileVO | null>(null)
const versions = ref<DocVersionVO[]>([])

async function onVersions(row: DocFileVO): Promise<void> {
  versionFile.value = row
  versionVisible.value = true
  await loadVersions()
}

async function loadVersions(): Promise<void> {
  if (!versionFile.value) return
  versionLoading.value = true
  try {
    versions.value = await ceamsApi.doc.versions.list(versionFile.value.id)
  } catch (err) {
    versions.value = []
    ElMessage.error(apiErrorMessage(err, '加载版本列表失败'))
  } finally {
    versionLoading.value = false
  }
}

async function onRollback(version: DocVersionVO): Promise<void> {
  const file = versionFile.value
  if (!file) return
  const confirmed = await ElMessageBox.confirm(
    `确认回滚到 v${version.versionNo}？回滚会以该版本内容生成新版本，历史版本保留。`,
    '版本回滚',
    { type: 'warning' }
  ).catch(() => false)
  if (!confirmed) return
  try {
    await ceamsApi.doc.versions.rollback(file.id, version.versionNo)
    ElMessage.success(`已回滚到 v${version.versionNo}`)
    await Promise.all([loadVersions(), loadFiles()])
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '版本回滚失败'))
  }
}

// ---------- 授权 ----------
const permissionVisible = ref(false)
const permissionFile = ref<DocFileVO | null>(null)

function onPermission(row: DocFileVO): void {
  permissionFile.value = row
  permissionVisible.value = true
}

// ---------- 删除 ----------
async function onRemove(row: DocFileVO): Promise<void> {
  const confirmed = await ElMessageBox.confirm(
    `确认删除文件「${row.name}」？删除后不可恢复。`,
    '删除确认',
    { type: 'warning' }
  ).catch(() => false)
  if (!confirmed) return
  try {
    await ceamsApi.doc.files.remove(row.id)
    ElMessage.success('文件已删除')
    await loadFiles()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '删除文件失败'))
  }
}

/**
 * 深链打开：/doc?fileId=&dirId=&libraryType=
 * 用于 AI 助手引用来源点击后定位目录并自动预览文件。
 */
async function openFromQuery(): Promise<boolean> {
  const fileId = typeof route.query.fileId === 'string' ? route.query.fileId : ''
  const dirId = typeof route.query.dirId === 'string' ? route.query.dirId : ''
  if (!fileId || !dirId) return false
  const lt = typeof route.query.libraryType === 'string' ? route.query.libraryType : ''
  const matched = libraryOptions.find((opt) => opt.value === lt)
  if (matched && libraryType.value !== matched.value) {
    libraryType.value = matched.value
    if (matched.value === 'PROJECT') void loadProjects()
  }
  currentDirId.value = null
  await loadTree()
  if (findPath(tree.value, dirId).length === 0) return false
  currentDirId.value = dirId
  await loadFiles()
  const target = files.value.find((item) => item.id === fileId)
  if (target) await onPreview(target)
  return true
}

onMounted(async () => {
  void loadUsers()
  const opened = await openFromQuery()
  if (!opened) void loadTree()
})
</script>

<template>
  <div class="doc-view">
    <!-- 页头：标题 + 灰色说明 + 右侧主操作 -->
    <div class="page-header">
      <div>
        <h2 class="page-title">文档知识库</h2>
        <p class="page-subtitle">按个人、部门、项目与公共库分类管理目录与文件</p>
      </div>
      <el-button v-if="canUpload" type="primary" @click="openUpload">上传文件</el-button>
    </div>

    <!-- 库切换 -->
    <div class="library-bar">
      <el-radio-group v-model="libraryType" @change="onLibraryChange">
        <el-radio-button v-for="opt in libraryOptions" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </el-radio-button>
      </el-radio-group>
      <el-select
        v-if="libraryType === 'PROJECT'"
        v-model="projectId"
        placeholder="请选择项目"
        filterable
        clearable
        style="width: 260px"
        @change="onProjectChange"
      >
        <el-option
          v-for="item in projects"
          :key="item.id"
          :label="item.projectName"
          :value="item.id"
        />
      </el-select>
    </div>

    <div class="doc-body">
      <!-- 左侧目录树面板 -->
      <div class="tree-pane">
        <DirectoryTree
          :data="tree"
          :loading="treeLoading"
          :library-type="libraryType"
          :selected-id="currentDirId"
          :project-id="projectId || null"
          :can-manage="canManageDir"
          @select="onSelectDir"
          @refresh="loadTree"
        />
      </div>

      <div class="content-pane">
        <el-breadcrumb separator="/" class="breadcrumb">
          <el-breadcrumb-item v-if="breadcrumb.length === 0">未选择目录</el-breadcrumb-item>
          <el-breadcrumb-item v-for="node in breadcrumb" :key="node.id">
            {{ node.name }}
          </el-breadcrumb-item>
        </el-breadcrumb>

        <!-- 筛选区：白色圆角卡片，按钮靠右 -->
        <div class="filter-bar">
          <el-input
            v-model="keyword"
            placeholder="文件名搜索"
            clearable
            style="width: 220px"
            @keyup.enter="onSearch"
          />
          <div class="filter-actions">
            <el-button type="primary" @click="onSearch">查询</el-button>
          </div>
        </div>

        <template v-if="currentDirId">
          <!-- 文件列表卡片 -->
          <div class="data-card file-card">
            <div class="card-header">
              <div class="card-heading">
                <span class="card-title">文件列表</span>
                <span class="card-count">共 {{ total }} 个文件</span>
              </div>
            </div>

            <FileTable
              :files="files"
              :loading="loading"
              :user-names="userNames"
              :can-edit="canEditFile"
              :can-delete="canDeleteFile"
              :can-permission="canManagePermission"
              @preview="onPreview"
              @download="onDownload"
              @edit="onEdit"
              @versions="onVersions"
              @permission="onPermission"
              @remove="onRemove"
            />
            <!-- 分页右对齐，距表 16px -->
            <el-pagination
              v-model:current-page="page"
              v-model:page-size="size"
              :total="total"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              class="pagination"
              @size-change="onSearch"
              @current-change="loadFiles"
            />
          </div>
        </template>
        <!-- 空态：居中灰色、40px 留白 -->
        <div v-else class="empty-state">
          <el-empty description="请选择左侧目录查看文件" />
        </div>
      </div>
    </div>

    <!-- 上传 -->
    <UploadDialog
      v-model="uploadVisible"
      :dir-id="currentDirId"
      :dir-name="currentDirName"
      @uploaded="onUploaded"
    />

    <!-- 在线编辑 / 预览 -->
    <DocEditor
      v-model="editorVisible"
      :file-id="editorFileId"
      :mode="editorMode"
    />

    <!-- 授权 -->
    <PermissionDrawer
      v-model="permissionVisible"
      :file-id="permissionFile?.id ?? null"
      :file-name="permissionFile?.name ?? ''"
    />

    <!-- 版本 -->
    <el-dialog v-model="versionVisible" class="doc-dialog" title="版本历史" width="760px">
      <div class="version-tip">文件：{{ versionFile?.name ?? '-' }}</div>
      <el-table v-loading="versionLoading" :data="versions" border size="small">
        <el-table-column label="版本" width="90">
          <template #default="{ row }">v{{ (row as DocVersionVO).versionNo }}</template>
        </el-table-column>
        <el-table-column label="来源" width="110">
          <template #default="{ row }">
            {{ docVersionSourceLabels[(row as DocVersionVO).source] ?? (row as DocVersionVO).source }}
          </template>
        </el-table-column>
        <el-table-column label="变更说明" min-width="150">
          <template #default="{ row }">{{ (row as DocVersionVO).changeNote ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="上传人" width="100">
          <template #default="{ row }">
            {{ (row as DocVersionVO).uploadedByName || userNames[(row as DocVersionVO).uploadedBy ?? ''] || ((row as DocVersionVO).uploadedBy ?? '-') }}
          </template>
        </el-table-column>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDateTime((row as DocVersionVO).createTime) }}</template>
        </el-table-column>
        <el-table-column v-if="canEditFile" label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="onRollback(row as DocVersionVO)">
              回滚
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="versionVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.doc-view {
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* 页头与下方内容保持 16px 间距 */
.page-header {
  margin-bottom: var(--ceams-space-4);
}

/* 库切换条 */
.library-bar {
  display: flex;
  align-items: center;
  gap: var(--ceams-space-3);
  margin-bottom: var(--ceams-space-4);
}

.doc-body {
  display: flex;
  flex: 1;
  gap: var(--ceams-space-4);
  min-height: 0;
}

/* 左侧目录树面板：白底圆角卡片 */
.tree-pane {
  width: 260px;
  flex: none;
  padding: var(--ceams-space-4);
  overflow: hidden;
  background: var(--ceams-bg-card);
  border: 1px solid var(--ceams-border-base);
  border-radius: var(--ceams-radius-lg);
}

/* 右侧内容区：面包屑 + 筛选 + 文件卡片，16px 间距 */
.content-pane {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: var(--ceams-space-4);
  min-width: 0;
}

.breadcrumb {
  margin-bottom: 0;
}

/* 筛选卡片：16px 20px 内边距，按钮靠右 */
.filter-bar {
  padding: 16px 20px;
  margin-bottom: 0;
  gap: var(--ceams-space-3);
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: var(--ceams-space-3);
  margin-left: auto;
}

/* 文件列表卡片：20px 内边距 */
.file-card {
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
.empty-state {
  padding: var(--ceams-space-10) 0;
}

.empty-state :deep(.el-empty__description p) {
  color: var(--ceams-text-placeholder);
}

/* 版本历史弹窗提示文案 */
.version-tip {
  margin-bottom: var(--ceams-space-3);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-sm);
}

/* 弹窗内容 20px 24px */
:global(.doc-dialog .el-dialog__body) {
  padding: 20px 24px;
}
</style>
