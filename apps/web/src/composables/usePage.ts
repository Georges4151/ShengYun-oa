import { ref } from 'vue'
import type { Ref } from 'vue'

/** 分页状态组合式函数返回的结构。 */
export interface PageState {
  page: Ref<number>
  size: Ref<number>
  total: Ref<number>
  resetPage: () => void
}

/**
 * 分页状态组合式函数：统一 page / size / total 管理，供各列表组件复用。
 */
export function usePage(initialSize = 20): PageState {
  const page = ref(1)
  const size = ref(initialSize)
  const total = ref(0)

  /** 查询条件变化时回到第一页。 */
  function resetPage(): void {
    page.value = 1
  }

  return { page, size, total, resetPage }
}
