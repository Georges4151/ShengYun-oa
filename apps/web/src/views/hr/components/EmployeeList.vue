<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import {
  ElButton,
  ElDatePicker,
  ElDialog,
  ElDrawer,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElOption,
  ElPagination,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag,
  ElTimeline,
  ElTimelineItem,
  ElTooltip,
  ElTree,
  ElTreeSelect
} from 'element-plus'
import type { FormInstance, FormRules, LoadFunction, TreeInstance } from 'element-plus'
import { ceamsApi } from '@/services/http'
import BpmFlowPreviewDialog from '@/views/bpm/components/BpmFlowPreviewDialog.vue'
import type {
  DeptVO,
  EmployeeChangeRequest,
  EmployeeChangeVO,
  EmployeeUpdateRequest,
  EmployeeVO,
  HrChangeType,
  HrEmployeeStatus,
  HrEmploymentType,
  HrGender,
  JobLevelVO,
  OrgTreeNodeVO,
  PositionVO
} from '@ceams/types'
import { usePage } from '@/composables/usePage'
import { hasPermission } from '@/composables/usePermission'
import { apiErrorMessage } from '@/utils/error'
import {
  hrApprovalStatusLabels,
  hrChangeTypeLabels,
  hrEmployeeStatusLabels,
  hrEmploymentTypeLabels,
  hrGenderLabels,
  toOptions
} from '@/utils/labels'

defineOptions({ name: 'EmployeeList' })

const statusOptions = [
  ...toOptions(hrEmployeeStatusLabels),
  { label: '全部员工', value: 'ALL' }
]
const genderOptions = toOptions(hrGenderLabels)
const employmentTypeOptions = toOptions(hrEmploymentTypeLabels)

const query = reactive<{
  keyword: string
  deptId: string
  status: HrEmployeeStatus | 'ALL' | ''
}>({ keyword: '', deptId: '', status: '' })
const { page, size, total, resetPage } = usePage()
const list = ref<EmployeeVO[]>([])
const loading = ref(false)

/** 部门下拉（展平组织树，label 保留层级缩进） */
interface DeptOption {
  value: string
  label: string
  name: string
}
const deptOptions = ref<DeptOption[]>([])
const positionOptions = ref<PositionVO[]>([])
/** 组织树全量数据（关键字模式展示 + 上级候选树），顺序完全由后端决定 */
const orgTree = ref<OrgTreeNodeVO[]>([])
const orgTreeLoading = ref(false)
const orgTreeRef = ref<TreeInstance | null>(null)
const treeProps = { label: 'name', children: 'children', isLeaf: 'isLeaf' }
/** 懒加载节点：额外携带 isLeaf，避免无下级的节点渲染展开箭头 */
interface OrgTreeRenderNode extends OrgTreeNodeVO {
  isLeaf?: boolean
}
/** 懒加载模式的根节点缓存（供「展开一级」「全部收起」使用） */
const lazyRoots = ref<OrgTreeRenderNode[]>([])
/** lazy：关键字为空时按需加载；full：关键字过滤时一次性展示命中路径 */
const treeMode = ref<'lazy' | 'full'>('lazy')
/** 变更 key 强制重建 el-tree，避免切换模式后残留 lazy / 展开状态 */
const treeEpoch = ref(0)
/** 已生效的关键字（回车 / 查询后才刷新树，输入过程中不打断） */
const appliedKeyword = ref('')
/** 重建后是否自动展开：懒模式展开根部门，全量模式展开全部命中路径 */
const autoExpand = ref(true)
/** 树请求序号：快速切换筛选时丢弃过期响应 */
let treeLoadSeq = 0
/** 组织架构全屏展示 */
const treeFullscreen = ref(false)

const treeEmpty = computed(() =>
  treeMode.value === 'lazy' ? lazyRoots.value.length === 0 : orgTree.value.length === 0
)
const deptNameMap = computed(() => new Map(deptOptions.value.map((item) => [item.value, item.name])))
const positionNameMap = computed(
  () => new Map(positionOptions.value.map((item) => [item.id, item.name]))
)

function flattenDept(nodes: DeptVO[], depth = 0, result: DeptOption[] = []): DeptOption[] {
  for (const node of nodes) {
    result.push({ value: node.id, label: `${'　'.repeat(depth)}${node.name}`, name: node.name })
    if (node.children?.length) flattenDept(node.children, depth + 1, result)
  }
  return result
}

function deptName(id?: string | null): string {
  if (!id) return '-'
  return deptNameMap.value.get(id) ?? id
}

function positionName(id?: string | null): string {
  if (!id) return '-'
  return positionNameMap.value.get(id) ?? id
}

function statusTagType(status: HrEmployeeStatus): 'success' | 'warning' | 'info' | 'danger' {
  switch (status) {
    case 'ACTIVE':
      return 'success'
    case 'PROBATION':
      return 'warning'
    case 'LEAVE':
      return 'info'
    default:
      return 'danger'
  }
}

function approvalTagType(
  status: EmployeeChangeVO['approvalStatus']
): 'success' | 'warning' | 'info' | 'danger' {
  switch (status) {
    case 'APPROVED':
      return 'success'
    case 'PENDING':
      return 'warning'
    case 'REJECTED':
      return 'danger'
    default:
      return 'info'
  }
}

async function loadDepartments(): Promise<void> {
  try {
    const tree = await ceamsApi.iam.deptTree()
    deptOptions.value = flattenDept(tree)
  } catch {
    // 部门树加载失败不阻断员工列表
  }
}

/** 组织树节点行样式：部门加粗、离职员工置灰。 */
function nodeRowClass(node: OrgTreeNodeVO): Record<string, boolean> {
  return {
    'org-node--dept': node.nodeType !== 'EMPLOYEE',
    'is-dimmed': node.nodeType === 'EMPLOYEE' && node.status === 'TERMINATED'
  }
}

function crossLeaderTip(node: OrgTreeNodeVO): string {
  return `${node.leaderName || '未知'}（${node.leaderDeptName || '未知部门'}）`
}

/** 是否有下级：懒加载接口只给 hasChildren，全量接口用 children 判断。 */
function hasChildNode(node: OrgTreeNodeVO): boolean {
  return node.hasChildren ?? Boolean(node.children?.length)
}

