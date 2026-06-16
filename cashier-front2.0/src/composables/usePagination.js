import { ref, reactive, computed } from 'vue'

/**
 * 可复用的服务端分页组合式函数
 *
 * @param {Function} fetchFn — 异步函数 (params) => request，返回 { data: { total, pages, data: [...] } }
 * @param {Object} defaultFilters — 初始筛选条件
 * @param {Number} defaultPageSize — 默认每页条数 (默认 10)
 */
export function usePagination(fetchFn, defaultFilters = {}, defaultPageSize = 10) {
  const pageNum = ref(1)
  const pageSize = ref(defaultPageSize)
  const total = ref(0)
  const pages = ref(0)
  const loading = ref(false)
  const data = ref([])
  const filters = reactive({ ...defaultFilters })

  const pagination = computed(() => ({
    currentPage: pageNum.value,
    pageSize: pageSize.value,
    total: total.value,
    pageSizes: [10, 20, 50, 100],
    layout: 'total, sizes, prev, pager, next, jumper',
  }))

  const load = async () => {
    loading.value = true
    try {
      const params = {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        ...filters,
      }
      const res = await fetchFn(params)
      // 兼容 PageResult 格式 { total, pages, data } 和普通 Result 格式 { data: [...] }
      // 注意：request 拦截器已经解包了一层 res.data，所以 res 本身就是后端返回的 body
      if (res && typeof res.total !== 'undefined') {
        total.value = res.total
        pages.value = res.pages
        data.value = res.data || []
      } else {
        // 普通 Result 格式 { data: [...] } 或直接返回数组
        const body = res?.data
        data.value = body || res || []
        total.value = data.value.length
        pages.value = 1
      }
    } catch (e) {
      data.value = []
      total.value = 0
      pages.value = 0
    } finally {
      loading.value = false
    }
  }

  const onPageChange = (val) => {
    pageNum.value = val
    load()
  }

  const onSizeChange = (val) => {
    pageSize.value = val
    pageNum.value = 1
    load()
  }

  const resetAndLoad = () => {
    pageNum.value = 1
    load()
  }

  return {
    pageNum,
    pageSize,
    total,
    pages,
    loading,
    data,
    filters,
    pagination,
    load,
    onPageChange,
    onSizeChange,
    resetAndLoad,
  }
}
