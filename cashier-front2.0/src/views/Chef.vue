<template>
  <AppLayout logo-emoji="👨‍🍳" :role-label="$t('role.chef')" home-path="/chef/home" :menu-items="menuItems" />
</template>
<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import AppLayout from '@/components/AppLayout.vue'
const { t } = useI18n()
const menuItems = computed(() => [
  { index: '/chef/home', icon: 'HomeFilled', label: t('menu.home') },
  { index: '/chef/kds', icon: 'KnifeFork', label: t('menu.kitchenKDS') },
  { index: '/chef/ai', icon: 'Notebook', label: t('menu.aiSuggest') },
  { index: '/chef/orders', icon: 'List', label: t('menu.orderQuery') },
  { index: '/chef/recipes', icon: 'KnifeFork', label: t('menu.recipeManage') },
  { index: '/chef/consumptions', icon: 'TrendCharts', label: t('menu.consumptionManage') },
  { index: '/chef/logs/changelog', icon: 'Clock', label: t('menu.changelog') },
])
</script>
