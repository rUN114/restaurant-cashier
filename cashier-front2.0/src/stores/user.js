import { defineStore } from 'pinia'
import { useCookies } from '@vueuse/integrations/useCookies'
import { getCurrentUser } from '@/api'

const cookies = useCookies(['cashier-token', 'cashier-role', 'cashier-store-id'])

export const useUserStore = defineStore('user', {
  state: () => ({
    token: cookies.get('cashier-token') || '',
    role: cookies.get('cashier-role')?.trim() || '',
    storeId: cookies.get('cashier-store-id') || null,
    userId: null,
    username: '',
    realName: '',
    roleCode: cookies.get('cashier-role')?.trim() || '',
  }),

  actions: {
    // 登录成功后调用
    setUserInfo(token, role, userData = {}) {
      this.token = token
      this.role = role
      this.roleCode = role

      cookies.set('cashier-token', token)
      cookies.set('cashier-role', role)
      if (userData.storeId) {
        cookies.set('cashier-store-id', userData.storeId)
        this.storeId = userData.storeId
      }

      this.userId = userData.userId || userData.id
      this.username = userData.username || ''
      this.realName = userData.realName || ''
    },

    // 利用 token 获取完整用户信息（用于刷新页面后恢复状态）
    async fetchCurrentUser() {
      try {
        const res = await getCurrentUser()
        if (res.code === 200) {
          const d = res.data
          this.userId = d.userId || d.id
          this.username = d.username
          this.realName = d.realName
          this.storeId = d.storeId
          this.role = d.roleCode
          this.roleCode = d.roleCode
          // 同步 cookie
          cookies.set('cashier-role', d.roleCode)
          if (d.storeId) cookies.set('cashier-store-id', d.storeId)
        } else {
          this.logout()
        }
      } catch {
        this.logout()
      }
    },

    logout() {
      this.token = ''
      this.role = ''
      this.roleCode = ''
      this.userId = null
      this.username = ''
      this.realName = ''
      this.storeId = null

      cookies.remove('cashier-token')
      cookies.remove('cashier-role')
      cookies.remove('cashier-store-id')
    },
  },
})
