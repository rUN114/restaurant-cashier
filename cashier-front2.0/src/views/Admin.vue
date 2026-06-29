<template>
  <AppLayout logo-emoji="🍽️" :role-label="$t('role.admin')" home-path="/admin/home" :menu-items="menuItems" />
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import AppLayout from '@/components/AppLayout.vue'

const { t } = useI18n()

const menuItems = computed(() => [
  // ====== 📊 经营看板 ======
  {
    index: 'dashboard', icon: 'DataAnalysis' in window ? 'TrendCharts' : 'TrendCharts', label: '经营看板',
    children: [
      { index: '/admin/home',      icon: 'HomeFilled',  label: t('menu.home') },
      { index: '/admin/report',    icon: 'TrendCharts', label: t('menu.report') },
      { index: '/admin/bigscreen', icon: 'Monitor',     label: '实时大屏' },
      { index: '/admin/queue-board', icon: 'Monitor',   label: '等位大屏' },
    ],
  },

  // ====== 🤖 AI 决策 ======
  { index: '/admin/ai', icon: 'Notebook', label: '🤖 AI预制建议' },

  // ====== 💰 日常运营 ======
  {
    index: 'ops', icon: 'ShoppingCart', label: '日常运营',
    children: [
      { index: '/admin/pos',    icon: 'ShoppingCartFull', label: t('menu.pos') },
      { index: '/admin/orders', icon: 'List',             label: t('menu.orderQuery') },
      { index: '/admin/queue',  icon: 'List',             label: '等位管理' },
    ],
  },

  // ====== 🍳 菜品管理 ======
  {
    index: 'dish', icon: 'KnifeFork', label: '菜品管理',
    children: [
      { index: '/admin/dishes',     label: t('menu.dishList') },
      { index: '/admin/categories', label: t('menu.categoryManage') },
      { index: '/admin/recipes',    label: t('menu.recipeManage') },
    ],
  },

  // ====== 📦 进销存 ======
  {
    index: 'supply', icon: 'Box', label: '进销存',
    children: [
      { index: '/admin/inventory',    label: t('menu.inventoryQuery') },
      { index: '/admin/purchases',    label: t('menu.purchaseManage') },
      { index: '/admin/materials',    label: t('menu.materialManage') },
      { index: '/admin/consumptions', label: t('menu.consumptionManage') },
      { index: '/admin/fixed-costs',  label: t('menu.fixedCostManage') },
    ],
  },

  // ====== 👥 会员管理 ======
  {
    index: 'member', icon: 'UserFilled', label: '会员管理',
    children: [
      { index: '/admin/members',       label: t('menu.memberList') },
      { index: '/admin/member-levels', label: t('menu.memberLevel') },
      { index: '/admin/member-points', label: t('menu.memberPoints') },
    ],
  },

  // ====== 🏠 门店与人 ======
  {
    index: 'org', icon: 'OfficeBuilding', label: '门店与人员',
    children: [
      { index: '/admin/stores', label: t('menu.storeManage') },
      { index: '/admin/tables', label: t('menu.tableManage') },
      { index: '/admin/users',  label: t('menu.employeeManage') },
      { index: '/admin/roles',  label: t('menu.roleManage') },
    ],
  },

  // ====== ⚙️ 系统 ======
  {
    index: 'system', icon: 'Setting', label: '系统设置',
    children: [
      { index: '/admin/sys-config',        label: '系统配置' },
      { index: '/admin/logs/operation',    label: t('menu.operationLog') },
      { index: '/admin/logs/login',        label: t('menu.loginLog') },
      { index: '/admin/logs/order-item',   label: t('menu.orderItemLog') },
      { index: '/admin/logs/changelog',    label: t('menu.changelog') },
    ],
  },
])
</script>
