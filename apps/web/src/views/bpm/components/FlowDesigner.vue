<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import {
  ElAlert,
  ElButton,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElOption,
  ElSelect,
  ElSwitch,
  ElTag
} from 'element-plus'
import { ceamsApi } from '@/services/http'
import type { UserOptionVO } from '@ceams/types'
import { apiErrorMessage } from '@/utils/error'
import {
  bpmAssigneeSourceLabels,
  bpmConditionOperatorLabels,
  bpmNodeTypeLabels
} from '@/utils/labels'

defineOptions({ name: 'FlowDesigner' })

const props = defineProps<{ schemaJson: string }>()
const emit = defineEmits<{ 'update:schemaJson': [value: string] }>()

type EditableNodeType = 'SINGLE' | 'COUNTERSIGN' | 'OR_SIGN' | 'CC' | 'AUTO'

interface DesignerAssignee {
  type: string
  value: string
}

interface DesignerCondition {
  field: string
  operator: string
  value: string
}

interface DesignerNode {
  uid: string
  nodeKey: string
  nodeName: string
  nodeType: EditableNodeType
  assignees: DesignerAssignee[]
  dueHours: number | null
  autoApproveIfInitiator: boolean
  autoApproveWhen: DesignerCondition[]
}

type RawNode = Record<string, unknown>

const PALETTE: { type: EditableNodeType; label: string; desc: string }[] = [
  { type: 'SINGLE', label: '单人审批', desc: '一名审批人处理' },
  { type: 'COUNTERSIGN', label: '会签', desc: '全部同意后流转' },
  { type: 'OR_SIGN', label: '或签', desc: '任一人同意即流转' },
  { type: 'CC', label: '抄送', desc: '仅通知，不阻塞流程' },
  { type: 'AUTO', label: '自动节点', desc: '无需人工处理' }
]

/** 解析器支持直接书写的节点类型；APPROVAL 为历史写法。 */
const SUPPORTED_TYPES = new Set<string>([
  'START',
  'END',
  'SINGLE',
  'COUNTERSIGN',
  'OR_SIGN',
  'CC',
  'AUTO',
  'APPROVAL'
])

/** 必须解析出审批人的节点类型。 */
const ASSIGNABLE = new Set<EditableNodeType>(['SINGLE', 'COUNTERSIGN', 'OR_SIGN', 'CC'])

/** 模板审批人来源仅支持解析器已实现的分支。 */
const ASSIGNEE_TYPE_OPTIONS = ['USER', 'ROLE', 'DEPT_LEADER', 'SUPERIOR'].map((type) => ({
  value: type,
  label: bpmAssigneeSourceLabels[type] ?? type
}))

const OPERATOR_OPTIONS = Object.keys(bpmConditionOperatorLabels).map((value) => ({
  value,
  label: `${value}（${bpmConditionOperatorLabels[value]}）`
}))

const nodes = ref<DesignerNode[]>([])
const selectedUid = ref('')
const readOnly = ref(false)
const readOnlyReason = ref('')
const dragOverUid = ref('')

const userOptions = ref<UserOptionVO[]>([])
const userLoading = ref(false)
const userOptionsLoaded = ref(false)

/** 角色下拉：label 展示角色名称，value 仍存角色编码（后端按编码解析审批人）。 */
const roleOptions = ref<{ code: string; name: string }[]>([])
const roleOptionsLoaded = ref(false)

/** 补充历史配置中不在当前角色列表里的编码，避免下拉框只显示裸编码。 */
const roleSelectOptions = computed(() => {
  const map = new Map<string, string>()
  for (const role of roleOptions.value) {
    map.set(role.code, role.name)
  }
  for (const node of nodes.value) {
    for (const assignee of node.assignees) {
      if (assignee.type === 'ROLE' && assignee.value && !map.has(assignee.value)) {
        map.set(assignee.value, assignee.value)
      }
    }
  }
  return Array.from(map, ([code, name]) => ({ code, name }))
})

async function loadRoles(): Promise<void> {
  if (roleOptionsLoaded.value) return
  try {
    const page = await ceamsApi.iam.rolePage({ page: 1, size: 100 })
    roleOptions.value = page.records.map((role) => ({ code: role.code, name: role.name }))
    roleOptionsLoaded.value = true
  } catch {
    // 角色加载失败时仍保留历史值展示
  }
}

let uidSeed = 0
let keySeed = 0
let suppressEmit = false

