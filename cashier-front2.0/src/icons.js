// 按需注册 Element Plus 图标，替代 main.js 中 import * 的全量注册
// 审计了所有 .vue 文件中实际使用的图标，仅注册以下 ~20 个

import {
  HomeFilled,
  ShoppingCart,
  ShoppingCartFull,
  List,
  KnifeFork,
  Grid,
  Shop,
  User,
  Setting,
  Box,
  TrendCharts,
  Document,
  Lock,
  Goods,
  Notebook,
  Money,
  UserFilled,
  Globe,
  ArrowDown,
} from '@element-plus/icons-vue'

const icons = {
  HomeFilled,
  ShoppingCart,
  ShoppingCartFull,
  List,
  KnifeFork,
  Grid,
  Shop,
  User,
  Setting,
  Box,
  TrendCharts,
  Document,
  Lock,
  Goods,
  Notebook,
  Money,
  UserFilled,
  Globe,
  ArrowDown,
}

export function registerIcons(app) {
  for (const [name, component] of Object.entries(icons)) {
    app.component(name, component)
  }
}
