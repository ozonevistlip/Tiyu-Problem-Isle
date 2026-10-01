<template>
  <section class="auth-card">
    <div class="card-glow" />
    <div class="card-head">
      <span class="badge">学习星球入口</span>
      <h2>欢迎回来</h2>
      <p class="muted">登录后继续你的 C++ 闯关、比赛和课堂练习。</p>
    </div>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
      <el-form-item label="账号" prop="username">
        <el-input v-model.trim="form.username" size="large" autocomplete="username" :prefix-icon="User" placeholder="请输入账号" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          size="large"
          type="password"
          autocomplete="current-password"
          :prefix-icon="Lock"
          placeholder="请输入密码"
          show-password
        />
      </el-form-item>
      <el-button type="primary" size="large" :loading="loading" native-type="submit" class="full">
        {{ loading ? '正在进入你的学习星球' : '进入学习星球' }}
      </el-button>
    </el-form>
    <p class="helper">忘记密码可以联系老师重置，别担心，进度还在。</p>
    <p v-if="registrationEnabled" class="switch">还没有账号？<router-link to="/register">创建一个新账号</router-link></p>
    <p v-else class="switch muted">当前未开放自主注册：老师请联系超级管理员，学生请联系老师。</p>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { publicConfigApi } from '@/api/public'

const user = useUserStore()
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const registrationEnabled = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({ username: '', password: '' })
const rules: FormRules = {
  username: [
    { required: true, message: '再检查一下账号哦', trigger: 'blur' },
    { min: 3, max: 30, message: '账号一般是 3-30 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '密码还没有填写呢', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度需要 6-64 个字符', trigger: 'blur' }
  ]
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    ElMessage.warning('再检查一下账号和密码哦')
    return
  }
  loading.value = true
  try {
    const info = await user.login(form)
    ElMessage.success('太棒了，登录成功！')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    const roleHome = info.role === 'SUPER_ADMIN' ? '/super-admin' : (info.role === 'teacher' ? '/teacher' : '/student')
    await router.replace(redirect || roleHome)
  } catch {
    ElMessage.error('登录没有成功，再检查一下账号或密码吧')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try { registrationEnabled.value = (await publicConfigApi()).registrationEnabled } catch { /* keep login usable */ }
})
</script>

<style scoped lang="scss">
.auth-card {
  position: relative;
  isolation: isolate;
  align-self: center;
  width: min(100%, 420px);
  margin: 0 auto;
  padding: 30px;
  overflow: hidden;
  background: var(--surface-glass);
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-hover);
  backdrop-filter: blur(24px);
  animation: auth-card-in 0.45s var(--ease-out);
}

.card-glow {
  position: absolute;
  inset: -90px -80px auto auto;
  z-index: -1;
  width: 190px;
  height: 190px;
  background: radial-gradient(circle, color-mix(in srgb, var(--color-primary), transparent 62%), transparent 70%);
  border-radius: 999px;
}

.card-head {
  margin-bottom: 24px;
}

.badge {
  display: inline-flex;
  align-items: center;
  padding: 7px 11px;
  color: var(--color-primary);
  background: var(--color-primary-soft);
  border: 1px solid var(--border-soft);
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
}

h2 {
  margin: 14px 0 8px;
  color: var(--text-primary);
  font-size: 32px;
  line-height: 1.1;
}

.full {
  width: 100%;
  height: 48px;
  margin-top: 6px;
  font-size: 16px;
}

.helper {
  margin: 14px 0 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.switch {
  margin: 18px 0 0;
  color: var(--text-secondary);
  text-align: center;
}

a {
  color: var(--color-primary);
  font-weight: 700;
}

@keyframes auth-card-in {
  from {
    opacity: 0;
    transform: translateY(14px) scale(0.98);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
</style>
