<!-- eslint-disable vue/multi-word-component-names -->
<template>
  <div class="login-page">
    <div class="lang-bar">
      <el-button link class="theme-btn" @click="toggleTheme">{{ isDark ? '☀️' : '🌙' }}</el-button>
      <LangSwitcher variant="light" />
    </div>

    <div class="main-container">
      <div class="left-bg">
        <div class="bg-overlay">
          <div class="shopping-text">CASHIER</div>
        </div>
      </div>

      <div class="right-form">
        <div class="login-card">
          <h2 class="card-title">{{ $t('login.title') }}</h2>
          <p class="card-subtitle"></p>

          <el-form ref="formref" :model="form" :rules="rules" label-width="auto">
            <el-form-item prop="username">
              <el-input v-model="form.username" :placeholder="$t('login.usernamePlaceholder')">
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                :placeholder="$t('login.passwordPlaceholder')"
                show-password
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item>
              <div style="display: flex; align-items: center; gap: 8px">
                <el-checkbox v-model="rememberMe">{{ $t('login.rememberMe') }}</el-checkbox>
                <el-select
                  v-model="rememberDays"
                  size="small"
                  style="width: 100px"
                  :disabled="!rememberMe"
                >
                  <el-option :label="$t('login.days7')" :value="7"></el-option>
                  <el-option :label="$t('login.days15')" :value="15"></el-option>
                  <el-option :label="$t('login.days30')" :value="30"></el-option>
                </el-select>
              </div>
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                class="login-btn"
                :loading="loginLoading"
                :disabled="!captchaReady"
                @click="onSubmit"
              >{{ $t('login.loginBtn') }}</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>

    <!-- 滑块验证码组件 -->
    <CaptchaSlider ref="captchaRef" v-model:visible="captchaVisible" @success="onCaptchaSuccess" @fail="onCaptchaFail" />
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { User, Lock } from '@element-plus/icons-vue'
import { ElNotification } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'
import LangSwitcher from '@/components/LangSwitcher.vue'
import CaptchaSlider from '@/components/CaptchaSlider.vue'
import { useTheme } from '@/composables/useTheme'
const { isDark, toggle: toggleTheme } = useTheme()

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()

const rememberMe = ref(false)
const rememberDays = ref(7)

const isDev = import.meta.env.DEV
const SKIP_CAPTCHA_IN_DEV = true

const captchaReady = ref(true)
const loginLoading = ref(false)
const captchaVisible = ref(false)
const captchaRef = ref(null)
let pendingCaptchaPayload = null

const form = reactive({ username: 'admin', password: '123456' })

const rules = {
  username: [{ required: true, message: t('login.usernamePlaceholder'), trigger: 'blur' }],
  password: [{ required: true, message: t('login.passwordPlaceholder'), trigger: 'blur' }],
}

const formref = ref(null)

const ROLE_MAP = {
  admin_global: 'ADMIN', admin_store: 'STORE_ADMIN', cashier: 'CASHIER',
  waiter: 'WAITER', purchaser: 'PURCHASER', buyer: 'PURCHASER', chef: 'CHEF',
}

onMounted(() => {
  const data = localStorage.getItem('loginRemember')
  if (data) {
    try {
      const { username, password, expire } = JSON.parse(data)
      if (Date.now() <= expire) {
        form.username = username
        form.password = password
      } else {
        localStorage.removeItem('loginRemember')
      }
    } catch { /* localStorage 解析失败，清除残余数据 */ }
  }
})

const onSubmit = async () => {
  try { await formref.value.validate() } catch { return }

  if (!isDev || !SKIP_CAPTCHA_IN_DEV) {
    captchaVisible.value = true
    await captchaRef.value.fetchCaptcha()
    return
  }

  await doLogin()
}

const onCaptchaSuccess = async (captchaPayload) => {
  pendingCaptchaPayload = captchaPayload
  await doLogin(captchaPayload)
}

const onCaptchaFail = () => {
  loginLoading.value = false
}

