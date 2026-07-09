<template>
  <section class="auth-card">
    <div class="card-head">
      <span class="badge">新同学登记</span>
      <h2>创建账号</h2>
      <p class="muted">选择老师或学生身份，注册后就可以回到登录页进入学习星球。</p>
    </div>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
      <el-form-item label="账号" prop="username"><el-input v-model.trim="form.username" size="large" autocomplete="username" placeholder="设置登录账号" /></el-form-item>
      <el-form-item label="密码" prop="password"><el-input v-model="form.password" size="large" type="password" autocomplete="new-password" placeholder="设置登录密码" show-password /></el-form-item>
      <el-form-item label="姓名"><el-input v-model.trim="form.realName" size="large" placeholder="填写真实姓名，老师更容易找到你" /></el-form-item>
      <el-form-item label="身份" prop="role">
        <el-segmented v-model="form.role" :options="roleOptions" />
      </el-form-item>
      <el-button type="primary" size="large" :loading="loading" native-type="submit" class="full">
        {{ loading ? '正在创建你的入口' : '创建账号' }}
      </el-button>
    </el-form>
    <p class="switch">已有账号？<router-link to="/login">返回登录</router-link></p>
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
    { required: true, message: '先给自己取一个账号吧', trigger: 'blur' },
    { min: 3, max: 30, message: '账号长度需要 3-30 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '密码还没有填写呢', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度需要 6-64 个字符', trigger: 'blur' }
  ],
  role: [{ required: true, message: '请选择你是老师还是学生', trigger: 'change' }]
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    ElMessage.warning('还有信息需要补一下哦')
    return
  }
  loading.value = true
  try {
    await user.register(form)
    ElMessage.success('注册成功，欢迎加入学习星球！')
    await router.replace('/login')
  } catch {
    ElMessage.error('注册没有成功，请检查账号是否已存在')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.auth-card {
  position: relative;
  align-self: center;
  width: min(100%, 420px);
  margin: 0 auto;
  padding: 30px;
  background: var(--surface-glass);
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-hover);
  backdrop-filter: blur(24px);
}

.card-head {
  margin-bottom: 24px;
}

.badge {
  display: inline-flex;
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

.switch {
  margin: 18px 0 0;
  color: var(--text-secondary);
  text-align: center;
}

a {
  color: var(--color-primary);
  font-weight: 700;
}
</style>
