<template>
  <section class="auth-card">
    <h2>注册账号</h2>
    <p class="muted">选择老师或学生身份，注册后回到登录页</p>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
      <el-form-item label="账号" prop="username"><el-input v-model="form.username" size="large" autocomplete="username" /></el-form-item>
      <el-form-item label="密码" prop="password"><el-input v-model="form.password" size="large" type="password" autocomplete="new-password" show-password /></el-form-item>
      <el-form-item label="姓名"><el-input v-model="form.realName" size="large" /></el-form-item>
      <el-form-item label="身份" prop="role">
        <el-segmented v-model="form.role" :options="roleOptions" />
      </el-form-item>
      <el-button type="primary" size="large" :loading="loading" native-type="submit" class="full">注册</el-button>
    </el-form>
    <p class="switch">已有账号？<router-link to="/login">去登录</router-link></p>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import type { UserRole } from '@/api/types'

const user = useUserStore()
const router = useRouter()
const loading = ref(false)
const formRef = ref<FormInstance>()
const roleOptions = [
  { label: '老师', value: 'teacher' },
  { label: '学生', value: 'student' }
]
const form = reactive<{ username: string; password: string; role: UserRole; realName: string }>({
  username: '',
  password: '',
  role: 'student',
  realName: ''
})
const rules: FormRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 30, message: '账号长度必须在 3 到 30 个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度必须在 6 到 64 个字符之间', trigger: 'blur' }
  ],
  role: [{ required: true, message: '请选择身份', trigger: 'change' }]
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    await user.register(form)
    ElMessage.success('注册成功，请登录')
    await router.replace('/login')
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
