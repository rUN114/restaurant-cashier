<template>
  <AppLayout logo-emoji="🍽️" :role-label="$t('role.admin')" home-path="/admin/home" :menu-items="menuItems" />
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import AppLayout from '@/components/AppLayout.vue'

const { t } = useI18n()

const menuItems = computed(() => [
  // 第一梯队：日常最高频操作
  { index: '/admin/home',   icon: 'HomeFilled',   label: t('menu.home') },
  { index: '/admin/pos',    icon: 'ShoppingCart',  label: t('menu.pos') },
  { index: '/admin/orders', icon: 'List',          label: t('menu.orderQuery') },
  // 会员管理紧跟收银线（收银→查会员→结账 是同一趟任务流）
  {
    index: 'member', icon: 'UserFilled', label: t('menu.member'),
    children: [
      { index: '/admin/members',        label: t('menu.memberList') },
      { index: '/admin/member-levels',  label: t('menu.memberLevel') },
      { index: '/admin/member-points',  label: t('menu.memberPoints') },
    ],
  },
  // 营业统计：管理层日常核心，上移
  { index: '/admin/report', icon: 'TrendCharts', label: t('menu.report') },

  // 第二梯队：组织架构管理
  {
    index: 'org', icon: 'OfficeBuilding', label: t('menu.storesAndStaff'),
    children: [
      { index: '/admin/stores', icon: 'Shop',    label: t('menu.storeManage') },
      { index: '/admin/users',  icon: 'User',    label: t('menu.employeeManage') },
      { index: '/admin/roles',  icon: 'Setting', label: t('menu.roleManage') },
      { index: '/admin/tables', icon: 'Grid',    label: t('menu.tableManage') },
    ],
  },
  // 菜品管理独立出来（高频日常维护，不再埋在第3级）
  {
    index: 'dish', icon: 'KnifeFork', label: t('menu.dishManage'),
    children: [
      { index: '/admin/dishes',     label: t('menu.dishList') },
      { index: '/admin/categories', label: t('menu.categoryManage') },
    ],
  },

  // 第三梯队：供应链（合并采购库存 + 成本后厨，按业务流排列）
  {
    index: 'supply', icon: 'Box', label: t('menu.supplyChain'),
    children: [
      { index: '/admin/materials',     label: t('menu.materialManage') },
      { index: '/admin/purchases',     label: t('menu.purchaseManage') },
      { index: '/admin/inventory',     label: t('menu.inventoryQuery') },
      { index: '/admin/recipes',       label: t('menu.recipeManage') },
      { index: '/admin/consumptions',  label: t('menu.consumptionManage') },
      { index: '/admin/fixed-costs',   label: t('menu.fixedCostManage') },
    ],
  },

  // 第四梯队：系统日志（按使用频率重排：操作→登录→菜品→更新）
  {
    index: 'logs', icon: 'Document', label: t('menu.systemLogs'),
    children: [
      { index: '/admin/logs/operation',  label: t('menu.operationLog') },
      { index: '/admin/logs/login',      label: t('menu.loginLog') },
      { index: '/admin/logs/order-item', label: t('menu.orderItemLog') },
      { index: '/admin/logs/changelog',  label: t('menu.changelog') },
    ],
  },
])
</script>
