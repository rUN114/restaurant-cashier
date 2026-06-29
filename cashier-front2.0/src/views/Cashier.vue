<template>
  <AppLayout logo-emoji="💰" :role-label="$t('role.cashier')" home-path="/cashier/home" :menu-items="menuItems" />
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import AppLayout from '@/components/AppLayout.vue'

const { t } = useI18n()

const menuItems = computed(() => [
  { index: '/cashier/home',      icon: 'HomeFilled',      label: t('menu.home') },
  { index: '/cashier/pos',       icon: 'ShoppingCartFull', label: t('menu.pos') },
  { index: '/cashier/orders',    icon: 'List',             label: t('menu.orderQuery') },
  { index: '/cashier/bigscreen', icon: 'Monitor',          label: '实时大屏' },
  { index: '/cashier/logs/changelog', icon: 'Clock',       label: t('menu.changelog') },
])
</script>
