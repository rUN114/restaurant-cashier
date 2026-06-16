import { createI18n } from 'vue-i18n'
import zhCN from '@/locales/zh-CN'
import zhTW from '@/locales/zh-TW'
import en from '@/locales/en'

// 从 localStorage 读取上次选择的语言，默认简体中文
const savedLang = localStorage.getItem('app-lang') || 'zh-CN'

const i18n = createI18n({
  legacy: false,        // 使用 Composition API 模式
  locale: savedLang,
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'zh-TW': zhTW,
    en,
  },
})

export default i18n
