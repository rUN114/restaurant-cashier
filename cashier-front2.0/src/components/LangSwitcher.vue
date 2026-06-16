<template>
  <el-dropdown trigger="click" @command="handleSwitch">
    <span class="lang-btn" :class="variant">
      <el-icon :size="16"><Globe /></el-icon>
      {{ currentLabel }}
      <el-icon :size="14"><ArrowDown /></el-icon>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="zh-CN">
          <span :class="{ active: locale === 'zh-CN' }">🇨🇳 {{ $t('lang.zhCN') }}</span>
        </el-dropdown-item>
        <el-dropdown-item command="zh-TW">
          <span :class="{ active: locale === 'zh-TW' }">🇹🇼 {{ $t('lang.zhTW') }}</span>
        </el-dropdown-item>
        <el-dropdown-item command="en">
          <span :class="{ active: locale === 'en' }">🇺🇸 {{ $t('lang.en') }}</span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  variant: { type: String, default: 'light' }, // 'light' | 'dark'
})

const { locale } = useI18n()

const labelMap = { 'zh-CN': '简体中文', 'zh-TW': '繁體中文', en: 'English' }
const currentLabel = computed(() => labelMap[locale.value] || '简体中文')

const handleSwitch = (lang) => {
  locale.value = lang
  localStorage.setItem('app-lang', lang)
}
</script>

<style scoped>
.lang-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 13px;
  padding: 6px 14px;
  border-radius: 6px;
  user-select: none;
  transition: all 0.2s;
  white-space: nowrap;
}
.lang-btn.light {
  color: #333;
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
}
.lang-btn.light:hover {
  color: #409eff;
  border-color: #409eff;
  background: #ecf5ff;
}
.lang-btn.dark {
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.25);
}
.lang-btn.dark:hover {
  background: rgba(255, 255, 255, 0.22);
}
.active {
  color: #409eff;
  font-weight: 600;
}
</style>
