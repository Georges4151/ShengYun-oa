<script setup lang="ts">
// 页面容器：标题区（标题 / 副标题 / 右侧操作）+ 内容区
defineOptions({ name: 'CeamsPageContainer' })

const props = withDefaults(defineProps<{ title?: string; subtitle?: string }>(), {
  title: '',
  subtitle: ''
})
</script>

<template>
  <section class="ceams-page">
    <header v-if="props.title || $slots.header" class="ceams-page__header">
      <div class="ceams-page__heading">
        <h1 v-if="props.title" class="ceams-page__title">{{ props.title }}</h1>
        <p v-if="props.subtitle" class="ceams-page__subtitle">{{ props.subtitle }}</p>
      </div>
      <div class="ceams-page__extra"><slot name="extra" /></div>
    </header>
    <div class="ceams-page__content"><slot /></div>
  </section>
</template>

<style scoped>
.ceams-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ceams-page__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--ceams-space-4, 16px);
}

.ceams-page__heading {
  min-width: 0;
}

.ceams-page__title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--ceams-text-primary, #1f2937);
}

.ceams-page__subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--ceams-text-secondary, #6b7280);
}

.ceams-page__extra {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: var(--ceams-space-2, 8px);
  margin-left: auto;
}

.ceams-page__content {
  flex: 1;
  min-width: 0;
}
</style>