/** 部门人数徽标提示：在职人数 / 含离职总人数。 */
function headcountTip(node: OrgTreeNodeVO): string {
  const active = node.headcount ?? 0
  return `在职 ${active} 人 / 含离职共 ${node.totalHeadcount ?? active} 人`
}

/** 懒加载节点补充 isLeaf，避免无下级节点渲染出可点击的展开箭头。 */
function toTreeData(node: OrgTreeNodeVO): OrgTreeRenderNode {
  const result: OrgTreeRenderNode = { ...node }
  if (node.hasChildren !== undefined) result.isLeaf = !node.hasChildren
  return result
}

/** 展开懒加载根部门（只展开一级，不递归）。 */
function expandRootNodes(): void {
  const tree = orgTreeRef.value
  if (!tree) return
  for (const node of lazyRoots.value) {
    if (node.nodeType === 'EMPLOYEE' || node.isLeaf) continue
    tree.getNode(node.id)?.expand()
  }
}

/** 展开全量树中所有带下级的节点（关键字命中路径全部可见）。 */
function expandLoadedNodes(nodes: OrgTreeNodeVO[]): void {
  const tree = orgTreeRef.value
  if (!tree) return
  for (const node of nodes) {
    if (!node.children?.length) continue
    tree.getNode(node.id)?.expand()
    expandLoadedNodes(node.children)
  }
}

/** 收起全量树中所有节点，保留数据避免重新请求。 */
function collapseLoadedNodes(nodes: OrgTreeNodeVO[]): void {
  const tree = orgTreeRef.value
  if (!tree) return
  for (const node of nodes) {
    tree.getNode(node.id)?.collapse()
    if (node.children?.length) collapseLoadedNodes(node.children)
  }
}

/** el-tree 懒加载：level 0 取根节点，其余按父节点类型 / ID 取直接子节点。 */
const lazyLoadNode: LoadFunction = (node, resolve) => {
  const parent = node.level > 0 && node.data ? (node.data as OrgTreeNodeVO) : null
  if (!parent) {
    orgTreeLoading.value = true
    const seq = treeLoadSeq
    void (async () => {
      try {
        const roots = await ceamsApi.hr.orgTreeRoots({
          status: treeStatus.value,
          keyword: appliedKeyword.value || undefined,
          includeResigned: treeIncludeResigned.value
        })
        if (seq !== treeLoadSeq) {
          resolve([])
          return
        }
        lazyRoots.value = roots.map(toTreeData)
        resolve(lazyRoots.value)
        await nextTick()
        if (seq === treeLoadSeq && autoExpand.value) expandRootNodes()
      } catch {
        if (seq === treeLoadSeq) lazyRoots.value = []
        resolve([])
      } finally {
        if (seq === treeLoadSeq) orgTreeLoading.value = false
      }
    })()
    return
  }
  void (async () => {
    try {
      const children = await ceamsApi.hr.orgTreeChildren({
        nodeType: parent.nodeType,
        nodeId: parent.id,
        status: treeStatus.value,
        keyword: appliedKeyword.value || undefined,
        includeResigned: treeIncludeResigned.value
      })
      resolve(children.map(toTreeData))
    } catch {
      resolve([])
    }
  })()
}

/** 关键字模式：一次性加载过滤后的完整树，并展开全部命中路径。 */
async function loadFullTree(seq: number): Promise<void> {
  orgTreeLoading.value = true
  try {
    const data = await ceamsApi.hr.orgTree({
      status: treeStatus.value,
      keyword: appliedKeyword.value || undefined,
      includeResigned: treeIncludeResigned.value
    })
    if (seq !== treeLoadSeq) return
    orgTree.value = data
    await nextTick()
    if (seq === treeLoadSeq && autoExpand.value) expandLoadedNodes(data)
  } catch {
    // 组织树加载失败不阻断员工列表，保留上一次数据
  } finally {
    if (seq === treeLoadSeq) orgTreeLoading.value = false
  }
}

/** 重新加载组织树：关键字为空走懒加载，非空走全量过滤；切换模式时重建树实例。 */
async function reloadTree(): Promise<void> {
  treeLoadSeq += 1
  const seq = treeLoadSeq
  const nextMode: 'lazy' | 'full' = appliedKeyword.value ? 'full' : 'lazy'
  treeMode.value = nextMode
  treeEpoch.value += 1
  lazyRoots.value = []
  autoExpand.value = true
  if (nextMode === 'full') await loadFullTree(seq)
}

// ---------- 组织树工具栏 ----------
function refreshTree(): void {
  void reloadTree()
}

function collapseTree(): void {
  autoExpand.value = false
  if (treeMode.value === 'lazy') {
    const tree = orgTreeRef.value
    for (const node of lazyRoots.value) tree?.getNode(node.id)?.collapse()
    return
  }
  collapseLoadedNodes(orgTree.value)
}

function expandFirstLevel(): void {
  autoExpand.value = true
  expandRootNodes()
}

function expandFullTree(): void {
  autoExpand.value = true
  expandLoadedNodes(orgTree.value)
}

/** 抽屉中的上级候选树：懒加载模式下按需拉取一次全量组织树。 */
async function ensureLeaderTree(): Promise<void> {
  if (treeMode.value !== 'lazy' || orgTree.value.length) return
  try {
    // 上级候选固定取未离职员工全量树，不受列表状态筛选影响
    orgTree.value = await ceamsApi.hr.orgTree({})
  } catch {
    // 上级候选加载失败不阻断表单
  }
}

/** 组织树状态参数：'ALL'（全部员工）不限状态，仅额外包含离职员工。 */
const treeStatus = computed<HrEmployeeStatus | undefined>(() =>
  query.status && query.status !== 'ALL' ? query.status : undefined
)
/** 仅当筛选“全部员工”时组织树才包含已离职员工。 */
const treeIncludeResigned = computed(() => query.status === 'ALL')

async function loadPositions(): Promise<void> {
  try {
    positionOptions.value = await ceamsApi.hr.positionList()
  } catch {
    // 岗位列表加载失败不阻断员工列表
  }
}