type DragPayload =
  | { kind: 'palette'; nodeType: EditableNodeType }
  | { kind: 'node'; uid: string }

let dragPayload: DragPayload | null = null

const selectedNode = computed<DesignerNode | null>(
  () => nodes.value.find((node) => node.uid === selectedUid.value) ?? null
)

function nextUid(): string {
  uidSeed += 1
  return `d${uidSeed}`
}

function uniqueKey(): string {
  const existed = new Set(nodes.value.map((node) => node.nodeKey))
  let key = ''
  do {
    keySeed += 1
    key = `node_${keySeed}`
  } while (existed.has(key))
  return key
}

function isAssignable(type: EditableNodeType): boolean {
  return ASSIGNABLE.has(type)
}

function nodeTagType(type: EditableNodeType): 'primary' | 'success' | 'warning' | 'info' {
  switch (type) {
    case 'COUNTERSIGN':
      return 'success'
    case 'OR_SIGN':
      return 'warning'
    case 'CC':
    case 'AUTO':
      return 'info'
    default:
      return 'primary'
  }
}

function assigneeSummary(node: DesignerNode): string {
  if (!isAssignable(node.nodeType)) {
    return '无需审批人'
  }
  if (!node.assignees.length) {
    return '未配置审批人'
  }
  return node.assignees
    .map((assignee) => {
      const label = bpmAssigneeSourceLabels[assignee.type] ?? assignee.type
      if (!assignee.value) return label
      if (assignee.type === 'ROLE') {
        const role = roleSelectOptions.value.find((item) => item.code === assignee.value)
        return `${label}：${role?.name ?? assignee.value}`
      }
      return `${label}：${assignee.value}`
    })
    .join('、')
}

function defaultAssignees(type: EditableNodeType): DesignerAssignee[] {
  return isAssignable(type) ? [{ type: 'DEPT_LEADER', value: '' }] : []
}

function addNode(type: EditableNodeType, index = nodes.value.length): void {
  if (readOnly.value) return
  const node: DesignerNode = {
    uid: nextUid(),
    nodeKey: uniqueKey(),
    nodeName: PALETTE.find((item) => item.type === type)?.label ?? type,
    nodeType: type,
    assignees: defaultAssignees(type),
    dueHours: null,
    autoApproveIfInitiator: false,
    autoApproveWhen: []
  }
  nodes.value.splice(index, 0, node)
  selectedUid.value = node.uid
}

function removeNode(index: number): void {
  const [removed] = nodes.value.splice(index, 1)
  if (removed && selectedUid.value === removed.uid) {
    selectedUid.value = ''
  }
}

function moveNode(index: number, offset: number): void {
  const target = index + offset
  if (target < 0 || target >= nodes.value.length) return
  const [moved] = nodes.value.splice(index, 1)
  nodes.value.splice(target, 0, moved)
}

function onNodeTypeChange(node: DesignerNode): void {
  if (!isAssignable(node.nodeType)) {
    node.assignees = []
    return
  }
  if (!node.assignees.length) {
    node.assignees = defaultAssignees(node.nodeType)
  }
}

function addAssignee(node: DesignerNode): void {
  node.assignees.push({ type: 'ROLE', value: '' })
}

function removeAssignee(node: DesignerNode, index: number): void {
  node.assignees.splice(index, 1)
}

function onAssigneeTypeChange(assignee: DesignerAssignee): void {
  if (assignee.type === 'USER' || assignee.type === 'ROLE') {
    return
  }
  assignee.value = ''
}

function addAutoCondition(node: DesignerNode): void {
  node.autoApproveWhen.push({ field: '', operator: '>=', value: '' })
}

function removeAutoCondition(node: DesignerNode, index: number): void {
  node.autoApproveWhen.splice(index, 1)
}

async function searchUsers(keyword: string): Promise<void> {
  userLoading.value = true
  try {
    userOptions.value = await ceamsApi.bpm.delegateCandidates(keyword || undefined)
    userOptionsLoaded.value = true
  } catch (err) {
    ElMessage.error(apiErrorMessage(err, '加载人员失败'))
  } finally {
    userLoading.value = false
  }
}

function onUserDropdown(visible: boolean): void {
  if (visible && !userOptionsLoaded.value) {
    void searchUsers('')
  }
}

// ---------------- 拖拽 ----------------

function onPaletteDragStart(type: EditableNodeType): void {
  dragPayload = { kind: 'palette', nodeType: type }
}

