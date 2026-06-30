<template>
  <AppLayout logo-emoji="🍽️" :role-label="$t('role.admin')" home-path="/admin/home" :menu-items="menuItems" />
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import AppLayout from '@/components/AppLayout.vue'

const { t } = useI18n()

const menuItems = computed(() => [
  { index: 'dash', icon: 'TrendCharts', label: t('menu.dashboard'), children: [
    { index: '/admin/home', icon: 'HomeFilled', label: t('menu.home') },
    { index: '/admin/report', icon: 'TrendCharts', label: t('menu.report') },
    { index: '/admin/bigscreen', icon: 'Monitor', label: t('menu.bigscreen') },
    { index: '/admin/queue-board', icon: 'Monitor', label: t('menu.queueBoard') },
  ]},
  { index: '/admin/ai', icon: 'Notebook', label: t('menu.aiSuggest') },
  { index: 'ops', icon: 'ShoppingCart', label: t('menu.dailyOps'), children: [
    { index: '/admin/pos', icon: 'ShoppingCartFull', label: t('menu.pos') },
    { index: '/admin/orders', icon: 'List', label: t('menu.orderQuery') },
    { index: '/admin/queue', icon: 'List', label: t('menu.queueManage') },
  ]},
  { index: 'dish', icon: 'KnifeFork', label: t('menu.dishManage'), children: [
    { index: '/admin/dishes', label: t('menu.dishList') },
    { index: '/admin/categories', label: t('menu.categoryManage') },
    { index: '/admin/recipes', label: t('menu.recipeManage') },
  ]},
  { index: 'supply', icon: 'Box', label: t('menu.invSupply'), children: [
    { index: '/admin/inventory', label: t('menu.inventoryQuery') },
    { index: '/admin/purchases', label: t('menu.purchaseManage') },
    { index: '/admin/materials', label: t('menu.materialManage') },
    { index: '/admin/consumptions', label: t('menu.consumptionManage') },
    { index: '/admin/fixed-costs', label: t('menu.fixedCostManage') },
  ]},
  { index: 'mbr', icon: 'UserFilled', label: t('menu.member'), children: [
    { index: '/admin/members', label: t('menu.memberList') },
    { index: '/admin/member-levels', label: t('menu.memberLevel') },
    { index: '/admin/member-points', label: t('menu.memberPoints') },
  ]},
  { index: 'org', icon: 'OfficeBuilding', label: t('menu.storeHR'), children: [
    { index: '/admin/stores', label: t('menu.storeManage') },
    { index: '/admin/tables', label: t('menu.tableManage') },
    { index: '/admin/users', label: t('menu.employeeManage') },
    { index: '/admin/roles', label: t('menu.roleManage') },
  ]},
  { index: 'sys', icon: 'Setting', label: t('menu.sysSettings'), children: [
    { index: '/admin/sys-config', label: t('pages.sysConfigTitle') },
    { index: '/admin/logs/operation', label: t('menu.operationLog') },
    { index: '/admin/logs/login', label: t('menu.loginLog') },
    { index: '/admin/logs/order-item', label: t('menu.orderItemLog') },
    { index: '/admin/logs/changelog', label: t('menu.changelog') },
  ]},
])
</script>