async function loadLevels(): Promise<void> {
  try {
    // 员工档案只选择启用中的职级；字典维护页仍使用全量列表
    levelOptions.value = await ceamsApi.hr.jobLevelEnabledList()
  } catch {
    // 职级列表加载失败不阻断员工列表
  }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await ceamsApi.hr.employeePage({
      page: page.value,
      size: size.value,
      keyword: query.keyword || undefined,
      deptId: query.deptId || undefined,
      status: query.status && query.status !== 'ALL' ? query.status : undefined
    })
    list.value = res.records
    total.value = res.total
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载员工档案失败'))
  } finally {
    loading.value = false
  }
}

function onSearch(): void {
  appliedKeyword.value = query.keyword.trim()
  resetPage()
  void load()
  void reloadTree()
}

/** 部门下拉变化：只刷新右侧列表，树保持当前展开状态并高亮所选部门。 */
function onDeptChange(): void {
  resetPage()
  orgTreeRef.value?.setCurrentKey(query.deptId || null, true)
  void load()
}

/** 每页条数变化：回到第 1 页重新拉取列表，树不变。 */
function onPageSizeChange(): void {
  resetPage()
  void load()
}

// ---------- 左侧组织树交互 ----------
/** 部门节点：右侧列表按该部门及所有子部门筛选并回到第 1 页，同时展开该节点。 */
function onTreeNodeClick(node: OrgTreeNodeVO): void {
  const tree = orgTreeRef.value
  tree?.setCurrentKey(node.id, true)
  if (node.nodeType === 'EMPLOYEE') {
    void openEmployeeDetail(node)
    return
  }
  if (node.nodeType !== 'DEPT') return
  query.deptId = node.id
  resetPage()
  tree?.getNode(node.id)?.expand()
  void load()
}

/** 员工节点：复用现有详情 / 编辑抽屉，不改变右侧筛选条件。 */
async function openEmployeeDetail(node: OrgTreeNodeVO): Promise<void> {
  try {
    openEdit(await ceamsApi.hr.employeeDetail(node.id))
  } catch {
    // 详情接口不可用时以树节点数据兜底，保证抽屉仍可打开
    openEdit({
      id: node.id,
      employeeNo: node.employeeNo ?? '',
      name: node.name,
      deptId: node.deptId ?? null,
      leaderId: node.leaderId ?? null,
      leaderName: node.leaderName ?? null,
      levelId: node.levelId ?? null,
      levelName: node.levelName ?? null,
      levelCode: node.levelCode ?? null,
      status: node.status ?? 'ACTIVE'
    })
  }
}

/** 跨部门上级标签：展开并选中上级员工节点；懒加载下未加载到时提示而不报错。 */
async function locateLeader(node: OrgTreeNodeVO): Promise<void> {
  const leaderId = node.leaderId
  const tree = orgTreeRef.value
  if (!leaderId || !tree || !tree.getNode(leaderId)) {
    ElMessage.info('该上级暂未加载，请先展开其所在部门')
    return
  }
  tree.setCurrentKey(leaderId, true)
  await nextTick()
  const root = tree.$el as HTMLElement | undefined
  root?.querySelector(`[data-key="${leaderId}"]`)?.scrollIntoView({ block: 'nearest' })
}

function onCrossLeaderClick(node: OrgTreeNodeVO): void {
  void locateLeader(node)
}

function clearTreeFilter(): void {
  query.deptId = ''
  query.keyword = ''
  appliedKeyword.value = ''
  resetPage()
  orgTreeRef.value?.setCurrentKey(null)
  void load()
  void reloadTree()
}

// ---------- 新增 / 编辑抽屉 ----------
const drawerVisible = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()
const levelOptions = ref<JobLevelVO[]>([])
const form = reactive({
  userId: '',
  employeeNo: '',
  name: '',
  gender: '' as HrGender | '',
  birthDate: '',
  idCard: '',
  phone: '',
  email: '',
  deptId: '',
  positionId: '',
  leaderId: '',
  levelId: '',
  employmentType: '' as HrEmploymentType | '',
  hireDate: '',
  probationEndDate: '',
  emergencyContact: '',
  emergencyPhone: '',
  remark: ''
})

/** 从组织树中抽取员工节点（保持汇报嵌套），作为“直属上级”候选。 */
function employeeNodesOf(nodes: OrgTreeNodeVO[]): OrgTreeNodeVO[] {
  const result: OrgTreeNodeVO[] = []
  for (const node of nodes) {
    if (node.nodeType === 'EMPLOYEE') {
      const children = node.children?.length ? employeeNodesOf(node.children) : []
      if (node.status === 'TERMINATED') {
        // 已离职员工不作为上级 / 转交候选，但其下级仍需保留
        result.push(...children)
      } else {
        result.push({ ...node, children: children.length ? children : undefined })
      }
    } else if (node.children?.length) {
      result.push(...employeeNodesOf(node.children))
    }
  }
  return result
}

/** 上级候选树：编辑时剔除本人及其所有下级，避免形成环。 */
function filterLeaderTree(nodes: OrgTreeNodeVO[], excludeId: string | null): OrgTreeNodeVO[] {
  const result: OrgTreeNodeVO[] = []
  for (const node of nodes) {
    if (excludeId && node.id === excludeId) continue
    const children = node.children?.length ? filterLeaderTree(node.children, excludeId) : undefined
    result.push(children ? { ...node, children } : { ...node })
  }
  return result
}

const leaderTreeData = computed(() => filterLeaderTree(employeeNodesOf(orgTree.value), editingId.value))

/** 离职转交候选树：剔除离职员工本人与全部待转交下属（含其下级）。 */
function filterTreeExcluding(nodes: OrgTreeNodeVO[], excluded: Set<string>): OrgTreeNodeVO[] {
  const result: OrgTreeNodeVO[] = []
  for (const node of nodes) {
    if (excluded.has(node.id)) continue
    const children = node.children?.length ? filterTreeExcluding(node.children, excluded) : undefined
    result.push(children ? { ...node, children } : { ...node })
  }
  return result
}

const resignTargetTree = computed(() => {
  if (!changeEmployee.value) return []
  const excluded = new Set<string>([
    changeEmployee.value.id,
    ...resignSubordinates.value.map((item) => item.id)
  ])
  return filterTreeExcluding(employeeNodesOf(orgTree.value), excluded)
})
/** 是否具备「维护员工上下级」权限（hr:employee:leader）。 */
const canMaintainLeader = computed(() => hasPermission('hr:employee:leader'))