function onNodeDragStart(uid: string): void {
  dragPayload = { kind: 'node', uid }
}

function onNodeDragOver(uid: string): void {
  dragOverUid.value = uid
}

function onDragEnd(): void {
  dragPayload = null
  dragOverUid.value = ''
}

function onCanvasDrop(): void {
  const payload = dragPayload
  onDragEnd()
  if (payload?.kind === 'palette') {
    addNode(payload.nodeType)
  }
}

function onNodeDrop(uid: string): void {
  const payload = dragPayload
  onDragEnd()
  if (!payload) return
  if (payload.kind === 'palette') {
    const index = nodes.value.findIndex((node) => node.uid === uid)
    addNode(payload.nodeType, index < 0 ? nodes.value.length : index)
    return
  }
  if (payload.uid === uid) return
  const from = nodes.value.findIndex((node) => node.uid === payload.uid)
  const to = nodes.value.findIndex((node) => node.uid === uid)
  if (from < 0 || to < 0) return
  const [moved] = nodes.value.splice(from, 1)
  nodes.value.splice(to, 0, moved)
}

// ---------------- 解析与生成 ----------------

function nodeTypeOf(raw: RawNode): string {
  const rawType = typeof raw.nodeType === 'string' ? raw.nodeType.trim() : ''
  if (!rawType) return 'SINGLE'
  const upper = rawType.toUpperCase()
  if (upper === 'APPROVAL') {
    return String(raw.approveMode ?? '').toUpperCase() === 'ANY' ? 'OR_SIGN' : 'COUNTERSIGN'
  }
  return upper
}

function parseAssignees(raw: RawNode, type: EditableNodeType): DesignerAssignee[] {
  if (!isAssignable(type)) return []
  const result: DesignerAssignee[] = []
  const list = raw.assignees
  if (Array.isArray(list)) {
    for (const item of list) {
      if (!item || typeof item !== 'object') continue
      const source = item as { type?: unknown; value?: unknown }
      const sourceType = typeof source.type === 'string' ? source.type.toUpperCase() : ''
      if (!sourceType) continue
      result.push({
        type: sourceType,
        value: typeof source.value === 'string' ? source.value : String(source.value ?? '')
      })
    }
  } else if (typeof raw.assigneeType === 'string' && raw.assigneeType) {
    result.push({
      type: raw.assigneeType.toUpperCase(),
      value: typeof raw.assigneeValue === 'string' ? raw.assigneeValue : ''
    })
  }
  return result.length ? result : defaultAssignees(type)
}

function parseAutoConditions(raw: RawNode): DesignerCondition[] {
  const list = raw.autoApproveWhen
  if (!Array.isArray(list)) return []
  const result: DesignerCondition[] = []
  for (const item of list) {
    if (!item || typeof item !== 'object') continue
    const condition = item as { field?: unknown; operator?: unknown; value?: unknown }
    result.push({
      field: typeof condition.field === 'string' ? condition.field : '',
      operator: typeof condition.operator === 'string' ? condition.operator : '>=',
      value: typeof condition.value === 'string' ? condition.value : String(condition.value ?? '')
    })
  }
  return result
}

function detectUnsupported(list: RawNode[]): string {
  for (const raw of list) {
    const type = nodeTypeOf(raw)
    if (type === 'START' || type === 'END') continue
    if (!SUPPORTED_TYPES.has(type)) {
      return `节点 ${String(raw.nodeKey ?? '')} 的类型 ${type} 暂不支持可视化编辑，请切换到 JSON 编辑`
    }
    if (Array.isArray(raw.conditions) && raw.conditions.length > 0) {
      return '该模板包含条件分支，可视化模式无法表达，请切换到 JSON 编辑'
    }
    if (raw.nextNodeKey || raw.defaultNext) {
      return '该模板包含自定义跳转，可视化模式无法表达，请切换到 JSON 编辑'
    }
  }
  return ''
}

function extractRawNodes(parsed: unknown): RawNode[] {
  if (!parsed || typeof parsed !== 'object') return []
  const list = (parsed as { nodes?: unknown }).nodes
  if (!Array.isArray(list)) return []
  return list.filter((item): item is RawNode => Boolean(item) && typeof item === 'object')
}

