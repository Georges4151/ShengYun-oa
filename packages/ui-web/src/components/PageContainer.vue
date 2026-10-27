<script setup lang="ts">
// 页面容器占位组件：标题栏 + 内容区 + 扩展操作区。
defineOptions({ name: 'CeamsPageContainer' })

const props = withDefaults(defineProps<{ title?: string; subtitle?: string }>(), {
  title: '',
  subtitle: ''
})
</script>

<template>
  <section class="ceams-page">
    <header v-if="props.title || $slots.header" class="ceams-page__header">
      <div>
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
  align-items: center;
  justify-content: space-between;
}
.ceams-page__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}
.ceams-page__subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: #6b7280;
}
.ceams-page__content {
  flex: 1;
}
</style>