const rules: FormRules<typeof form> = {
  employeeNo: [{ required: true, message: '请输入员工工号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

function resetForm(): void {
  Object.assign(form, {
    userId: '',
    employeeNo: '',
    name: '',
    gender: '',
    birthDate: '',
    idCard: '',
    phone: '',
    email: '',
    deptId: '',
    positionId: '',
    leaderId: '',
    levelId: '',
    employmentType: '',
    hireDate: '',
    probationEndDate: '',
    emergencyContact: '',
    emergencyPhone: '',
    remark: ''
  })
}

function openCreate(): void {
  editingId.value = null
  resetForm()
  void ensureLeaderTree()
  drawerVisible.value = true
}

function openEdit(row: EmployeeVO): void {
  editingId.value = row.id
  resetForm()
  form.userId = row.userId ?? ''
  form.employeeNo = row.employeeNo
  form.name = row.name
  form.gender = row.gender ?? ''
  form.birthDate = row.birthDate ?? ''
  form.deptId = row.deptId ?? ''
  form.positionId = row.positionId ?? ''
  form.leaderId = row.leaderId ?? ''
  form.levelId = row.levelId ?? ''
  form.employmentType = row.employmentType ?? ''
  form.hireDate = row.hireDate ?? ''
  form.probationEndDate = row.probationEndDate ?? ''
  form.emergencyContact = row.emergencyContact ?? ''
  form.remark = row.remark ?? ''
  // 身份证 / 手机号 / 邮箱 / 紧急联系电话后端仅返回脱敏值，留空表示不修改
  void ensureLeaderTree()
  drawerVisible.value = true
}

async function submit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      const payload: EmployeeUpdateRequest = {
        employeeNo: form.employeeNo || undefined,
        name: form.name || undefined,
        gender: form.gender || undefined,
        birthDate: form.birthDate || undefined,
        deptId: form.deptId || undefined,
        positionId: form.positionId || undefined,
        leaderId: form.leaderId || '0',
        employmentType: form.employmentType || undefined,
        hireDate: form.hireDate || undefined,
        probationEndDate: form.probationEndDate || undefined,
        emergencyContact: form.emergencyContact || undefined,
        remark: form.remark || undefined
      }
      // 敏感字段仅在有输入时提交，留空即不修改（与后端 update 语义一致）
      if (form.userId) payload.userId = form.userId
      if (form.idCard) payload.idCard = form.idCard
      if (form.phone) payload.phone = form.phone
      if (form.email) payload.email = form.email
      if (form.emergencyPhone) payload.emergencyPhone = form.emergencyPhone
      await ceamsApi.hr.employeeUpdate(editingId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await ceamsApi.hr.employeeCreate({
        userId: form.userId || null,
        employeeNo: form.employeeNo,
        name: form.name,
        gender: form.gender || undefined,
        birthDate: form.birthDate || undefined,
        idCard: form.idCard || undefined,
        phone: form.phone || undefined,
        email: form.email || undefined,
        deptId: form.deptId || undefined,
        positionId: form.positionId || undefined,
        leaderId: form.leaderId || undefined,
        levelId: form.levelId || undefined,
        employmentType: form.employmentType || undefined,
        hireDate: form.hireDate || undefined,
        probationEndDate: form.probationEndDate || undefined,
        emergencyContact: form.emergencyContact || undefined,
        emergencyPhone: form.emergencyPhone || undefined,
        remark: form.remark || undefined
      })
      ElMessage.success('创建成功')
    }
    drawerVisible.value = false
    await load()
    orgTree.value = []
    await reloadTree()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '保存员工档案失败'))
  }
}

// ---------- 转正 / 调岗 / 晋升 / 离职 ----------
const changeVisible = ref(false)
const changeType = ref<HrChangeType>('REGULAR')
const changeEmployee = ref<EmployeeVO | null>(null)
const changeFormRef = ref<FormInstance>()
const changeForm = reactive({
  effectiveDate: '',
  reason: '',
  targetDeptId: '',
  targetPositionId: '',
  targetLevelId: '',
  resignLeaderId: '',
  remark: ''
})
/** 离职员工名下在职直接下属（存在时必须指定转交上级）。 */
const resignSubordinates = ref<EmployeeVO[]>([])

const changeTitle = computed(() => `${hrChangeTypeLabels[changeType.value]}申请`)

// ---------- 审批流程预览（发起前） ----------
const changePreviewVisible = ref(false)
const changePreviewCode = ref('')
const changePreviewSubjectUserId = ref<string | null>(null)

/** 入转调离类型 -> BPM 模板编码（与后端 ChangeType.bpmBizType 一致）。 */
function changeTemplateCode(type: HrChangeType): string {
  switch (type) {
    case 'TRANSFER':
      return 'HR_TRANSFER'
    case 'PROMOTION':
      return 'HR_PROMOTION'
    case 'RESIGN':
      return 'HR_RESIGN'
    default:
      return 'HR_REGULAR'
  }
}

/** 打开发起前流程预览：按目标员工账号解析其部门负责人/直属上级。 */
function openChangePreview(): void {
  if (!changeEmployee.value) return
  changePreviewCode.value = changeTemplateCode(changeType.value)
  changePreviewSubjectUserId.value = changeEmployee.value.userId ?? null
  changePreviewVisible.value = true
}

function openChange(row: EmployeeVO, type: HrChangeType): void {
  changeEmployee.value = row
  changeType.value = type
  Object.assign(changeForm, {
    effectiveDate: '',
    reason: '',
    targetDeptId: '',
    targetPositionId: '',
    targetLevelId: '',
    resignLeaderId: '',
    remark: ''
  })
  resignSubordinates.value = []
  changeVisible.value = true
  if (type === 'RESIGN') {
    void loadResignSubordinates(row.id)
  }
}

/** 加载离职员工名下在职直接下属，用于强制转交提示与候选过滤。 */
async function loadResignSubordinates(employeeId: string): Promise<void> {
  try {
    const list = await ceamsApi.hr.employeeSubordinates(employeeId)
    if (changeEmployee.value?.id === employeeId) {
      resignSubordinates.value = list
    }
  } catch {
    resignSubordinates.value = []
  }
}