/** 载入模板 JSON 到画布；不支持的模板进入只读提示，避免误改丢失配置。 */
function load(schemaJson: string): void {
  suppressEmit = true
  selectedUid.value = ''
  dragOverUid.value = ''
  readOnly.value = false
  readOnlyReason.value = ''

  let parsed: unknown
  try {
    parsed = JSON.parse(schemaJson || '{"nodes":[]}')
  } catch {
    nodes.value = []
    readOnly.value = true
    readOnlyReason.value = '当前内容不是合法 JSON，请在 JSON 编辑模式修正后再切换到可视化设计'
    void nextTick(() => {
      suppressEmit = false
    })
    return
  }

  const list = extractRawNodes(parsed)
  const problem = detectUnsupported(list)
  if (problem) {
    nodes.value = []
    readOnly.value = true
    readOnlyReason.value = problem
    void nextTick(() => {
      suppressEmit = false
    })
    return
  }

  const usedKeys = new Set<string>()
  const result: DesignerNode[] = []
  list.forEach((raw, index) => {
    const type = nodeTypeOf(raw)
    if (type === 'START' || type === 'END') return
    let key = typeof raw.nodeKey === 'string' ? raw.nodeKey.trim() : ''
    if (!key) key = `node_${index + 1}`
    const base = key
    let suffix = 0
    while (usedKeys.has(key)) {
      suffix += 1
      key = `${base}_${suffix}`
    }
    usedKeys.add(key)
    const dueHours = typeof raw.dueHours === 'number' && Number.isFinite(raw.dueHours) ? raw.dueHours : null
    result.push({
      uid: nextUid(),
      nodeKey: key,
      nodeName: typeof raw.nodeName === 'string' ? raw.nodeName : key,
      nodeType: type as EditableNodeType,
      assignees: parseAssignees(raw, type as EditableNodeType),
      dueHours,
      autoApproveIfInitiator: raw.autoApproveIfInitiator === true,
      autoApproveWhen: parseAutoConditions(raw)
    })
  })
  nodes.value = result
  void nextTick(() => {
    suppressEmit = false
  })
}

function toSchemaNode(node: DesignerNode, seqNo: number): Record<string, unknown> {
  const result: Record<string, unknown> = {
    nodeKey: node.nodeKey,
    nodeName: node.nodeName || node.nodeKey,
    seqNo,
    nodeType: node.nodeType
  }
  if (isAssignable(node.nodeType)) {
    result.assignees = node.assignees
      .filter((assignee) => assignee.type)
      .map((assignee) => ({
        type: assignee.type,
        ...(assignee.value ? { value: assignee.value } : {})
      }))
  }
  if (node.dueHours) {
    result.dueHours = node.dueHours
  }
  if (node.autoApproveIfInitiator) {
    result.autoApproveIfInitiator = true
  }
  const autoConditions = node.autoApproveWhen.filter(
    (condition) => condition.field && condition.operator
  )
  if (autoConditions.length) {
    result.autoApproveWhen = autoConditions.map((condition) => ({
      field: condition.field,
      operator: condition.operator,
      value: condition.value
    }))
  }
  return result
}

function emitSchema(): void {
  if (suppressEmit || readOnly.value) return
  const payload = {
    nodes: [
      { nodeKey: 'start', nodeName: '发起', seqNo: 1, nodeType: 'START' },
      ...nodes.value.map((node, index) => toSchemaNode(node, index + 2)),
      { nodeKey: 'end', nodeName: '结束', seqNo: nodes.value.length + 2, nodeType: 'END' }
    ]
  }
  emit('update:schemaJson', JSON.stringify(payload, null, 2))
}

watch(nodes, emitSchema, { deep: true })

onMounted(() => {
  void loadRoles()
  load(props.schemaJson)
})

defineExpose({ load })
</script>

