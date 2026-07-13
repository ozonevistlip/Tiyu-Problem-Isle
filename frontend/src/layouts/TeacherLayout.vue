<template>
  <el-container class="app-layout">
    <el-aside width="240px">
      <div class="brand">
        <span>C++</span>
        <div>
          <strong>教师工作台</strong>
          <small>组织课堂挑战</small>
        </div>
      </div>
      <el-menu router :default-active="$route.path">
        <el-menu-item index="/teacher">首页</el-menu-item>
        <el-menu-item index="/teacher/classes">班级管理</el-menu-item>
        <el-menu-item index="/teacher/problems">题库管理</el-menu-item>
        <el-menu-item index="/teacher/contests">比赛管理</el-menu-item>
        <el-menu-item index="/teacher/code-visualizer">
          <el-icon><DataAnalysis /></el-icon>
          <span>代码可视化</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header>
        <div class="header-title">
          <strong>{{ user.userInfo?.realName || user.userInfo?.username }} 的课堂空间</strong>
          <small>清晰管理班级、题目与比赛</small>
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
import { DataAnalysis } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import ThemeSwitcher from '@/components/ThemeSwitcher.vue'

const user = useUserStore()
const router = useRouter()
function logout() {
  user.logout()
  ElMessage.success('已退出教师工作台')
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
