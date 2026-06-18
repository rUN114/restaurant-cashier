<template>
  <AppLayout logo-emoji="🍽️" :role-label="$t('role.admin')" home-path="/admin/home" :menu-items="menuItems" />
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import AppLayout from '@/components/AppLayout.vue'

const { t } = useI18n()

const menuItems = computed(() => [
  { index: '/admin/home',   icon: 'HomeFilled',   label: t('menu.home') },
  { index: '/admin/pos',    icon: 'ShoppingCart',  label: t('menu.pos') },
  { index: '/admin/orders', icon: 'List',          label: t('menu.orderQuery') },
  {
    index: 'system', icon: 'Setting', label: t('menu.systemManage'),
    children: [
      { index: '/admin/stores', icon: 'Shop',    label: t('menu.storeManage') },
      { index: '/admin/users',  icon: 'User',    label: t('menu.employeeManage') },
      { index: '/admin/roles',  icon: 'Setting', label: t('menu.roleManage') },
      { index: '/admin/tables', icon: 'Grid',    label: t('menu.tableManage') },
      {
        index: 'dish', icon: 'KnifeFork', label: t('menu.dishManage'),
        children: [
          { index: '/admin/dishes',     label: t('menu.dishList') },
          { index: '/admin/categories', label: t('menu.categoryManage') },
        ],
      },
    ],
  },
  {
    index: 'inventory', icon: 'Box', label: t('menu.purchaseInventory'),
    children: [
      { index: '/admin/materials',  label: t('menu.materialManage') },
      { index: '/admin/purchases',  label: t('menu.purchaseManage') },
      { index: '/admin/inventory',  label: t('menu.inventoryQuery') },
    ],
  },
  {
    index: 'cost', icon: 'TrendCharts', label: t('menu.costKitchen'),
    children: [
      { index: '/admin/recipes',      label: t('menu.recipeManage') },
      { index: '/admin/consumptions', label: t('menu.consumptionManage') },
      { index: '/admin/fixed-costs',  label: t('menu.fixedCostManage') },
    ],
  },
  {
    index: 'member', icon: 'UserFilled', label: t('menu.member'),
    children: [
      { index: '/admin/members',        label: t('menu.memberList') },
      { index: '/admin/member-levels',  label: t('menu.memberLevel') },
      { index: '/admin/member-points',  label: t('menu.memberPoints') },
    ],
  },
  { index: '/admin/report', icon: 'TrendCharts', label: t('menu.report') },
  {
    index: 'logs', icon: 'Document', label: t('menu.logQuery'),
    children: [
      { index: '/admin/logs/login',      label: t('menu.loginLog') },
      { index: '/admin/logs/operation',  label: t('menu.operationLog') },
      { index: '/admin/logs/order-item', label: t('menu.orderItemLog') },
      { index: '/admin/logs/changelog',  label: t('menu.changelog') },
    ],
  },
])
</script>
