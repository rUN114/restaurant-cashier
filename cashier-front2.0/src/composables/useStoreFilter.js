import { ref, computed, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { getValidStores } from '@/api'

/**
 * 门店筛选器组合函数
 *
 * - admin_global：下拉列出所有有效门店，手动选择
 * - 其他角色（店长/采购/厨师/收银/服务员）：自动锁定为本店，下拉禁用，不可切换
 *
 * 用法：
 *   const { stores, selectedStoreId, isLocked, loadStores } = useStoreFilter()
 *   onMounted(() => loadStores())
 *   模板中 :disabled="isLocked" 绑定到 el-select
 */
export function useStoreFilter() {
  const user = useUserStore()
  const stores = ref([])
  const selectedStoreId = ref(null)
  const isLocked = ref(false)

  // 加载有效门店列表
  const loadStores = async () => {
    try {
      const res = await getValidStores()
      stores.value = res.data || []
    } catch {
      stores.value = []
    }

    // 非管理员角色：自动锁定自己的门店，不可切换
    const lockedRoles = ['STORE_ADMIN', 'PURCHASER', 'CHEF', 'CASHIER', 'WAITER']
    if (lockedRoles.includes(user.roleCode)) {
      if (user.storeId) {
        selectedStoreId.value = user.storeId
        isLocked.value = true
      }
    }
  }

  const getStoreName = (storeId) => {
    if (!storeId) return '-'
    const store = stores.value.find((s) => s.id === storeId)
    return store ? store.name : '-'
  }

  return {
    stores,
    selectedStoreId,
    isLocked,
    loadStores,
    getStoreName,
  }
}
