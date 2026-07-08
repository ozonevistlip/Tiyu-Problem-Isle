<template>
  <section class="auth-card">
    <h2>登录</h2>
    <p class="muted">进入你的课堂 OJ 工作区</p>
    <el-form :model="form" label-position="top" @submit.prevent="submit">
      <el-form-item label="账号">
        <el-input v-model="form.username" size="large" autocomplete="username" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" size="large" type="password" autocomplete="current-password" show-password />
      </el-form-item>
      <el-button type="primary" size="large" :loading="loading" native-type="submit" class="full">登录</el-button>
    </el-form>
    <p class="switch">还没有账号？<router-link to="/register">去注册</router-link></p>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const user = useUserStore()
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

async function submit() {
  loading.value = true
  try {
    const info = await user.login(form)
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    await router.replace(redirect || (info.role === 'teacher' ? '/teacher' : '/student'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.auth-card {
  align-self: center;
  margin: 0 32px;
  padding: 28px;
  background: #fff;
  border: 1px solid #d8e2ec;
  border-radius: 8px;
}

h2 {
  margin: 0 0 6px;
}

.full {
  width: 100%;
}

.switch {
  margin: 18px 0 0;
  text-align: center;
}

a {
  color: #2878d8;
  font-weight: 700;
}
</style>
