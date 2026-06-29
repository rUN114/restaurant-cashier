<template>
  <AppLayout logo-emoji="🍽️" :role-label="$t('role.storeAdmin')" home-path="/manager/home" :menu-items="menuItems" />
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import AppLayout from '@/components/AppLayout.vue'

const { t } = useI18n()

const menuItems = computed(() => [
  // 经营看板
  {
    index: 'dash', icon: 'TrendCharts', label: '经营看板',
    children: [
      { index: '/manager/home',      label: t('menu.home') },
      { index: '/manager/report',    label: t('menu.report') },
      { index: '/manager/bigscreen', label: '实时大屏' },
      { index: '/manager/queue-board', label: '等位大屏' },
    ],
  },
  // AI
  { index: '/manager/ai', icon: 'Notebook', label: '🤖 AI预制建议' },
  // 日常运营
  {
    index: 'ops', icon: 'ShoppingCart', label: '日常运营',
    children: [
      { index: '/manager/pos',    label: t('menu.pos') },
      { index: '/manager/orders', label: t('menu.orderQuery') },
      { index: '/manager/queue',  label: '等位管理' },
    ],
  },
  // 菜品管理
  {
    index: 'dish', icon: 'KnifeFork', label: '菜品管理',
    children: [
      { index: '/manager/dishes',     label: t('menu.dishList') },
      { index: '/manager/categories', label: t('menu.categoryManage') },
      { index: '/manager/recipes',    label: t('menu.recipeManage') },
    ],
  },
  // 进销存
  {
    index: 'supply', icon: 'Box', label: '进销存',
    children: [
      { index: '/manager/inventory',    label: t('menu.inventoryQuery') },
      { index: '/manager/purchases',    label: t('menu.purchaseManage') },
      { index: '/manager/materials',    label: t('menu.materialManage') },
      { index: '/manager/consumptions', label: t('menu.consumptionManage') },
      { index: '/manager/fixed-costs',  label: t('menu.fixedCostManage') },
    ],
  },
  // 门店
  {
    index: 'store', icon: 'OfficeBuilding', label: '门店管理',
    children: [
      { index: '/manager/stores', label: t('menu.storeInfo') },
      { index: '/manager/tables', label: t('menu.tableManage') },
      { index: '/manager/users',  label: t('menu.employeeManage') },
    ],
  },
  // 系统
  {
    index: 'sys', icon: 'Setting', label: '系统',
    children: [
      { index: '/manager/logs/operation',  label: t('menu.operationLog') },
      { index: '/manager/logs/login',      label: t('menu.loginLog') },
      { index: '/manager/logs/order-item', label: t('menu.orderItemLog') },
      { index: '/manager/logs/changelog',  label: t('menu.changelog') },
    ],
  },
])
</script>
