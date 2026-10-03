<template>
  <el-container class="app-layout">
    <el-aside :width="isCollapsed ? '72px' : '240px'" data-pet-exclusion="teacher-navigation">
      <div class="sidebar-header" :class="{ 'is-collapsed': isCollapsed }">
        <div v-if="!isCollapsed" class="brand">
          <span>C++</span>
          <div>
            <strong>教师工作台</strong>
            <small>组织课堂挑战</small>
          </div>
        </div>
        <el-button class="sidebar-toggle" text circle :title="isCollapsed ? '展开侧边栏' : '收起侧边栏'" :aria-label="isCollapsed ? '展开侧边栏' : '收起侧边栏'" @click="isCollapsed = !isCollapsed">
          <el-icon :size="20"><Expand v-if="isCollapsed" /><Fold v-else /></el-icon>
        </el-button>
      </div>
      <el-menu router :default-active="$route.path" :collapse="isCollapsed" :collapse-transition="false">
        <el-menu-item index="/teacher">
          <el-icon><HomeFilled /></el-icon>
          <template #title>首页</template>
        </el-menu-item>
        <el-menu-item index="/teacher/classes">
          <el-icon><School /></el-icon>
          <template #title>班级管理</template>
        </el-menu-item>
        <el-menu-item index="/teacher/students">
          <el-icon><UserFilled /></el-icon><template #title>学生账号</template>
        </el-menu-item>
        <el-menu-item index="/teacher/problems">
          <el-icon><Collection /></el-icon>
          <template #title>题库管理</template>
        </el-menu-item>
        <el-menu-item index="/teacher/contests">
          <el-icon><Trophy /></el-icon>
          <template #title>比赛管理</template>
        </el-menu-item>
        <el-menu-item index="/teacher/code-visualizer">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>代码可视化</template>
        </el-menu-item>
        <el-menu-item index="/teacher/pets"><el-icon><Opportunity /></el-icon><template #title>宠物伙伴</template></el-menu-item>
        <el-menu-item index="/teacher/account">
          <el-icon><UserFilled /></el-icon>
          <template #title>管理自身账号</template>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-main>
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
    <PetWidget v-if="catalog.selected" :key="catalog.selected.id" :pet-id="catalog.selected.id" :pet-name="catalog.selected.name" />
  </el-container>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { Collection, DataAnalysis, Expand, Fold, HomeFilled, Opportunity, School, Trophy, UserFilled } from '@element-plus/icons-vue'
import PetWidget from '@/components/pet/PetWidget.vue'
import { usePet } from '@/composables/usePet'
import { usePetCatalogStore } from '@/stores/petCatalog'

const isCollapsed = ref(false)
const pet = usePet()
const catalog = usePetCatalogStore()

function refreshOnFocus() { void catalog.refresh().catch(() => undefined) }
onMounted(async () => { window.addEventListener('focus', refreshOnFocus); await catalog.refresh().catch(() => undefined); if (catalog.selected) pet.welcome('老师好，今天也一起看看同学们的学习进度吧！') })
onUnmounted(() => window.removeEventListener('focus', refreshOnFocus))
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
  overflow: hidden;
  transition: width .28s cubic-bezier(.22, 1, .36, 1);
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 78px;
  padding: 0 12px 0 18px;
}

.sidebar-header.is-collapsed {
  justify-content: center;
  padding: 0;
}

.brand {
  display: flex;
  gap: 10px;
  align-items: center;
  min-width: 0;
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

.brand strong {
  display: block;
  color: var(--text-primary);
}

.brand small {
  color: var(--text-muted);
  font-size: 12px;
}

.sidebar-toggle {
  flex: 0 0 auto;
  color: var(--text-muted);
}

.sidebar-toggle:hover {
  color: var(--color-primary);
  background: color-mix(in srgb, var(--color-primary), transparent 90%);
}

.el-menu {
  border-right: 0;
}

:deep(.el-menu--collapse) {
  width: 72px;
}

:deep(.el-menu--collapse .el-menu-item) {
  justify-content: center;
  padding: 0 !important;
}

:deep(.el-menu--collapse .el-menu-item:hover) {
  transform: none;
}

:deep(.el-menu--collapse .el-menu-item .el-icon) {
  margin: 0;
  transform: translateX(-12px);
}

:deep(.el-menu-item .el-icon) {
  font-size: 18px;
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

  .sidebar-header.is-collapsed {
    min-height: 56px;
  }

  .sidebar-header.is-collapsed + .el-menu {
    display: none;
  }

  .el-main {
    padding: 16px;
  }

}
</style>
