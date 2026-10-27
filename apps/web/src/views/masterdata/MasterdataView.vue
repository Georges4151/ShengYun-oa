<script setup lang="ts">
import { ref } from 'vue'
import { ElTabPane, ElTabs } from 'element-plus'
import CategoryList from './components/CategoryList.vue'
import ContactList from './components/ContactList.vue'
import MaterialList from './components/MaterialList.vue'
import EquipmentList from './components/EquipmentList.vue'
import EquipmentLeaseList from './components/EquipmentLeaseList.vue'

defineOptions({ name: 'MasterdataView' })

const activeTab = ref('category')
</script>

<template>
  <el-tabs v-model="activeTab">
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
</template>
