<template>
  <el-container class="layout-container">
    <el-aside width="220px">
      <div class="logo">🍽️ {{ $t('role.storeAdmin') }}</div>
      <el-menu
        router
        :default-active="route.path"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#ffd04b"
      >
        <el-menu-item index="/manager/home"
          ><el-icon><HomeFilled /></el-icon>{{ $t('menu.home') }}</el-menu-item
        >
        <el-menu-item index="/manager/pos"
          ><el-icon><ShoppingCart /></el-icon>{{ $t('menu.pos') }}</el-menu-item
        >
        <el-menu-item index="/manager/orders"
          ><el-icon><List /></el-icon>{{ $t('menu.orderQuery') }}</el-menu-item
        >
        <el-sub-menu index="system">
          <template #title
            ><el-icon><Setting /></el-icon>{{ $t('menu.systemManage') }}</template
          >
          <el-menu-item index="/manager/stores"
            ><el-icon><Shop /></el-icon>{{ $t('menu.storeInfo') }}</el-menu-item
          >
          <el-menu-item index="/manager/users"
            ><el-icon><User /></el-icon>{{ $t('menu.employeeManage') }}</el-menu-item
          >
          <el-menu-item index="/manager/tables"
            ><el-icon><Grid /></el-icon>{{ $t('menu.tableManage') }}</el-menu-item
          >
          <el-sub-menu index="dish">
            <template #title
              ><el-icon><KnifeFork /></el-icon>{{ $t('menu.dishManage') }}</template
            >
            <el-menu-item index="/manager/dishes">{{ $t('menu.dishList') }}</el-menu-item>
            <el-menu-item index="/manager/categories">{{ $t('menu.categoryManage') }}</el-menu-item>
          </el-sub-menu>
        </el-sub-menu>
        <el-sub-menu index="inventory">
          <template #title
            ><el-icon><Box /></el-icon>{{ $t('menu.purchaseInventory') }}</template
          >
          <el-menu-item index="/manager/materials">{{ $t('menu.materialManage') }}</el-menu-item>
          <el-menu-item index="/manager/purchases">{{ $t('menu.purchaseManage') }}</el-menu-item>
          <el-menu-item index="/manager/inventory">{{ $t('menu.inventoryQuery') }}</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="cost">
          <template #title
            ><el-icon><TrendCharts /></el-icon>{{ $t('menu.costKitchen') }}</template
          >
          <el-menu-item index="/manager/recipes">{{ $t('menu.recipeManage') }}</el-menu-item>
          <el-menu-item index="/manager/consumptions">{{ $t('menu.consumptionManage') }}</el-menu-item>
          <el-menu-item index="/manager/fixed-costs">{{ $t('menu.fixedCostManage') }}</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/manager/report"
          ><el-icon><TrendCharts /></el-icon>{{ $t('menu.report') }}</el-menu-item
        >
        <el-sub-menu index="logs">
          <template #title
            ><el-icon><Document /></el-icon>{{ $t('menu.logQuery') }}</template
          >
          <el-menu-item index="/manager/logs/login">{{ $t('menu.loginLog') }}</el-menu-item>
          <el-menu-item index="/manager/logs/operation">{{ $t('menu.operationLog') }}</el-menu-item>
          <el-menu-item index="/manager/logs/order-item">{{ $t('menu.orderItemLog') }}</el-menu-item>
          <el-menu-item index="/manager/logs/changelog">{{ $t('menu.changelog') }}</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header-bar">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/manager/home' }">{{ $t('role.storeAdmin') }}</el-breadcrumb-item>
          <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
        </el-breadcrumb>
        <div class="header-right">
          <LangSwitcher />
          <span>{{ user.realName || user.username }}
            <el-tag size="small" type="info">{{ $t('role.storeAdmin') }}</el-tag>
          </span>
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
import { ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
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
.layout-container {
  height: 100vh;
}
.el-aside {
  background-color: #304156;
  overflow-y: auto;
}
.logo {
  height: 60px;
  line-height: 60px;
  color: #fff;
  font-size: 20px;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  padding: 0 20px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.el-main {
  background: #f0f2f5;
}
</style>