<template>
  <div class="flow-designer">
    <aside class="palette">
      <div class="palette-title">节点库</div>
      <div
        v-for="item in PALETTE"
        :key="item.type"
        class="palette-item"
        :class="{ disabled: readOnly }"
        :draggable="!readOnly"
        @dragstart="onPaletteDragStart(item.type)"
        @dragend="onDragEnd"
        @click="addNode(item.type)"
      >
        <span class="palette-name">{{ item.label }}</span>
        <span class="palette-desc">{{ item.desc }}</span>
      </div>
      <div class="palette-tip">拖入中间画布，或点击直接追加到流程末尾</div>
    </aside>

    <div class="canvas" @dragover.prevent @drop.prevent="onCanvasDrop">
      <el-alert
        v-if="readOnly"
        :title="readOnlyReason"
        type="warning"
        :closable="false"
        show-icon
      />
      <template v-else>
        <div class="fixed-node">
          <el-tag size="small" type="success">开始</el-tag>
          <span class="fixed-name">发起</span>
        </div>

        <div v-if="!nodes.length" class="canvas-empty">从左侧拖入审批节点</div>

        <div v-for="(node, index) in nodes" :key="node.uid" class="node-wrap">
          <div class="connector" />
          <div
            class="node-card"
            :class="{ active: node.uid === selectedUid, 'drag-over': node.uid === dragOverUid }"
            draggable="true"
            @dragstart="onNodeDragStart(node.uid)"
            @dragend="onDragEnd"
            @dragover.stop.prevent="onNodeDragOver(node.uid)"
            @drop.stop.prevent="onNodeDrop(node.uid)"
            @click="selectedUid = node.uid"
          >
            <div class="node-head">
              <span class="node-seq">{{ index + 1 }}</span>
              <span class="node-name">{{ node.nodeName || node.nodeKey }}</span>
              <el-tag size="small" :type="nodeTagType(node.nodeType)">
                {{ bpmNodeTypeLabels[node.nodeType] ?? node.nodeType }}
              </el-tag>
            </div>
            <div class="node-meta">{{ assigneeSummary(node) }}</div>
            <div class="node-extra">
              <span v-if="node.dueHours">时限 {{ node.dueHours }} 小时</span>
              <span v-if="node.autoApproveIfInitiator">发起人自动通过</span>
              <span v-if="node.autoApproveWhen.length">
                自动通过条件 {{ node.autoApproveWhen.length }} 条
              </span>
            </div>
            <div class="node-actions" @click.stop>
              <el-button link size="small" :disabled="index === 0" @click="moveNode(index, -1)">
                上移
              </el-button>
              <el-button
                link
                size="small"
                :disabled="index === nodes.length - 1"
                @click="moveNode(index, 1)"
              >
                下移
              </el-button>
              <el-button link type="danger" size="small" @click="removeNode(index)">删除</el-button>
            </div>
          </div>
        </div>

        <div v-if="nodes.length" class="connector" />
        <div class="fixed-node">
          <el-tag size="small" type="info">结束</el-tag>
          <span class="fixed-name">结束</span>
        </div>
      </template>
    </div>

    <aside class="props">
      <template v-if="selectedNode && !readOnly">
        <div class="props-title">节点属性</div>
        <el-form label-position="top" size="small">
          <el-form-item label="节点名称">
            <el-input v-model="selectedNode.nodeName" maxlength="30" placeholder="节点名称" />
          </el-form-item>
          <el-form-item label="节点类型">
            <el-select v-model="selectedNode.nodeType" @change="onNodeTypeChange(selectedNode)">
              <el-option
                v-for="item in PALETTE"
                :key="item.type"
                :label="item.label"
                :value="item.type"
              />
            </el-select>
          </el-form-item>

          <el-form-item v-if="isAssignable(selectedNode.nodeType)" label="审批人">
            <div class="assignee-list">
              <div
                v-for="(assignee, index) in selectedNode.assignees"
                :key="index"
                class="assignee-row"
              >
                <el-select
                  v-model="assignee.type"
                  class="assignee-type"
                  @change="onAssigneeTypeChange(assignee)"
                >
                  <el-option
                    v-for="option in ASSIGNEE_TYPE_OPTIONS"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
                <el-select
                  v-if="assignee.type === 'USER'"
                  v-model="assignee.value"
                  class="assignee-value"
                  filterable
                  remote
                  :remote-method="searchUsers"
                  :loading="userLoading"
                  placeholder="搜索用户"
                  @visible-change="onUserDropdown"
                >
                  <el-option
                    v-for="user in userOptions"
                    :key="user.id"
                    :label="`${user.name}（${user.username}）`"
                    :value="user.id"
                  />
                </el-select>
                <el-select
                  v-else-if="assignee.type === 'ROLE'"
                  v-model="assignee.value"
                  class="assignee-value"
                  filterable
                  placeholder="选择角色"
                >
                  <el-option
                    v-for="role in roleSelectOptions"
                    :key="role.code"
                    :label="role.name"
                    :value="role.code"
                  />
                </el-select>
                <span v-else class="assignee-none">按发起人组织关系解析</span>
                <el-button link type="danger" @click="removeAssignee(selectedNode, index)">
                  移除
                </el-button>
              </div>
              <el-button link type="primary" @click="addAssignee(selectedNode)">+ 添加审批人来源</el-button>
            </div>
          </el-form-item>

          <el-form-item label="处理时限（小时）">
            <el-input-number
              v-model="selectedNode.dueHours"
              :min="1"
              :max="720"
              :step="1"
              controls-position="right"
              placeholder="留空取全局默认"
            />
          </el-form-item>

          <el-form-item label="发起人自动通过">
            <div class="switch-line">
              <el-switch v-model="selectedNode.autoApproveIfInitiator" />
              <span class="switch-hint">解析出的审批人全是发起人本人时自动通过</span>
            </div>
          </el-form-item>

          <el-form-item v-if="isAssignable(selectedNode.nodeType)" label="条件自动通过">
            <div class="condition-list">
              <div
                v-for="(condition, index) in selectedNode.autoApproveWhen"
                :key="index"
                class="condition-row"
              >
                <el-input v-model="condition.field" class="condition-field" placeholder="变量名，如 amount" />
                <el-select v-model="condition.operator" class="condition-operator">
                  <el-option
                    v-for="option in OPERATOR_OPTIONS"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
                <el-input v-model="condition.value" class="condition-value" placeholder="比较值" />
                <el-button link type="danger" @click="removeAutoCondition(selectedNode, index)">
                  移除
                </el-button>
              </div>
              <el-button link type="primary" @click="addAutoCondition(selectedNode)">
                + 添加自动通过条件
              </el-button>
            </div>
          </el-form-item>
        </el-form>
      </template>
      <div v-else class="props-empty">
        {{ readOnly ? '当前模板不支持可视化编辑' : '选中画布中的节点后在此编辑属性' }}
      </div>
    </aside>
  </div>
