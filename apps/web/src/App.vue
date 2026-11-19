<script setup lang="ts">
// 根组件：仅承载路由出口；全局中文语言包，覆盖日期选择器等内置英文文案
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
</script>

<template>
  <ElConfigProvider :locale="zhCn">
    <RouterView />
  </ElConfigProvider>
</template>
