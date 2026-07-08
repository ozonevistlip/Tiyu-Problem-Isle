<template>
  <el-container class="app-layout">
    <el-aside width="240px">
      <div class="brand"><span>C++</span><strong>教师工作台</strong></div>
      <el-menu router :default-active="$route.path">
        <el-menu-item index="/teacher">首页</el-menu-item>
        <el-menu-item index="/teacher/classes">班级管理</el-menu-item>
        <el-menu-item index="/teacher/problems">题库管理</el-menu-item>
        <el-menu-item index="/teacher/contests">比赛管理</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header>
        <span>{{ user.userInfo?.realName || user.userInfo?.username }}</span>
        <el-button text @click="logout">退出</el-button>
      </el-header>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const user = useUserStore()
const router = useRouter()
function logout() {
  user.logout()
  void router.replace('/login')
}
</script>

<style scoped lang="scss">
.app-layout {
  min-height: 100vh;
}

.el-aside {
  background: #fff;
  border-right: 1px solid #d8e2ec;
}

.brand {
  display: flex;
  gap: 10px;
  align-items: center;
  height: 64px;
  padding: 0 18px;
}

.brand span {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  color: #fff;
  background: #2878d8;
  border-radius: 8px;
  font-weight: 800;
}

.el-header {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  background: #fff;
  border-bottom: 1px solid #d8e2ec;
}

.el-main {
  padding: 22px;
}
</style>
