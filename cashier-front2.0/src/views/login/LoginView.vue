<!-- eslint-disable vue/multi-word-component-names -->
<template>
  <div class="login-page">
    <div class="lang-bar">
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

    <!-- 滑块验证码弹窗 -->
    <el-dialog
      v-model="captchaVisible"
      :close-on-click-modal="false"
      width="340px"
      class="captcha-dialog"
      @opened="onDialogOpened"
      @closed="onDialogClosed"
    >
      <div class="captcha-box">
        <div class="captcha-image-wrapper" ref="captchaWrapper">
          <img
            :src="captchaData.backgroundImage"
            class="captcha-bg-img"
            ref="bgImg"
            alt="captcha"
          />
          <img
            v-if="captchaData.sliderImage"
            :src="captchaData.sliderImage"
            class="captcha-slider-piece"
            :style="sliderStyle"
            alt="slider"
          />
        </div>
        <div class="captcha-track-bar" ref="trackBarRef">
          <div class="captcha-track-fill" :style="{ width: sliderLeft + 'px' }"></div>
          <div class="captcha-track-btn" :style="{ left: sliderLeft + 'px' }" :class="{ dragging: isDragging }">→</div>
          <span class="captcha-track-hint" v-if="!isDragging">拖动滑块完成验证</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { User, Lock } from '@element-plus/icons-vue'
import { ElNotification } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'
import LangSwitcher from '@/components/LangSwitcher.vue'

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

const captchaData = reactive({
  captchaId: '',
  backgroundImage: '',
  sliderImage: '',
  bgWidth: 600,
  bgHeight: 360,
  sliderWidth: 110,
  sliderHeight: 360,
})

// 固定显示宽度 280px，等比计算
const DISPLAY_W = 280
const displayW = ref(DISPLAY_W)
const displayH = ref(0)
const displaySliderW = ref(0)
const sliderStyle = ref({})

const sliderLeft = ref(0)
const isDragging = ref(false)
const trackList = ref([])
const bgImg = ref(null)
const trackBarRef = ref(null)
let dragStartTime = 0
let dragStartX = 0
let lastTrackTime = 0
let pendingLogin = false

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

// 根据原始尺寸计算显示尺寸
const calcSizes = () => {
  const r = DISPLAY_W / captchaData.bgWidth
  displayH.value = Math.round(captchaData.bgHeight * r)
  displaySliderW.value = Math.round(captchaData.sliderWidth * r)
  sliderStyle.value = {
    left: '0px',
    top: '0px',
    width: displaySliderW.value + 'px',
    height: displayH.value + 'px',
  }
}

calcSizes()

const fetchCaptcha = async () => {
  try {
    const res = await request.post('/captcha/generate')
    if (res.code === 200 && res.data) {
      Object.assign(captchaData, res.data)
      captchaData.bgWidth = res.data.bgWidth || 600
      captchaData.bgHeight = res.data.bgHeight || 360
      captchaData.sliderWidth = res.data.sliderWidth || 110
      captchaData.sliderHeight = res.data.sliderHeight || 360
      sliderLeft.value = 0
      trackList.value = []
      calcSizes()
    }
  } catch (e) {
    console.error('获取验证码失败', e)
  }
}

// dialog 完全打开后绑定拖拽事件
const onDialogOpened = () => {
  const bar = trackBarRef.value
  if (!bar) return
  bar.onmousedown = onDragStart
  bar.ontouchstart = (e) => { e.preventDefault(); onDragStart(e) }
}

const onDialogClosed = () => {
  pendingLogin = false
  loginLoading.value = false
}

