<script setup lang="ts">
// 应用外壳布局占位：后续接入路由菜单与权限控制。
defineOptions({ name: 'CeamsAppShell' })

const props = withDefaults(defineProps<{ title?: string }>(), { title: 'CEAMS' })
</script>

<template>
  <div class="ceams-shell">
    <header class="ceams-shell__header">
      <div class="ceams-shell__brand">{{ props.title }}</div>
      <div class="ceams-shell__toolbar"><slot name="toolbar" /></div>
    </header>
    <div class="ceams-shell__body">
      <aside class="ceams-shell__aside"><slot name="aside" /></aside>
      <main class="ceams-shell__main"><slot /></main>
    </div>
  </div>
</template>

<style scoped>
.ceams-shell {
  display: flex;
  flex-direction: column;
  height: 100vh;
}
.ceams-shell__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 16px;
  border-bottom: 1px solid #e5e7eb;
}
.ceams-shell__brand {
  font-size: 16px;
  font-weight: 600;
}
.ceams-shell__body {
  display: flex;
  flex: 1;
  overflow: hidden;
}
.ceams-shell__aside {
  width: 220px;
  border-right: 1px solid #e5e7eb;
  overflow-y: auto;
}
.ceams-shell__main {
  flex: 1;
  overflow: auto;
  padding: 16px;
}
</style>