/** 当天日期 yyyy-MM-dd（晋升生效日期由后端按当天处理）。 */
function todayStr(): string {
  const d = new Date()
  const month = `${d.getMonth() + 1}`.padStart(2, '0')
  const day = `${d.getDate()}`.padStart(2, '0')
  return `${d.getFullYear()}-${month}-${day}`
}

async function submitChange(): Promise<void> {
  if (!changeEmployee.value) return
  if (changeType.value !== 'PROMOTION' && !changeForm.effectiveDate) {
    ElMessage.warning('请选择生效日期')
    return
  }
  if (changeType.value === 'TRANSFER' && !changeForm.targetDeptId && !changeForm.targetPositionId) {
    ElMessage.warning('调岗申请需指定目标部门或目标岗位')
    return
  }
  if (changeType.value === 'PROMOTION' && !changeForm.targetLevelId) {
    ElMessage.warning('晋升申请需选择目标职级')
    return
  }
  if (changeType.value === 'RESIGN' && resignSubordinates.value.length && !changeForm.resignLeaderId) {
    ElMessage.warning(`该员工还有 ${resignSubordinates.value.length} 名在职下属，请指定转交上级`)
    return
  }

  const body: EmployeeChangeRequest = {
    effectiveDate: changeType.value === 'PROMOTION' ? todayStr() : changeForm.effectiveDate,
    reason: changeForm.reason || undefined,
    remark: changeForm.remark || undefined,
    targetDeptId: changeType.value === 'TRANSFER' ? changeForm.targetDeptId || undefined : undefined,
    targetPositionId:
      changeType.value === 'TRANSFER' ? changeForm.targetPositionId || undefined : undefined,
    targetLevelId:
      changeType.value === 'PROMOTION' ? changeForm.targetLevelId || undefined : undefined,
    resignLeaderId:
      changeType.value === 'RESIGN' ? changeForm.resignLeaderId || undefined : undefined
  }

  try {
    if (changeType.value === 'REGULAR') {
      await ceamsApi.hr.employeeRegular(changeEmployee.value.id, body)
    } else if (changeType.value === 'TRANSFER') {
      await ceamsApi.hr.employeeTransfer(changeEmployee.value.id, body)
    } else if (changeType.value === 'PROMOTION') {
      await ceamsApi.hr.employeePromotion(changeEmployee.value.id, body)
    } else {
      await ceamsApi.hr.employeeResign(changeEmployee.value.id, body)
    }
    ElMessage.success('已发起审批')
    changeVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '发起申请失败'))
  }
}

// ---------- 变更记录 ----------
const changeListVisible = ref(false)
const changeListLoading = ref(false)
const changeRecords = ref<EmployeeChangeVO[]>([])
const changeRecordEmployee = ref<EmployeeVO | null>(null)

async function openChanges(row: EmployeeVO): Promise<void> {
  changeRecordEmployee.value = row
  changeListVisible.value = true
  changeListLoading.value = true
  changeRecords.value = []
  try {
    changeRecords.value = await ceamsApi.hr.employeeChanges(row.id)
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载变更记录失败'))
  } finally {
    changeListLoading.value = false
  }
}

onMounted(() => {
  void loadDepartments()
  void loadPositions()
  void loadLevels()
  void load()
  // 组织树由 el-tree 懒加载自动请求根节点，无需在这里手动加载
})
</script>