const doLogin = async (captchaPayload = null) => {
  loginLoading.value = true

  const loginPayload = { username: form.username, password: form.password }

  if (captchaPayload) {
    Object.assign(loginPayload, captchaPayload)
  }

  try {
    const res = await request.post('/users/login', loginPayload)

    if (res.code === 200) {
      captchaVisible.value = false
      ElNotification.success(t('login.loginSuccess'))

      if (rememberMe.value) {
        const expire = Date.now() + rememberDays.value * 86400000
        localStorage.setItem('loginRemember', JSON.stringify({
          username: form.username, password: form.password, expire,
        }))
      } else {
        localStorage.removeItem('loginRemember')
      }

      const { token, roleCode, userId, username, realName, storeId } = res.data
      const internalRole = ROLE_MAP[roleCode] || roleCode
      userStore.setUserInfo(token, internalRole, { userId, username, realName, storeId })

      const rolePathMap = {
        ADMIN: '/admin', STORE_ADMIN: '/manager', CASHIER: '/cashier',
        WAITER: '/waiter', PURCHASER: '/buyer', CHEF: '/chef',
      }
      router.push(rolePathMap[internalRole] || '/')
    } else {
      ElNotification.error(res.msg || t('login.loginFailed'))
      if (captchaVisible.value) captchaRef.value.fetchCaptcha()
    }
  } catch (err) {
    ElNotification.error(t('login.loginFailed'))
    if (captchaVisible.value) captchaRef.value.fetchCaptcha()
  } finally {
    loginLoading.value = false
  }
}
</script>

<style scoped>
.login-page {
  width: 100%;
  min-height: 100vh;
  background: linear-gradient(to right, rgba(173, 216, 230, 0.3), rgba(255, 192, 203, 0.3));
  overflow-x: hidden;
  position: relative;
}

.lang-bar {
  position: absolute;
  top: 20px;
  right: 30px;
  z-index: 200;
  display: flex;
  align-items: center;
  gap: 8px;
}
.theme-btn { font-size: 22px; }

.nav-bar {
  position: fixed;
  top: 0; left: 0;
  width: 100%;
  padding: 20px 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  z-index: 100;
  flex-wrap: wrap;
}
.logo { font-size: 24px; font-weight: bold; }
.nav-links { display: flex; gap: 20px; flex-wrap: wrap; }
.nav-links span { cursor: pointer; white-space: nowrap; }
.nav-links span:hover { color: #409eff; }

.main-container {
  display: flex;
  width: 100%;
  min-height: 100vh;
}

.left-bg {
  flex: 2;
  background: url('@/assets/登录首页.png') center/cover no-repeat;
  min-height: 100vh;
  position: relative;
}
.bg-overlay {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}
.shopping-text {
  font-size: 64px;
  color: #fff;
  font-weight: bold;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.2);
}

.right-form {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.login-card {
  width: 300px;
  padding: 30px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.05);
}

.card-title {
  font-size: 20px;
  margin-bottom: 5px;
  text-align: center;
  color: #333;
}
.card-subtitle {
  font-size: 13px;
  color: #666;
  margin-bottom: 30px;
  text-align: center;
}

.login-btn {
  width: 100%;
  background: linear-gradient(135deg, #409eff, #66b1ff);
  border: none;
  color: #fff;
  font-weight: 500;
  transition: all 0.3s;
}
.login-btn:hover {
  opacity: 0.9;
  transform: scale(0.98);
}

@media screen and (max-width: 768px) {
  .nav-bar {
    padding: 15px 20px;
    background: rgba(255, 255, 255, 0.6);
    backdrop-filter: blur(4px);
  }
  .main-container { flex-direction: column; }
  .left-bg { min-height: 40vh; height: 40vh; }
  .shopping-text { font-size: 36px; }
  .right-form { align-items: flex-start; padding: 40px 20px; }
  .login-card { width: 100%; max-width: 320px; margin: 0 auto; }
}
</style>
