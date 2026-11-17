<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
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

onMounted(() => {
  void loadUsers()
  void loadTree()
})
</script>

<template>
  <div class="doc-view">
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

        <div class="toolbar">
          <el-input
            v-model="keyword"
            placeholder="文件名搜索"
            clearable
            style="width: 220px"
            @keyup.enter="onSearch"
          />
          <el-button @click="onSearch">查询</el-button>
          <el-button v-if="canUpload" type="primary" @click="openUpload">上传文件</el-button>
        </div>

        <template v-if="currentDirId">
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
        </template>
        <el-empty v-else description="请选择左侧目录查看文件" />
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
    <el-dialog v-model="versionVisible" title="版本历史" width="760px">
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
            {{ userNames[(row as DocVersionVO).uploadedBy ?? ''] ?? ((row as DocVersionVO).uploadedBy ?? '-') }}
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
.library-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.doc-body {
  display: flex;
  flex: 1;
  gap: 12px;
  min-height: 0;
}
.tree-pane {
  width: 260px;
  flex: none;
  padding: 12px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}
.content-pane {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  padding: 12px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}
.breadcrumb {
  margin-bottom: 12px;
}
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
.version-tip {
  margin-bottom: 12px;
  color: #606266;
  font-size: 13px;
}
</style>
