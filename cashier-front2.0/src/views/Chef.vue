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
  // 厨师第一件事：看今日订单
  { index: '/chef/orders', icon: 'List', label: t('menu.orderQuery') },
  // 核心工作：配方管理 → 原料消耗（因果关系）
  { index: '/chef/recipes', icon: 'KnifeFork', label: t('menu.recipeManage') },
  { index: '/chef/consumptions', icon: 'TrendCharts', label: t('menu.consumptionManage') },
  { index: '/chef/kds', icon: 'KnifeFork', label: '后厨看板' },
  { index: '/chef/logs/changelog', icon: 'Clock', label: t('menu.changelog') },
])
</script>
