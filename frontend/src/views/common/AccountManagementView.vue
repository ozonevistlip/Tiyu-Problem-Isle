<template>
  <div class="page account-page" data-pet-exclusion="account-management">
    <PageHeader title="管理自身账号" subtitle="查看当前账号，调整页面主题或退出网站" />

    <section class="panel profile-card">
      <div class="avatar">{{ avatarText }}</div>
      <div class="profile-copy">
        <span>当前账号</span>
        <strong>{{ displayName }}</strong>
        <small>@{{ user.userInfo?.username }} · {{ roleLabel }}</small>
      </div>
      <el-tag effect="plain" type="success">正常使用中</el-tag>
    </section>

    <div class="setting-grid">
      <section class="panel setting-card">
        <div>
          <span>外观设置</span>
          <h2>页面主题</h2>
          <p>选择更适合自己的色彩风格，设置会保留在当前浏览器中。</p>
        </div>
        <ThemeSwitcher />
      </section>

      <section class="panel setting-card logout-card">
        <div>
          <span>会话管理</span>
          <h2>退出网站</h2>
          <p>退出后需要重新输入账号和密码才能继续使用。</p>
        </div>
        <el-button type="danger" plain @click="logout">退出当前账号</el-button>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import ThemeSwitcher from '@/components/ThemeSwitcher.vue'
import { useUserStore } from '@/stores/user'

const user = useUserStore()
const router = useRouter()
const displayName = computed(() => user.userInfo?.realName || user.userInfo?.nickname || user.userInfo?.username || '未命名用户')
const avatarText = computed(() => displayName.value.trim().slice(0, 1).toUpperCase())
const roleLabel = computed(() => user.role === 'teacher' ? '教师账号' : '学生账号')

async function logout() {
  await user.logout()
  ElMessage.success('已安全退出网站')
  await router.replace('/login')
}
</script>

<style scoped lang="scss">
.account-page { max-width: 980px; margin: 0 auto; }
.profile-card { display: flex; gap: 16px; align-items: center; padding: 22px; }
.avatar { display: grid; width: 58px; height: 58px; flex: 0 0 auto; place-items: center; color: var(--text-inverse); background: linear-gradient(135deg, var(--color-primary), var(--color-accent)); border-radius: 18px; font-size: 24px; font-weight: 900; box-shadow: 0 12px 24px color-mix(in srgb, var(--color-primary), transparent 72%); }
.profile-copy { display: grid; flex: 1; gap: 4px; min-width: 0; }.profile-copy span, .setting-card span { color: var(--color-primary); font-size: 11px; font-weight: 900; letter-spacing: .08em; }.profile-copy strong { overflow: hidden; color: var(--text-primary); font-size: 21px; text-overflow: ellipsis; white-space: nowrap; }.profile-copy small { color: var(--text-muted); font-size: 12px; }
.setting-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }.setting-card { display: flex; min-height: 190px; flex-direction: column; gap: 22px; align-items: flex-start; justify-content: space-between; padding: 22px; }.setting-card h2 { margin: 5px 0 8px; color: var(--text-primary); font-size: 19px; }.setting-card p { margin: 0; color: var(--text-muted); line-height: 1.7; }.logout-card { border-color: color-mix(in srgb, var(--color-danger), transparent 72%); }.logout-card span { color: var(--color-danger); }
@media (max-width: 700px) { .setting-grid { grid-template-columns: 1fr; }.profile-card { align-items: flex-start; flex-wrap: wrap; }.profile-copy { min-width: 180px; } }
</style>
