import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useCookies } from '@vueuse/integrations/useCookies'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
})

// 请求拦截
request.interceptors.request.use((config) => {
  const cookies = useCookies()
  const token = cookies.get('cashier-token')
  const storeId = cookies.get('cashier-store-id')

  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }

  if (storeId) {
    config.params = config.params || {}
    config.params.store_id = storeId
  }

  return config
})

// 响应拦截
request.interceptors.response.use(
  (res) => res.data,
  (err) => {
    const status = err.response?.status
    const msg = err.response?.data?.msg || '请求失败'

    // 401 时清除登录态并跳回登录页
    if (status === 401) {
      import('@/stores/user').then(({ useUserStore }) => {
        useUserStore().logout()
        router.push('/login')
      })
    }
    // 403 分店停用：清除登录态 + 提示 + 跳转
    else if (status === 403 && msg.includes('分店已停用')) {
      import('@/stores/user').then(({ useUserStore }) => {
        useUserStore().logout()
        router.push('/login')
      })
      ElMessage.error(msg)
    } else {
      ElMessage.error(msg)
    }
    return Promise.reject(err)
  },
)

export default request
