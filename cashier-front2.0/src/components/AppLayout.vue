<template>
  <el-container class="layout-container">
    <el-aside width="220px">
      <div class="logo">{{ logoEmoji }} {{ roleLabel }}</div>
      <el-menu
        router
        :default-active="route.path"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#ffd04b"
      >
        <template v-for="item in menuItems" :key="item.index">
          <!-- 无子菜单的普通项 -->
          <el-menu-item v-if="!item.children" :index="item.index">
            <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>{{ item.label }}
          </el-menu-item>

          <!-- 有子菜单：递归渲染 -->
          <el-sub-menu v-else :index="item.index">
            <template #title>
              <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>{{ item.label }}
            </template>
            <template v-for="child in item.children" :key="child.index">
              <!-- 子级普通项 -->
              <el-menu-item v-if="!child.children" :index="child.index">
                <el-icon v-if="child.icon"><component :is="child.icon" /></el-icon>{{ child.label }}
              </el-menu-item>
              <!-- 子级子菜单（3级嵌套，如 system > dish） -->
              <el-sub-menu v-else :index="child.index">
                <template #title>
                  <el-icon v-if="child.icon"><component :is="child.icon" /></el-icon>{{ child.label }}
                </template>
                <el-menu-item
                  v-for="grandchild in child.children"
                  :key="grandchild.index"
                  :index="grandchild.index"
                >{{ grandchild.label }}</el-menu-item>
              </el-sub-menu>
            </template>
          </el-sub-menu>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header-bar">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: homePath }">{{ roleLabel }}</el-breadcrumb-item>
          <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
        </el-breadcrumb>
        <div class="header-right">
          <LangSwitcher variant="light" />
          <span
            >{{ user.realName || user.username }}
            <el-tag size="small" type="info">{{ roleLabel }}</el-tag></span
          >
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

defineProps({
  logoEmoji: { type: String, default: '🍽️' },
  roleLabel: { type: String, required: true },
  homePath: { type: String, required: true },
  menuItems: { type: Array, required: true },
})

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