<template>
  <div class="list-page">
    <div class="body">
      <!-- 左侧组织树筛选：部门点击筛选右侧列表，员工点击打开详情 -->
      <div class="data-card tree-pane" :class="{ 'tree-pane--fullscreen': treeFullscreen }">
        <div class="tree-head">
          <span class="tree-title">组织架构</span>
          <div class="tree-actions">
            <el-button link type="primary" size="small" @click="treeFullscreen = !treeFullscreen">
              {{ treeFullscreen ? '退出全屏' : '全屏' }}
            </el-button>
            <el-button link type="primary" size="small" @click="refreshTree">刷新</el-button>
            <el-button link type="primary" size="small" @click="collapseTree">全部收起</el-button>
            <el-button
              v-if="treeMode === 'lazy'"
              link
              type="primary"
              size="small"
              @click="expandFirstLevel"
            >
              展开一级
            </el-button>
            <el-button v-else link type="primary" size="small" @click="expandFullTree">
              全部展开
            </el-button>
            <el-button link type="primary" size="small" @click="clearTreeFilter">重置</el-button>
          </div>
        </div>
        <div v-loading="orgTreeLoading" class="tree-body">
          <!-- lazy：关键字为空时按需加载；full：关键字过滤时一次性展示命中路径（key 变化强制重建） -->
          <el-tree
            v-show="!treeEmpty"
            :key="`${treeMode}-${treeEpoch}`"
            ref="orgTreeRef"
            :data="treeMode === 'full' ? orgTree : undefined"
            :lazy="treeMode === 'lazy'"
            :load="lazyLoadNode"
            :props="treeProps"
            node-key="id"
            highlight-current
            :expand-on-click-node="false"
            :indent="12"
            @node-click="onTreeNodeClick"
          >
            <template #default="{ data }">
              <span class="org-node" :class="nodeRowClass(data as OrgTreeNodeVO)">
                <svg
                  v-if="(data as OrgTreeNodeVO).nodeType === 'EMPLOYEE'"
                  class="org-icon"
                  viewBox="0 0 16 16"
                  aria-hidden="true"
                >
                  <path
                    fill="currentColor"
                    d="M8 8a3 3 0 1 0 0-6 3 3 0 0 0 0 6Zm0 1.2c-2.9 0-5 1.5-5 3.1V13h10v-.7c0-1.6-2.1-3.1-5-3.1Z"
                  />
                </svg>
                <svg
                  v-else-if="(data as OrgTreeNodeVO).nodeType === 'UNASSIGNED'"
                  class="org-icon"
                  viewBox="0 0 16 16"
                  aria-hidden="true"
                >
                  <path
                    fill="currentColor"
                    d="M1.6 3.4h4.2L7.2 4.8h7.2v7.8H1.6V3.4Zm1.2 2v6h11.2V6H2.8Z"
                  />
                </svg>
                <svg v-else class="org-icon" viewBox="0 0 16 16" aria-hidden="true">
                  <path
                    fill="currentColor"
                    d="M3 1.8h6.6L13 5.2v9H3v-12.4Zm1.2 1.2v10h7.6V5.6L9.6 3H4.2Zm1 2h4v1.1h-4V5Zm0 2.4h4v1.1h-4V7.4Zm0 2.4h2.8v1.1H5.2V9.8Z"
                  />
                </svg>
                <el-tooltip
                  :content="(data as OrgTreeNodeVO).name"
                  placement="top"
                  :show-after="300"
                >
                  <span class="org-name">{{ (data as OrgTreeNodeVO).name }}</span>
                </el-tooltip>
                <template v-if="(data as OrgTreeNodeVO).nodeType === 'EMPLOYEE'">
                  <span v-if="(data as OrgTreeNodeVO).levelCode" class="org-level">
                    {{ (data as OrgTreeNodeVO).levelCode }}
                  </span>
                  <span v-if="(data as OrgTreeNodeVO).employeeNo" class="org-no">
                    {{ (data as OrgTreeNodeVO).employeeNo }}
                  </span>
                  <el-tooltip
                    v-if="(data as OrgTreeNodeVO).positionName"
                    :content="(data as OrgTreeNodeVO).positionName ?? ''"
                    placement="top"
                    :show-after="300"
                  >
                    <span class="org-post">{{ (data as OrgTreeNodeVO).positionName }}</span>
                  </el-tooltip>
                  <el-tooltip
                    v-if="(data as OrgTreeNodeVO).crossDeptLeader"
                    :content="crossLeaderTip(data as OrgTreeNodeVO)"
                    placement="top"
                  >
                    <span class="org-cross" @click.stop="onCrossLeaderClick(data as OrgTreeNodeVO)">
                      ↗ 上级：{{ (data as OrgTreeNodeVO).leaderName }}（{{
                        (data as OrgTreeNodeVO).leaderDeptName
                      }}）
                    </span>
                  </el-tooltip>
                </template>
                <template v-else>
                  <span v-if="(data as OrgTreeNodeVO).leaderName" class="org-meta">
                    负责人：{{ (data as OrgTreeNodeVO).leaderName }}
                  </span>
                  <el-tooltip
                    v-if="(data as OrgTreeNodeVO).nodeType === 'DEPT'"
                    :content="headcountTip(data as OrgTreeNodeVO)"
                    placement="top"
                  >
                    <span class="org-count">{{ (data as OrgTreeNodeVO).headcount ?? 0 }}人</span>
                  </el-tooltip>
                  <span v-if="!hasChildNode(data as OrgTreeNodeVO)" class="org-empty">
                    暂无员工
                  </span>
                </template>
              </span>
            </template>
          </el-tree>
          <el-empty
            v-if="!orgTreeLoading && treeEmpty"
            :image-size="60"
            description="暂无组织数据"
          />
        </div>
      </div>

      <div class="main-pane">
        <!-- 筛选区：白色圆角卡片，控件 12px 间距，操作按钮靠右 -->
        <div class="filter-bar">
          <el-input
            v-model="query.keyword"
            placeholder="关键字（姓名/工号）"
            clearable
            style="width: 200px"
            @keyup.enter="onSearch"
          />
          <el-select
            v-model="query.deptId"
            placeholder="部门"
            clearable
            style="width: 180px"
            @change="onDeptChange"
          >
            <el-option
              v-for="opt in deptOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-select
            v-model="query.status"
            placeholder="员工状态"
            clearable
            style="width: 140px"
            @change="onSearch"
          >
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <div class="filter-actions">
            <el-button type="primary" @click="onSearch">查询</el-button>
          </div>
        </div>

        <!-- 表格卡片：头部放置标题、条数与主操作 -->
        <div class="data-card">
          <div class="card-head">
            <div class="card-heading">
              <span class="card-title">员工档案</span>
              <span class="card-sub">共 {{ total }} 条</span>
            </div>
            <div class="card-actions">
              <el-button type="primary" @click="openCreate">新增员工</el-button>
            </div>
          </div>

          <el-table v-loading="loading" :data="list" border>
            <el-table-column prop="employeeNo" label="工号" width="120" />
            <el-table-column prop="name" label="姓名" width="110" />
            <el-table-column label="部门" min-width="140">
              <template #default="{ row }">{{ deptName((row as EmployeeVO).deptId) }}</template>
            </el-table-column>
            <el-table-column label="岗位" min-width="140">
              <template #default="{ row }">{{ positionName((row as EmployeeVO).positionId) }}</template>
            </el-table-column>
            <el-table-column label="直属上级" min-width="110">
              <template #default="{ row }">{{ (row as EmployeeVO).leaderName || '-' }}</template>
            </el-table-column>
            <el-table-column label="职级" min-width="110">
              <template #default="{ row }">{{ (row as EmployeeVO).levelName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="hireDate" label="入职日期" width="120" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="statusTagType((row as EmployeeVO).status)">
                  {{ hrEmployeeStatusLabels[(row as EmployeeVO).status] }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="phone" label="手机号" width="140" />
            <el-table-column label="操作" width="370" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openEdit(row as EmployeeVO)">编辑</el-button>
                <el-button link type="primary" size="small" @click="openChange(row as EmployeeVO, 'REGULAR')">转正</el-button>
                <el-button link type="primary" size="small" @click="openChange(row as EmployeeVO, 'TRANSFER')">调岗</el-button>
                <el-button link type="primary" size="small" @click="openChange(row as EmployeeVO, 'PROMOTION')">晋升</el-button>
                <el-button link type="danger" size="small" @click="openChange(row as EmployeeVO, 'RESIGN')">离职</el-button>
                <el-button link type="info" size="small" @click="openChanges(row as EmployeeVO)">变更记录</el-button>
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
            @size-change="onPageSizeChange"
            @current-change="load"
          />
        </div>
      </div>
    </div>

    <el-drawer v-model="drawerVisible" :title="editingId ? '编辑员工档案' : '新增员工档案'" size="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="工号" prop="employeeNo">
          <el-input v-model="form.employeeNo" placeholder="请输入员工工号" />
        </el-form-item>
        <el-form-item label="绑定用户 ID">
          <el-input v-model="form.userId" placeholder="系统用户 ID（可空，编辑时填 0 表示解绑）" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="form.gender" placeholder="请选择性别" clearable style="width: 100%">
            <el-option v-for="opt in genderOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="出生日期">
          <el-date-picker
            v-model="form.birthDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input
            v-model="form.idCard"
            :placeholder="editingId ? '后端仅返回脱敏值，留空表示不修改' : '请输入身份证号'"
          />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input
            v-model="form.phone"
            :placeholder="editingId ? '后端仅返回脱敏值，留空表示不修改' : '请输入手机号'"
          />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input
            v-model="form.email"
            :placeholder="editingId ? '后端仅返回脱敏值，留空表示不修改' : '请输入邮箱'"
          />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="form.deptId" placeholder="请选择部门" clearable style="width: 100%">
            <el-option
              v-for="opt in deptOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位">
          <el-select v-model="form.positionId" placeholder="请选择岗位" clearable style="width: 100%">
            <el-option
              v-for="opt in positionOptions"
              :key="opt.id"
              :label="opt.name"
              :value="opt.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="直属上级">
          <el-tree-select
            v-model="form.leaderId"
            :data="leaderTreeData"
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            check-strictly
            clearable
            :disabled="!canMaintainLeader"
            placeholder="请选择直属上级"
            style="width: 100%"
          />
          <div v-if="!canMaintainLeader" class="form-hint">缺少权限：维护员工上下级（hr:employee:leader）</div>
        </el-form-item>
        <el-form-item label="职级">
          <el-select
            v-model="form.levelId"
            :disabled="!!editingId"
            placeholder="请选择职级"
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="opt in levelOptions"
              :key="opt.id"
              :label="`${opt.code} ${opt.name}`"
              :value="opt.id"
            />
          </el-select>
          <div v-if="editingId" class="form-hint">职级调整请走晋升申请</div>
        </el-form-item>
        <el-form-item label="用工类型">
          <el-select v-model="form.employmentType" placeholder="请选择用工类型" clearable style="width: 100%">
            <el-option
              v-for="opt in employmentTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="入职日期">
          <el-date-picker
            v-model="form.hireDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="试用期结束">
          <el-date-picker
            v-model="form.probationEndDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="紧急联系人">
          <el-input v-model="form.emergencyContact" placeholder="请输入紧急联系人" />
        </el-form-item>
        <el-form-item label="紧急联系电话">
          <el-input
            v-model="form.emergencyPhone"
            :placeholder="editingId ? '后端仅返回脱敏值，留空表示不修改' : '请输入紧急联系电话'"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-drawer>

    <el-dialog v-model="changeVisible" :title="changeTitle" width="520px">
      <div v-if="changeEmployee" class="change-tip">
        员工：{{ changeEmployee.name }}（{{ changeEmployee.employeeNo }}）
      </div>
      <el-form ref="changeFormRef" :model="changeForm" label-width="100px">
        <el-form-item v-if="changeType !== 'PROMOTION'" label="生效日期" required>
          <el-date-picker
            v-model="changeForm.effectiveDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item v-if="changeType === 'PROMOTION'" label="目标职级" required>
          <el-select
            v-model="changeForm.targetLevelId"
            placeholder="请选择目标职级"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="opt in levelOptions"
              :key="opt.id"
              :label="`${opt.code} ${opt.name}`"
              :value="opt.id"
            />
          </el-select>
        </el-form-item>
        <template v-if="changeType === 'TRANSFER'">
          <el-form-item label="目标部门">
            <el-select v-model="changeForm.targetDeptId" placeholder="请选择目标部门" clearable style="width: 100%">
              <el-option
                v-for="opt in deptOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="目标岗位">
            <el-select
              v-model="changeForm.targetPositionId"
              placeholder="请选择目标岗位"
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="opt in positionOptions"
                :key="opt.id"
                :label="opt.name"
                :value="opt.id"
              />
            </el-select>
          </el-form-item>
        </template>
        <template v-if="changeType === 'RESIGN'">
          <el-alert
            v-if="resignSubordinates.length"
            class="resign-tip"
            type="warning"
            :closable="false"
            show-icon
            :title="`该员工名下还有 ${resignSubordinates.length} 名在职下属，审批通过后将自动转交`"
          />
          <el-form-item v-if="resignSubordinates.length" label="下属转交上级" required>
            <el-tree-select
              v-model="changeForm.resignLeaderId"
              :data="resignTargetTree"
              node-key="id"
              :props="{ label: 'name', children: 'children' }"
              check-strictly
              clearable
              placeholder="请选择转交上级"
              style="width: 100%"
            />
            <div class="form-hint">候选范围已排除本人及待转交下属</div>
          </el-form-item>
        </template>
        <el-form-item label="申请原因">
          <el-input v-model="changeForm.reason" type="textarea" :rows="2" placeholder="申请原因（可选）" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="changeForm.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="openChangePreview">查看审批流程</el-button>
        <el-button @click="changeVisible = false">取消</el-button>
        <el-button type="primary" @click="submitChange">提交审批</el-button>
      </template>
    </el-dialog>

    <BpmFlowPreviewDialog
      v-model="changePreviewVisible"
      :template-code="changePreviewCode"
      :biz-type="changePreviewCode"
      :subject-user-id="changePreviewSubjectUserId"
    />

    <el-drawer v-model="changeListVisible" title="变更记录" size="480px">
      <div v-loading="changeListLoading">
        <div v-if="changeRecordEmployee" class="change-tip">
          员工：{{ changeRecordEmployee.name }}（{{ changeRecordEmployee.employeeNo }}）
        </div>
        <el-empty v-if="!changeListLoading && changeRecords.length === 0" description="暂无变更记录" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="item in changeRecords"
            :key="item.id"
            :timestamp="item.createTime ?? ''"
            placement="top"
          >
            <div class="record-title">
              <span>{{ hrChangeTypeLabels[item.changeType] }}</span>
              <el-tag size="small" :type="approvalTagType(item.approvalStatus)">
                {{ hrApprovalStatusLabels[item.approvalStatus] }}
              </el-tag>
            </div>
            <div class="record-line">生效日期：{{ item.effectiveDate }}</div>
            <div v-if="item.changeType === 'LEADER_TRANSFER'" class="record-line">
              转交上级：{{ item.resignLeaderName || '-' }}
            </div>
            <div v-else-if="item.changeType === 'PROMOTION'" class="record-line">
              目标职级：{{ item.targetLevelName || '-' }}
            </div>
            <div
              v-else-if="item.targetDeptId || item.targetPositionId"
              class="record-line"
            >
              目标：{{ deptName(item.targetDeptId) }} / {{ positionName(item.targetPositionId) }}
            </div>
            <div v-if="item.changeType === 'RESIGN' && item.resignLeaderName" class="record-line">
              下属转交上级：{{ item.resignLeaderName }}
            </div>
            <div v-if="item.reason" class="record-line">原因：{{ item.reason }}</div>
            <div v-if="item.remark" class="record-line">备注：{{ item.remark }}</div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
/* 左右分栏：左侧组织树，右侧筛选与列表 */
.body {
  display: flex;
  gap: var(--ceams-space-4, 16px);
  align-items: flex-start;
}

.tree-pane {
  width: 320px;
  flex-shrink: 0;
  padding: 16px;
}

/* 组织架构全屏：脱离分栏铺满视口，树区域内部滚动 */
.tree-pane--fullscreen {
  position: fixed;
  inset: 0;
  z-index: 2000;
  width: auto;
  height: auto;
  display: flex;
  flex-direction: column;
  border-radius: 0;
  background: var(--ceams-bg-card, #fff);
}

.tree-pane--fullscreen .tree-body {
  flex: 1;
  max-height: none;
  min-height: 0;
  overflow: auto;
}

.main-pane {
  flex: 1;
  min-width: 0;
}

.tree-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ceams-space-2, 8px);
}

