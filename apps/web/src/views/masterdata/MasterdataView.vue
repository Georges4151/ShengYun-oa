<script setup lang="ts">
import { ref } from 'vue'
import { ElTabPane, ElTabs } from 'element-plus'
import { PageContainer } from '@ceams/ui-web'
import CategoryList from './components/CategoryList.vue'
import ContactList from './components/ContactList.vue'
import MaterialList from './components/MaterialList.vue'
import EquipmentList from './components/EquipmentList.vue'
import EquipmentLeaseList from './components/EquipmentLeaseList.vue'

defineOptions({ name: 'MasterdataView' })

const activeTab = ref('category')
</script>

<template>
  <PageContainer title="主数据" subtitle="统一维护分类、往来单位、材料、设备与设备租赁等基础档案">
    <el-tabs v-model="activeTab" class="masterdata-tabs">
      <el-tab-pane label="分类" name="category">
        <CategoryList />
      </el-tab-pane>
      <el-tab-pane label="往来单位" name="contact">
        <ContactList />
      </el-tab-pane>
      <el-tab-pane label="材料" name="material">
        <MaterialList />
      </el-tab-pane>
      <el-tab-pane label="设备" name="equipment">
        <EquipmentList />
      </el-tab-pane>
      <el-tab-pane label="设备租赁" name="lease">
        <EquipmentLeaseList />
      </el-tab-pane>
    </el-tabs>
  </PageContainer>
</template>

<style scoped>
/* 页签栏与内容区保持 16px 间距 */
.masterdata-tabs :deep(.el-tabs__header) {
  margin-bottom: var(--ceams-space-4);
}

/* 页签字号与选中加粗 */
.masterdata-tabs :deep(.el-tabs__item) {
  font-size: var(--ceams-font-size-base);
}

.masterdata-tabs :deep(.el-tabs__item.is-active) {
  font-weight: 600;
}
</style>
