<template>
  <AppLayout logo-emoji="💰" :role-label="$t('role.cashier')" home-path="/cashier/home" :menu-items="menuItems" />
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import AppLayout from '@/components/AppLayout.vue'

const { t } = useI18n()

const menuItems = [
  { index: '/cashier/home', icon: 'HomeFilled', label: t('menu.home') },
  { index: '/cashier/pos', icon: 'ShoppingCart', label: t('menu.pos') },
  { index: '/cashier/orders', icon: 'List', label: t('menu.orderQuery') },
  { index: '/cashier/logs/changelog', icon: 'Clock', label: t('menu.changelog') },
]
</script>