.tree-title {
  flex-shrink: 0;
  font-size: var(--ceams-font-size-sm, 13px);
  font-weight: 600;
  color: var(--ceams-text-primary);
}

/* 组织树工具栏：按钮紧凑换行，避免挤占标题 */
.tree-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0 4px;
}

.tree-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.tree-body {
  margin-top: var(--ceams-space-3, 12px);
  min-height: 200px;
  max-height: calc(100vh - 260px);
  overflow: auto;
}

.tree-body :deep(.el-tree-node__content) {
  height: 30px;
}

/* 组织树节点行：图标 + 名称 + 负责人 / 人数 / 职级 / 跨部门上级 */
.org-node {
  display: flex;
  flex: 1;
  align-items: center;
  gap: var(--ceams-space-1, 4px);
  min-width: 0;
  padding-right: var(--ceams-space-2, 8px);
  font-size: var(--ceams-font-size-sm, 13px);
}

.org-icon {
  flex-shrink: 0;
  width: 14px;
  height: 14px;
  color: var(--ceams-text-secondary);
}

.org-node--dept .org-name {
  font-weight: 500;
}

.org-name {
  flex: 0 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 员工节点：工号 / 岗位，岗位超长省略并配合 tooltip */
.org-no {
  flex-shrink: 0;
  max-width: 72px;
  overflow: hidden;
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-xs, 12px);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.org-post {
  flex: 0 1 auto;
  min-width: 32px;
  max-width: 88px;
  overflow: hidden;
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-xs, 12px);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.org-meta,
.org-empty {
  flex-shrink: 0;
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-xs, 12px);
}

