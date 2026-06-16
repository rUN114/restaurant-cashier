<template>
  <el-container class="layout-container">
    <el-aside width="220px">
      <div class="logo">💰 {{ $t('role.cashier') }}</div>
      <el-menu router :default-active="route.path" background-color="#304156" text-color="#bfcbd9" active-text-color="#ffd04b">
        <el-menu-item index="/cashier/home"><el-icon><HomeFilled /></el-icon>{{ $t('menu.home') }}</el-menu-item>
        <el-menu-item index="/cashier/pos"><el-icon><ShoppingCart /></el-icon>{{ $t('menu.pos') }}</el-menu-item>
        <el-menu-item index="/cashier/orders"><el-icon><List /></el-icon>{{ $t('menu.orderQuery') }}</el-menu-item>
        <el-menu-item index="/cashier/logs/changelog"><el-icon><Clock /></el-icon>{{ $t('menu.changelog') }}</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header-bar">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/cashier/home' }">{{ $t('role.cashier') }}</el-breadcrumb-item>
          <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
        </el-breadcrumb>
        <div class="header-right">
          <LangSwitcher variant="light" />
          <span>{{ user.realName || user.username }} <el-tag size="small" type="info">{{ $t('role.cashier') }}</el-tag></span>
          <el-button link type="danger" @click="handleLogout">{{ $t('common.logout') }}</el-button>
        </div>
      </el-header>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useI18n } from 'vue-i18n'
import { ElMessageBox } from 'element-plus'
import LangSwitcher from '@/components/LangSwitcher.vue'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const user = useUserStore()

const handleLogout = async () => {
  await ElMessageBox.confirm(t('action.deleteConfirm'), t('common.confirm'), { type: 'warning' })
  user.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout-container { height: 100vh; }
.el-aside { background-color: #304156; overflow-y: auto; }
.logo { height: 60px; line-height: 60px; color: #fff; font-size: 20px; text-align: center; border-bottom: 1px solid rgba(255,255,255,0.1); }
.header-bar { display: flex; justify-content: space-between; align-items: center; background: #fff; box-shadow: 0 1px 4px rgba(0,0,0,0.08); padding: 0 20px; }
.header-right { display: flex; align-items: center; gap: 16px; }
.el-main { background: #f0f2f5; }
</style>
