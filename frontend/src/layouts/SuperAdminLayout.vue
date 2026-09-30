<template>
  <el-container class="admin-layout">
    <el-aside width="250px">
      <div class="brand">
        <span>SA</span>
        <div>
          <strong>超级管理中心</strong>
          <small>CppKid 系统控制台</small>
        </div>
      </div>
      <el-menu router :default-active="$route.path">
        <el-menu-item index="/super-admin"><el-icon><Odometer /></el-icon><span>运行总览</span></el-menu-item>
        <el-menu-item index="/super-admin/users"><el-icon><UserFilled /></el-icon><span>用户管理</span></el-menu-item>
        <el-menu-item index="/super-admin/online"><el-icon><Connection /></el-icon><span>在线用户</span></el-menu-item>
        <el-menu-item index="/super-admin/announcements"><el-icon><BellFilled /></el-icon><span>全站公告</span></el-menu-item>
        <el-menu-item index="/super-admin/settings"><el-icon><Setting /></el-icon><span>网站设置</span></el-menu-item>
      </el-menu>
      <div class="security-note">
        <strong>权限已保护</strong>
        <span>所有操作均由后端再次验证 SUPER_ADMIN 权限</span>
      </div>
    </el-aside>
    <el-container>
      <el-header>
        <div>
          <strong>{{ title }}</strong>
          <small>集中管理账号、安全与站点运行状态</small>
        </div>
        <div class="header-actions">
          <el-tag type="danger" effect="dark">SUPER ADMIN</el-tag>
          <ThemeSwitcher />
          <el-dropdown>
            <el-button text>{{ user.userInfo?.realName || user.userInfo?.username }}</el-button>
            <template #dropdown>
              <el-dropdown-menu><el-dropdown-item @click="logout">安全退出</el-dropdown-item></el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main>
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in"><component :is="Component" /></transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BellFilled, Connection, Odometer, Setting, UserFilled } from '@element-plus/icons-vue'
import ThemeSwitcher from '@/components/ThemeSwitcher.vue'
import { useUserStore } from '@/stores/user'

const user = useUserStore()
const route = useRoute()
const router = useRouter()
const title = computed(() => ({
  '/super-admin': '运行总览', '/super-admin/users': '用户管理', '/super-admin/online': '在线用户',
  '/super-admin/announcements': '全站公告', '/super-admin/settings': '网站设置'
}[route.path] || '超级管理中心'))

async function logout() {
  await user.logout()
  await router.replace('/login')
}
</script>

<style scoped lang="scss">
.admin-layout { min-height: 100vh; background: var(--bg-gradient); }
.el-aside { position: relative; display: flex; flex-direction: column; background: var(--surface-nav); border-right: 1px solid var(--border-color); box-shadow: var(--shadow-soft); }
.brand { display: flex; gap: 12px; align-items: center; min-height: 82px; padding: 0 20px; }
.brand > span { display: grid; width: 44px; height: 44px; place-items: center; color: #fff; background: linear-gradient(135deg, #7f1d1d, #ef4444); border-radius: 14px; font-weight: 900; box-shadow: 0 12px 25px rgba(185, 28, 28, .25); }
.brand strong, .el-header strong { display: block; color: var(--text-primary); }
.brand small, .el-header small { color: var(--text-muted); font-size: 12px; }
.security-note { display: grid; gap: 6px; margin: auto 16px 18px; padding: 14px; color: var(--text-secondary); background: color-mix(in srgb, var(--color-danger), transparent 94%); border: 1px solid color-mix(in srgb, var(--color-danger), transparent 78%); border-radius: 14px; font-size: 12px; line-height: 1.5; }
.security-note strong { color: var(--color-danger); }
.el-header { display: flex; align-items: center; justify-content: space-between; height: 74px; padding: 0 24px; background: var(--surface-glass); border-bottom: 1px solid var(--border-color); backdrop-filter: blur(18px); }
.header-actions { display: flex; gap: 12px; align-items: center; }
.el-main { padding: 24px; }
@media (max-width: 760px) { .admin-layout { display: block; } .el-aside { width: 100% !important; } .security-note { display: none; } .el-header { height: auto; padding: 14px; gap: 12px; flex-wrap: wrap; } .el-main { padding: 14px; } }
</style>
