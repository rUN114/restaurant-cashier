<template>
  <el-container class="layout-container">
    <el-aside :class="['layout-sidebar', { open: sidebarOpen }]">
      <div class="logo">{{ logoEmoji }} {{ roleLabel }}</div>
      <el-menu
        router
        :default-active="route.path"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#66b1ff"
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

      <!-- Swagger 文档（独立于菜单路由，仅管理员可见） -->
      <div v-if="user.roleCode === 'ADMIN'" class="swagger-link" @click="openSwagger">
        <el-icon><Document /></el-icon> API 文档
      </div>
    </el-aside>
    <el-container>
      <el-header class="header-bar">
        <el-button link class="hamburger" @click="sidebarOpen = !sidebarOpen">☰</el-button>
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: homePath }">{{ roleLabel }}</el-breadcrumb-item>
          <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
        </el-breadcrumb>
        <div class="header-right">
          <NotificationBell />
          <el-button link class="theme-btn" @click="toggleTheme">{{ isDark ? '☀️' : '🌙' }}</el-button>
          <LangSwitcher variant="light" />
          <span
            >{{ user.realName || user.username }}
            <el-tag size="small" type="info">{{ roleLabel }}</el-tag></span
          >
          <el-button link type="danger" @click="handleLogout">{{ $t('common.logout') }}</el-button>
        </div>
      </el-header>
      <el-main><router-view /></el-main>

      <!-- 手机端遮罩层 -->
      <div v-if="sidebarOpen" class="sidebar-overlay" @click="sidebarOpen = false"></div>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useI18n } from 'vue-i18n'
import { ElMessageBox } from 'element-plus'
import LangSwitcher from '@/components/LangSwitcher.vue'
import NotificationBell from '@/components/NotificationBell.vue'
import { useTheme } from '@/composables/useTheme'
const { isDark, toggle: toggleTheme } = useTheme()

defineProps({
  logoEmoji: { type: String, default: '🍽️' },
  roleLabel: { type: String, required: true },
  homePath: { type: String, required: true },
  menuItems: { type: Array, required: true },
})

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const sidebarOpen = ref(false)

// 监听路由变化关闭侧栏
watch(() => route.path, () => { sidebarOpen.value = false })

const openSwagger = () => {
  window.open('http://localhost:8088/swagger-ui.html', '_blank')
}
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
.layout-sidebar {
  display: flex;
  flex-direction: column;
  background-color: #304156;
  overflow-y: auto;
  width: 220px !important;
  transition: transform 0.3s;
  z-index: 100;
}
.layout-sidebar .el-menu {
  flex: 1;
  overflow-y: auto;
}
.logo {
  height: 60px;
  line-height: 60px;
  color: #fff;
  font-size: 20px;
  font-weight: 600;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}
.hamburger {
  display: none;
  font-size: 24px;
  padding: 4px 8px;
  margin-right: 12px;
}
.header-bar {
  display: flex;
  align-items: center;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  padding: 0 16px;
  min-height: 50px;
}
.header-bar .el-breadcrumb {
  flex: 0 0 auto;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}
.el-main {
  background: #f0f2f5;
}
.swagger-link {
  padding: 14px 20px;
  color: #bfcbd9;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  border-top: 1px solid rgba(255,255,255,0.1);
  transition: color 0.2s;
  flex-shrink: 0;
}
.swagger-link:hover {
  color: #66b1ff;
}
.sidebar-overlay {
  display: none;
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  z-index: 99;
}

/* 手机端 */
@media (max-width: 768px) {
  .hamburger {
    display: inline-flex;
  }
  .layout-sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    transform: translateX(-100%);
    width: 220px !important;
    z-index: 101;
  }
  .layout-sidebar.open {
    transform: translateX(0);
  }
  .sidebar-overlay {
    display: block;
  }
  .header-bar {
    padding: 0 12px;
  }
  .header-bar .el-breadcrumb {
    display: none;
  }
  .header-right {
    gap: 4px;
    font-size: 12px;
  }
  .header-right .el-tag {
    display: none;
  }
}
</style>
