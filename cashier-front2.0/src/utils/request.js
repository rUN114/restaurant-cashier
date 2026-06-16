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
    // 401 时清除登录态并跳回登录页
    if (err.response?.status === 401) {
      const cookies = useCookies()
      cookies.remove('cashier-token')
      cookies.remove('cashier-role')
      cookies.remove('cashier-store-id')
      router.push('/login')
    }
    ElMessage.error(err.response?.data?.msg || '请求失败')
    return Promise.reject(err)
  },
)

export default request
