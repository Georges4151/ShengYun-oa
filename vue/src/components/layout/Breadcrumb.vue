<template>
  <el-breadcrumb class="breadcrumb" separator="/">
    <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
      <span v-if="item.redirect === 'noRedirect' || item === breadcrumbs[breadcrumbs.length - 1]" class="no-redirect">
        {{ item.meta?.title }}
      </span>
      <a v-else @click.prevent="handleLink(item)">{{ item.meta?.title }}</a>
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const breadcrumbs = ref([])

function getBreadcrumbs() {
  const matched = route.matched.filter(item => item.meta && item.meta.title)
  const first = matched[0]
  
  if (first && first.path !== '/dashboard') {
    matched.unshift({ path: '/dashboard', meta: { title: '首页' } })
  }
  
  breadcrumbs.value = matched.filter(item => {
    return item.meta && item.meta.title && item.meta.breadcrumb !== false
  })
}

function handleLink(item) {
  const { path, redirect } = item
  if (redirect) {
    router.push(redirect)
    return
  }
  router.push(path)
}

watch(
  () => route.path,
  () => {
    getBreadcrumbs()
  },
  { immediate: true }
)
</script>

<style scoped>
.breadcrumb {
  padding: 16px 20px;
  background-color: #fff;
  border-bottom: 1px solid var(--border-color-lighter);
}

.no-redirect {
  color: var(--text-secondary);
  cursor: text;
}

a {
  color: var(--text-primary);
  cursor: pointer;
}

a:hover {
  color: var(--primary-color);
}
</style>
