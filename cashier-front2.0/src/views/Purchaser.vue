<template>
  <AppLayout logo-emoji="📦" :role-label="$t('role.purchaser')" home-path="/buyer/home" :menu-items="menuItems" />
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import AppLayout from '@/components/AppLayout.vue'

const { t } = useI18n()

const menuItems = [
  { index: '/buyer/home', icon: 'HomeFilled', label: t('menu.home') },
  { index: '/buyer/purchases', icon: 'Document', label: t('menu.purchaseManage') },
  { index: '/buyer/inventory', icon: 'Box', label: t('menu.inventoryQuery') },
  { index: '/buyer/materials', icon: 'Goods', label: t('menu.materialManage') },
  { index: '/buyer/logs/changelog', icon: 'Clock', label: t('menu.changelog') },
]
</script>
