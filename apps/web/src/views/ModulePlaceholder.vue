<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElEmpty } from 'element-plus'
import { PageContainer } from '@ceams/ui-web'

const route = useRoute()
const title = computed(() => String(route.meta.title ?? '模块'))
</script>

<template>
  <PageContainer :title="title" subtitle="该模块页面为占位，待后端契约冻结后实现">
    <el-empty :description="`「${title}」功能建设中`" />
  </PageContainer>
</template>