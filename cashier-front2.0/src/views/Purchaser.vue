<template>
  <AppLayout logo-emoji="📦" :role-label="$t('role.purchaser')" home-path="/buyer/home" :menu-items="menuItems" />
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import AppLayout from '@/components/AppLayout.vue'

const { t } = useI18n()

const menuItems = computed(() => [
  { index: '/buyer/home', icon: 'HomeFilled', label: t('menu.home') },
  // 采购工作流：先管原料档案 → 再执行采购入库 → 最后核库存
  { index: '/buyer/materials', icon: 'Goods', label: t('menu.materialManage') },
  { index: '/buyer/purchases', icon: 'Document', label: t('menu.purchaseManage') },
  { index: '/buyer/inventory', icon: 'Box', label: t('menu.inventoryQuery') },
  { index: '/buyer/logs/changelog', icon: 'Clock', label: t('menu.changelog') },
])
</script>
