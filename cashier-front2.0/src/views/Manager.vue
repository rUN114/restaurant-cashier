<template>
  <AppLayout logo-emoji="🍽️" :role-label="$t('role.storeAdmin')" home-path="/manager/home" :menu-items="menuItems" />
</template>
<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import AppLayout from '@/components/AppLayout.vue'
const { t } = useI18n()
const menuItems = computed(() => [
  { index: 'dash', icon: 'TrendCharts', label: t('menu.dashboard'), children: [
    { index: '/manager/home', label: t('menu.home') },
    { index: '/manager/report', label: t('menu.report') },
    { index: '/manager/bigscreen', label: t('menu.bigscreen') },
    { index: '/manager/queue-board', label: t('menu.queueBoard') },
  ]},
  { index: '/manager/ai', icon: 'Notebook', label: t('menu.aiSuggest') },
  { index: 'ops', icon: 'ShoppingCart', label: t('menu.dailyOps'), children: [
    { index: '/manager/pos', label: t('menu.pos') },
    { index: '/manager/orders', label: t('menu.orderQuery') },
    { index: '/manager/queue', label: t('menu.queueManage') },
  ]},
  { index: 'dish', icon: 'KnifeFork', label: t('menu.dishManage'), children: [
    { index: '/manager/dishes', label: t('menu.dishList') },
    { index: '/manager/categories', label: t('menu.categoryManage') },
    { index: '/manager/recipes', label: t('menu.recipeManage') },
  ]},
  { index: 'supply', icon: 'Box', label: t('menu.invSupply'), children: [
    { index: '/manager/inventory', label: t('menu.inventoryQuery') },
    { index: '/manager/purchases', label: t('menu.purchaseManage') },
    { index: '/manager/materials', label: t('menu.materialManage') },
    { index: '/manager/consumptions', label: t('menu.consumptionManage') },
    { index: '/manager/fixed-costs', label: t('menu.fixedCostManage') },
  ]},
  { index: 'org', icon: 'OfficeBuilding', label: t('menu.storeHR'), children: [
    { index: '/manager/stores', label: t('menu.storeInfo') },
    { index: '/manager/tables', label: t('menu.tableManage') },
    { index: '/manager/users', label: t('menu.employeeManage') },
  ]},
  { index: 'sys', icon: 'Setting', label: t('menu.sysSettings'), children: [
    { index: '/manager/logs/operation', label: t('menu.operationLog') },
    { index: '/manager/logs/login', label: t('menu.loginLog') },
    { index: '/manager/logs/order-item', label: t('menu.orderItemLog') },
    { index: '/manager/logs/changelog', label: t('menu.changelog') },
  ]},
])
</script>