const onDragStart = (e) => {
  e.preventDefault()
  isDragging.value = true
  dragStartTime = Date.now()
  const clientX = e.clientX || (e.touches && e.touches[0].clientX) || 0
  dragStartX = clientX - sliderLeft.value
  lastTrackTime = 0
  trackList.value = [{ x: sliderLeft.value, y: 0, t: 0 }]

  const onMove = (ev) => {
    if (!isDragging.value) return
    const cx = ev.clientX || (ev.touches && ev.touches[0].clientX) || 0
    const maxLeft = DISPLAY_W - displaySliderW.value
    const newLeft = Math.max(0, Math.min(cx - dragStartX, maxLeft))
    sliderLeft.value = newLeft
    sliderStyle.value = { ...sliderStyle.value, left: newLeft + 'px' }
    const elapsed = Date.now() - dragStartTime
    if (elapsed - lastTrackTime > 16) {
      trackList.value.push({ x: newLeft, y: 0, t: elapsed })
      lastTrackTime = elapsed
    }
  }

  const onEnd = () => {
    isDragging.value = false
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onEnd)
    document.removeEventListener('touchmove', onMove)
    document.removeEventListener('touchend', onEnd)
    doLogin()
  }

  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onEnd)
  document.addEventListener('touchmove', onMove, { passive: false })
  document.addEventListener('touchend', onEnd)
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
    } catch { /* ignore */ }
  }
})

const onSubmit = async () => {
  try { await formref.value.validate() } catch { return }

  if (!isDev || !SKIP_CAPTCHA_IN_DEV) {
    pendingLogin = true
    captchaVisible.value = true
    await fetchCaptcha()
    return
  }

  doLogin()
}

const doLogin = async () => {
  loginLoading.value = true

  const loginPayload = { username: form.username, password: form.password }

  if (captchaData.captchaId && captchaVisible.value) {
    const ratio = captchaData.bgWidth / DISPLAY_W
    loginPayload.captchaId = captchaData.captchaId
    loginPayload.bgImageWidth = captchaData.bgWidth
    loginPayload.bgImageHeight = captchaData.bgHeight
    loginPayload.sliderImageWidth = captchaData.sliderWidth
    loginPayload.sliderImageHeight = captchaData.sliderHeight
    loginPayload.startTime = dragStartTime
    loginPayload.stopTime = Date.now()
    const lastPt = trackList.value[trackList.value.length - 1]
    if (!lastPt || lastPt.x !== sliderLeft.value) {
      trackList.value.push({ x: sliderLeft.value, y: 0, t: Date.now() - dragStartTime })
    }
    loginPayload.trackList = trackList.value.map(pt => ({
      x: Math.round(pt.x * ratio),
      y: pt.y,
      t: pt.t,
    }))
  }

  try {
    const res = await request.post('/users/login', loginPayload)

    if (res.code === 200) {
      captchaVisible.value = false
      pendingLogin = false
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
      if (captchaVisible.value) fetchCaptcha()
    }
  } catch (err) {
    ElNotification.error(t('login.loginFailed'))
    if (captchaVisible.value) fetchCaptcha()
  } finally {
    loginLoading.value = false
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
html, body, #app {
  width: 100%;
  height: 100%;
  overflow-x: hidden;
}
</style>

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
}

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
  background: linear-gradient(to right, #a8edea, #fed6e3);
  border: none;
  color: #333;
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

/* 验证码弹窗样式 */
.captcha-box {
  user-select: none;
  padding: 10px 0;
}

.captcha-image-wrapper {
  position: relative;
  width: 280px;
  margin: 0 auto 8px;
  border-radius: 4px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}

.captcha-bg-img {
  display: block;
  width: 280px;
  height: auto;
  pointer-events: none;
}

.captcha-slider-piece {
  position: absolute;
  top: 0;
  pointer-events: none;
  filter: drop-shadow(1px 1px 2px rgba(0, 0, 0, 0.3));
}

.captcha-track-bar {
  position: relative;
  width: 100%;
  height: 36px;
  background: #eee;
  border-radius: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.captcha-track-fill {
  position: absolute;
  left: 0; top: 0;
  height: 100%;
  background: linear-gradient(to right, #a8edea, #fed6e3);
  border-radius: 18px 0 0 18px;
}

.captcha-track-btn {
  position: absolute;
  top: -2px;
  width: 40px;
  height: 40px;
  background: #fff;
  border: 2px solid #ccc;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
  font-size: 18px;
  color: #999;
  transition: border-color 0.2s;
  z-index: 2;
}

.captcha-track-btn.dragging {
  cursor: grabbing;
  border-color: #409eff;
}

.captcha-track-hint {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  font-size: 12px;
  color: #999;
  pointer-events: none;
  z-index: 1;
}
</style>
