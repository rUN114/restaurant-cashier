<template>
  <AppLayout logo-emoji="🍽️" :role-label="$t('role.waiter')" home-path="/waiter/home" :menu-items="menuItems" />
</template>
<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import AppLayout from '@/components/AppLayout.vue'
const { t } = useI18n()
const menuItems = computed(() => [
  { index: '/waiter/home', icon: 'HomeFilled', label: t('menu.home') },
  { index: '/waiter/pos', icon: 'ShoppingCart', label: t('menu.pos') },
  { index: '/waiter/orders', icon: 'List', label: t('menu.orderQuery') },
  { index: '/waiter/kds', icon: 'KnifeFork', label: t('menu.waiterKDS') },
  { index: '/waiter/queue', icon: 'List', label: t('menu.queueManage') },
  { index: '/waiter/queue-board', icon: 'Monitor', label: t('menu.queueBoard') },
  { index: '/waiter/logs/changelog', icon: 'Clock', label: t('menu.changelog') },
])
</script>
