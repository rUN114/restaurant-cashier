<template>
  <AppLayout logo-emoji="🍽️" :role-label="$t('role.storeAdmin')" home-path="/manager/home" :menu-items="menuItems" />
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import AppLayout from '@/components/AppLayout.vue'

const { t } = useI18n()

const menuItems = computed(() => [
  { index: '/manager/home',   icon: 'HomeFilled',   label: t('menu.home') },
  { index: '/manager/pos',    icon: 'ShoppingCart',  label: t('menu.pos') },
  { index: '/manager/orders', icon: 'List',          label: t('menu.orderQuery') },
  {
    index: 'system', icon: 'Setting', label: t('menu.systemManage'),
    children: [
      { index: '/manager/stores', icon: 'Shop',    label: t('menu.storeInfo') },
      { index: '/manager/users',  icon: 'User',    label: t('menu.employeeManage') },
      { index: '/manager/tables', icon: 'Grid',    label: t('menu.tableManage') },
      {
        index: 'dish', icon: 'KnifeFork', label: t('menu.dishManage'),
        children: [
          { index: '/manager/dishes',     label: t('menu.dishList') },
          { index: '/manager/categories', label: t('menu.categoryManage') },
        ],
      },
    ],
  },
  {
    index: 'inventory', icon: 'Box', label: t('menu.purchaseInventory'),
    children: [
      { index: '/manager/materials',  label: t('menu.materialManage') },
      { index: '/manager/purchases',  label: t('menu.purchaseManage') },
      { index: '/manager/inventory',  label: t('menu.inventoryQuery') },
    ],
  },
  {
    index: 'cost', icon: 'TrendCharts', label: t('menu.costKitchen'),
    children: [
      { index: '/manager/recipes',      label: t('menu.recipeManage') },
      { index: '/manager/consumptions', label: t('menu.consumptionManage') },
      { index: '/manager/fixed-costs',  label: t('menu.fixedCostManage') },
    ],
  },
  { index: '/manager/report', icon: 'TrendCharts', label: t('menu.report') },
  {
    index: 'logs', icon: 'Document', label: t('menu.logQuery'),
    children: [
      { index: '/manager/logs/login',      label: t('menu.loginLog') },
      { index: '/manager/logs/operation',  label: t('menu.operationLog') },
      { index: '/manager/logs/order-item', label: t('menu.orderItemLog') },
      { index: '/manager/logs/changelog',  label: t('menu.changelog') },
    ],
  },
])
</script>
