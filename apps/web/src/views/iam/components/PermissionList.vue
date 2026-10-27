<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElTag, ElTree } from 'element-plus'
import { ceamsApi } from '@/services/http'
import type { PermissionVO } from '@ceams/types'
import { permissionTypeLabels } from '@/utils/labels'

defineOptions({ name: 'PermissionList' })

const treeProps = { label: 'name', children: 'children' }
const tree = ref<PermissionVO[]>([])
const mine = ref<string[]>([])

async function load(): Promise<void> {
  const [treeData, mineCodes] = await Promise.all([ceamsApi.iam.permissionTree(), ceamsApi.iam.permissionMine()])
  tree.value = treeData
  mine.value = mineCodes
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="permission-page">
    <div class="tree-pane">
      <h3>权限树</h3>
      <el-tree :data="tree" :props="treeProps" node-key="id" default-expand-all>
        <template #default="{ data }">
          <span class="perm-node">
            <span class="perm-name">{{ data.name }}</span>
            <el-tag size="small" type="info">{{ permissionTypeLabels[(data as PermissionVO).type] }}</el-tag>
            <span class="perm-code">{{ data.code }}</span>
          </span>
        </template>
      </el-tree>
    </div>
    <div class="mine-pane">
      <h3>我的权限码（{{ mine.length }}）</h3>
      <div class="tags">
        <el-tag v-for="code in mine" :key="code" class="tag">{{ code }}</el-tag>
        <span v-if="mine.length === 0" class="empty">暂无权限码</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.permission-page {
  display: flex;
  gap: 24px;
}
.tree-pane {
  flex: 1;
  min-width: 0;
}
.mine-pane {
  width: 360px;
  flex-shrink: 0;
}
h3 {
  margin: 0 0 12px;
  font-size: 14px;
}
.perm-node {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.perm-name {
  font-size: 14px;
}
.perm-code {
  font-size: 12px;
  color: #9ca3af;
}
.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.tag {
  font-family: monospace;
}
.empty {
  color: #9ca3af;
  font-size: 13px;
}
</style>
