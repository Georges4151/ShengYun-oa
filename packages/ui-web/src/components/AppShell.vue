<script setup lang="ts">
// 应用外壳：白色顶栏 + 深色侧边栏 + 浅色内容区
// 对外契约保持不变：props.title、插槽 aside / toolbar / default
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
      <aside class="ceams-shell__aside">
        <!-- 侧边栏品牌标识：纯 CSS 绘制，不依赖图片资源 -->
        <div class="ceams-shell__logo">
          <span class="ceams-shell__logo-mark" aria-hidden="true">C</span>
          <span class="ceams-shell__logo-text">CEAMS</span>
        </div>
        <nav class="ceams-shell__nav"><slot name="aside" /></nav>
      </aside>

      <main class="ceams-shell__main"><slot /></main>
    </div>
  </div>
</template>

<style scoped>
.ceams-shell {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background-color: var(--ceams-bg-page, #f4f6fa);
}

/* ---------- 顶栏 ---------- */
.ceams-shell__header {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  height: var(--ceams-header-height, 60px);
  padding: 0 24px;
  background-color: var(--ceams-bg-card, #ffffff);
  border-bottom: 1px solid var(--ceams-border-base, #e5e7eb);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.03);
  position: relative;
  z-index: 10;
}

.ceams-shell__brand {
  font-size: 16px;
  font-weight: 600;
  color: var(--ceams-text-primary, #1f2937);
  letter-spacing: 0.01em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ceams-shell__toolbar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: var(--ceams-space-3, 12px);
  margin-left: auto;
}

/* ---------- 主体 ---------- */
.ceams-shell__body {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

/* ---------- 侧边栏 ---------- */
.ceams-shell__aside {
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  width: var(--ceams-sidebar-width, 232px);
  background-color: var(--ceams-sidebar-bg, #1b2536);
  overflow-y: auto;
  /* 隐藏滚动条，保留滚动能力 */
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.ceams-shell__aside::-webkit-scrollbar {
  display: none;
}

.ceams-shell__logo {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 10px;
  padding: 16px 18px 18px;
  border-bottom: 1px solid var(--ceams-sidebar-border, rgba(255, 255, 255, 0.06));
}

.ceams-shell__logo-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background-color: var(--ceams-color-primary, #2563eb);
  background-image: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  color: #ffffff;
  font-size: 15px;
  font-weight: 700;
  line-height: 1;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.35);
}

.ceams-shell__logo-text {
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.12em;
  color: var(--ceams-sidebar-text-active, #ffffff);
}

.ceams-shell__nav {
  flex: 1;
  padding: 8px 0 16px;
}

/* ---------- 内容区 ---------- */
.ceams-shell__main {
  flex: 1;
  min-width: 0;
  padding: 20px 24px;
  overflow: auto;
  background-color: var(--ceams-bg-page, #f4f6fa);
}
</style>
