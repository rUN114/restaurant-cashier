<template>
  <el-dialog
    v-model="visible"
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
</template>

<script setup>
import { reactive, ref, nextTick } from 'vue'
import request from '@/utils/request'

const emit = defineEmits(['success', 'fail'])

const visible = defineModel('visible', { type: Boolean, default: false })

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
  emit('fail')
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
    emitCaptcha()
  }

  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onEnd)
  document.addEventListener('touchmove', onMove, { passive: false })
  document.addEventListener('touchend', onEnd)
}

const emitCaptcha = () => {
  const ratio = captchaData.bgWidth / DISPLAY_W
  const lastPt = trackList.value[trackList.value.length - 1]
  if (!lastPt || lastPt.x !== sliderLeft.value) {
    trackList.value.push({ x: sliderLeft.value, y: 0, t: Date.now() - dragStartTime })
  }
  emit('success', {
    captchaId: captchaData.captchaId,
    bgImageWidth: captchaData.bgWidth,
    bgImageHeight: captchaData.bgHeight,
    sliderImageWidth: captchaData.sliderWidth,
    sliderImageHeight: captchaData.sliderHeight,
    startTime: dragStartTime,
    stopTime: Date.now(),
    trackList: trackList.value.map(pt => ({
      x: Math.round(pt.x * ratio),
      y: pt.y,
      t: pt.t,
    })),
  })
}

// 暴露 fetchCaptcha 供父组件调用
defineExpose({ fetchCaptcha })
</script>

<style scoped>
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