.org-count {
  flex-shrink: 0;
  padding: 0 6px;
  border-radius: 9px;
  background: var(--ceams-fill-light, #f0f2f5);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-xs, 12px);
  line-height: 18px;
}

.org-level {
  flex-shrink: 0;
  padding: 0 6px;
  border: 1px solid var(--ceams-border-divider, #e4e7ed);
  border-radius: 4px;
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-xs, 12px);
  line-height: 16px;
}

.org-cross {
  flex-shrink: 0;
  max-width: 150px;
  overflow: hidden;
  color: var(--ceams-color-primary, #409eff);
  font-size: var(--ceams-font-size-xs, 12px);
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.org-node.is-dimmed {
  opacity: 0.55;
}

.form-hint {
  width: 100%;
  margin-top: var(--ceams-space-1, 4px);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-xs, 12px);
  line-height: 18px;
}

.resign-tip {
  margin-bottom: 16px;
}

/* 筛选卡片：白色圆角、16px 20px 内边距，操作按钮靠右 */
.filter-bar {
  padding: 16px 20px;
}

.filter-actions {
  display: flex;
  gap: var(--ceams-space-2, 8px);
  margin-left: auto;
}

/* 表格卡片：20px 内边距 */
.data-card {
  padding: 20px;
}

/* 卡片头部：左侧标题与条数，右侧主操作 */
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ceams-space-3, 12px);
  margin-bottom: var(--ceams-space-4, 16px);
}

.card-heading {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--ceams-space-2, 8px);
}

.card-title {
  font-size: var(--ceams-font-size-md, 16px);
  font-weight: 600;
  color: var(--ceams-text-primary);
}

.card-sub {
  font-size: var(--ceams-font-size-xs, 12px);
  color: var(--ceams-text-secondary);
}

.card-actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: var(--ceams-space-2, 8px);
}

.card-actions .el-button + .el-button {
  margin-left: 0;
}

/* 分页右对齐，距表格 16px */
.pagination {
  margin-top: var(--ceams-space-4, 16px);
  justify-content: flex-end;
}

/* 空态居中灰色，上下 40px 留白 */
:deep(.el-table__empty-block) {
  padding: 40px 0;
  min-height: 0;
}

:deep(.el-table__empty-text) {
  color: var(--ceams-text-secondary);
}

/* 变更提示与时间线文本 */
.change-tip {
  margin-bottom: var(--ceams-space-3, 12px);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-sm, 13px);
}

.record-title {
  display: flex;
  gap: var(--ceams-space-2, 8px);
  align-items: center;
  font-weight: 500;
}

.record-line {
  margin-top: var(--ceams-space-1, 4px);
  color: var(--ceams-text-secondary);
  font-size: var(--ceams-font-size-sm, 13px);
}

/* 抽屉 / 弹窗：标题加粗，内容 20px 24px，底部操作条靠右并带顶部细边框 */
:deep(.el-drawer__title),
:deep(.el-dialog__title) {
  font-weight: 600;
}

:deep(.el-drawer__body),
:deep(.el-dialog__body) {
  padding: 20px 24px;
}

:deep(.el-drawer__footer),
:deep(.el-dialog__footer) {
  padding: 12px 24px 18px;
  border-top: 1px solid var(--ceams-border-divider);
  text-align: right;
}
</style>
