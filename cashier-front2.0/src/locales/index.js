import { createI18n } from 'vue-i18n'
import zhCN from '@/locales/zh-CN'
import zhHK from '@/locales/zh-HK'
import en from '@/locales/en'

// 从 localStorage 读取上次选择的语言，默认简体中文
const savedLang = localStorage.getItem('app-lang') || 'zh-CN'
// 兼容旧值 zh-TW → zh-HK
const locale = savedLang === 'zh-TW' ? 'zh-HK' : savedLang

const i18n = createI18n({
  legacy: false,
  locale,
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'zh-HK': zhHK,
    en,
  },
})

export default i18n