</template>

<style scoped>
.flow-designer {
  display: grid;
  grid-template-columns: 170px minmax(0, 1fr) 320px;
  gap: 12px;
  height: 540px;
}

.palette {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 10px;
  overflow-y: auto;
  background: var(--el-fill-color-lighter);
}
.palette-title,
.props-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 8px;
}
.palette-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 10px;
  margin-bottom: 8px;
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  background: var(--el-bg-color);
  cursor: grab;
  user-select: none;
}
.palette-item:hover {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}
.palette-item.disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
.palette-name {
  font-size: 13px;
  font-weight: 500;
}
.palette-desc,
.palette-tip {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}
.palette-tip {
  margin-top: 4px;
  line-height: 1.5;
}

.canvas {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 14px;
  overflow-y: auto;
  background: var(--el-fill-color-blank);
}
.fixed-node {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-fill-color-lighter);
  width: fit-content;
}
.fixed-name {
  font-size: 13px;
  color: var(--el-text-color-regular);
}
.node-wrap {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}
.connector {
  width: 2px;
  height: 18px;
  margin-left: 22px;
  background: var(--el-border-color);
}
.node-card {
  position: relative;
  width: 100%;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  padding: 10px 12px;
  background: var(--el-bg-color);
  cursor: grab;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.node-card:hover {
  border-color: var(--el-color-primary-light-5);
}
.node-card.active {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 2px var(--el-color-primary-light-7);
}
.node-card.drag-over {
  border-top: 2px solid var(--el-color-primary);
}
.node-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.node-seq {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  font-size: 12px;
  color: var(--el-color-white);
  background: var(--el-color-primary);
}
.node-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.node-meta {
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-text-color-regular);
}
.node-extra {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 4px;
  font-size: 11px;
  color: var(--el-text-color-secondary);
}
.node-actions {
  display: flex;
  gap: 6px;
  margin-top: 6px;
}
.canvas-empty {
  margin: 16px 0 16px 8px;
  padding: 18px;
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  text-align: center;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.props {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 12px;
  overflow-y: auto;
  background: var(--el-fill-color-lighter);
}
.props-empty {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}
.assignee-list,
.condition-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}
.assignee-row {
  display: flex;
  align-items: center;
  gap: 6px;
}
.assignee-type {
  width: 110px;
  flex: none;
}
.assignee-value {
  flex: 1;
}
.assignee-none {
  flex: 1;
  font-size: 11px;
  color: var(--el-text-color-secondary);
}
.condition-row {
  display: flex;
  align-items: center;
  gap: 4px;
}
.condition-field {
  width: 84px;
  flex: none;
}
.condition-operator {
  width: 108px;
  flex: none;
}
.condition-value {
  flex: 1;
}
.switch-line {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.switch-hint {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}
</style>
