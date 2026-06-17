import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import i18n from '@/locales'

// 引入 Element Plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// 按需注册 Element Plus 图标（替代 import * 全量注册）
import { registerIcons } from '@/icons'

import '@/styles/global.scss'

const app = createApp(App)

registerIcons(app)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.use(i18n)

app.mount('#app')
