<template>
  <AppLayout logo-emoji="🍽️" :role-label="$t('role.storeAdmin')" home-path="/manager/home" :menu-items="menuItems" />
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import AppLayout from '@/components/AppLayout.vue'

const { t } = useI18n()

const menuItems = computed(() => [
  // 第一梯队：日常最高频操作
  { index: '/manager/home',   icon: 'HomeFilled',   label: t('menu.home') },
  { index: '/manager/pos',    icon: 'ShoppingCart',  label: t('menu.pos') },
  { index: '/manager/orders', icon: 'List',          label: t('menu.orderQuery') },
  // 营业统计：店长日常核心职责，上移
  { index: '/manager/report', icon: 'TrendCharts', label: t('menu.report') },
  { index: '/manager/bigscreen', icon: 'Monitor', label: '实时大屏' },

  // 第二梯队：门店与人员（店长看本店信息、管员工、管餐桌）
  {
    index: 'org', icon: 'OfficeBuilding', label: t('menu.storesAndStaff'),
    children: [
      { index: '/manager/stores', icon: 'Shop',    label: t('menu.storeInfo') },
      { index: '/manager/users',  icon: 'User',    label: t('menu.employeeManage') },
      { index: '/manager/tables', icon: 'Grid',    label: t('menu.tableManage') },
    ],
  },
  // 菜品管理独立
  {
    index: 'dish', icon: 'KnifeFork', label: t('menu.dishManage'),
    children: [
      { index: '/manager/dishes',     label: t('menu.dishList') },
      { index: '/manager/categories', label: t('menu.categoryManage') },
    ],
  },

  // 第三梯队：供应链
  {
    index: 'supply', icon: 'Box', label: t('menu.supplyChain'),
    children: [
      { index: '/manager/materials',     label: t('menu.materialManage') },
      { index: '/manager/purchases',     label: t('menu.purchaseManage') },
      { index: '/manager/inventory',     label: t('menu.inventoryQuery') },
      { index: '/manager/recipes',       label: t('menu.recipeManage') },
      { index: '/manager/consumptions',  label: t('menu.consumptionManage') },
      { index: '/manager/fixed-costs',   label: t('menu.fixedCostManage') },
    ],
  },

  // 第四梯队：系统日志
  {
    index: 'logs', icon: 'Document', label: t('menu.systemLogs'),
    children: [
      { index: '/manager/logs/operation',  label: t('menu.operationLog') },
      { index: '/manager/logs/login',      label: t('menu.loginLog') },
      { index: '/manager/logs/order-item', label: t('menu.orderItemLog') },
      { index: '/manager/logs/changelog',  label: t('menu.changelog') },
    ],
  },
])
</script>
