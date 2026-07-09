<template>
  <el-container class="app-layout">
    <el-aside width="230px">
      <div class="brand">
        <span>C++</span>
        <div>
          <strong>学生课堂</strong>
          <small>继续闯关成长</small>
        </div>
      </div>
      <el-menu router :default-active="$route.path">
        <el-menu-item index="/student">首页</el-menu-item>
        <el-menu-item index="/student/contests">我的比赛</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header>
        <div class="header-title">
          <strong>欢迎回来，{{ user.userInfo?.realName || user.userInfo?.username }}</strong>
          <small>今天也向下一关出发吧</small>
        </div>
        <div class="header-actions">
          <ThemeSwitcher />
          <el-button text @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main>
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import ThemeSwitcher from '@/components/ThemeSwitcher.vue'

const user = useUserStore()
const router = useRouter()
function logout() {
  user.logout()
  ElMessage.success('已退出，期待下次继续学习！')
  void router.replace('/login')
}
</script>

<style scoped lang="scss">
.app-layout {
  min-height: 100vh;
  background: var(--bg-gradient);
}

.el-aside {
  position: relative;
  z-index: 2;
  background: var(--surface-nav);
  border-right: 1px solid var(--border-color);
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(18px);
}

.brand {
  display: flex;
  gap: 10px;
  align-items: center;
  min-height: 78px;
  padding: 0 18px;
}

.brand span {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  color: var(--text-inverse);
  background: linear-gradient(135deg, var(--color-primary), var(--color-accent));
  border-radius: 14px;
  box-shadow: 0 12px 24px color-mix(in srgb, var(--color-primary), transparent 72%);
  font-weight: 800;
}

.brand strong,
.header-title strong {
  display: block;
  color: var(--text-primary);
}

.brand small,
.header-title small {
  color: var(--text-muted);
  font-size: 12px;
}

.el-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 74px;
  gap: 16px;
  background: var(--surface-glass);
  border-bottom: 1px solid var(--border-color);
  backdrop-filter: blur(18px);
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.el-main {
  padding: 26px;
}

@media (max-width: 760px) {
  .app-layout {
    display: block;
  }

  .el-aside {
    width: 100% !important;
  }

  .el-header {
    height: auto;
    padding: 14px;
    align-items: flex-start;
    flex-direction: column;
  }

  .header-actions {
    width: 100%;
    justify-content: space-between;
  }

  .el-main {
    padding: 16px;
  }
}
</style>
