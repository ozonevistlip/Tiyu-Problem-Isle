<template>
  <section class="page">
    <div class="panel heading"><div><h1>网站设置</h1><p>控制注册入口与关键站点策略。</p></div></div>
    <div class="settings-grid" v-loading="loading">
      <article class="panel setting-card"><div><h3>开放用户注册</h3><p>关闭后统一登录页仍可使用；老师账号由超级管理员创建，学生账号由老师创建。以后仍可重新开放自主注册。</p></div><el-switch v-model="registrationEnabled" size="large" :loading="saving" @change="saveRegistration" /></article>
      <article class="panel info-card"><h3>账号安全策略</h3><el-descriptions :column="1" border><el-descriptions-item label="鉴权方式">JWT + 服务端会话</el-descriptions-item><el-descriptions-item label="在线判定">最近 15 分钟活跃</el-descriptions-item><el-descriptions-item label="封禁行为">立即终止全部会话</el-descriptions-item><el-descriptions-item label="密码重置">立即终止全部会话</el-descriptions-item></el-descriptions></article>
      <article class="panel warning-card"><h3>初始化管理员配置</h3><p>生产环境请通过 <code>SUPER_ADMIN_USERNAME</code> 与 <code>SUPER_ADMIN_PASSWORD</code> 环境变量设置初始账号，并在首次登录后妥善保管凭据。</p></article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { settingsApi, updateRegistrationApi } from '@/api/superAdmin'

const registrationEnabled = ref(false), loading = ref(false), saving = ref(false)
async function load() { loading.value = true; try { registrationEnabled.value = (await settingsApi()).registrationEnabled } finally { loading.value = false } }
async function saveRegistration(value: string | number | boolean) { saving.value = true; try { await updateRegistrationApi(Boolean(value)); ElMessage.success(Boolean(value) ? '已开放用户注册' : '已关闭用户注册') } catch { registrationEnabled.value = !Boolean(value) } finally { saving.value = false } }
onMounted(load)
</script>

<style scoped lang="scss">
.heading h1 { margin: 0 0 6px; }.heading p, .setting-card p, .warning-card p { margin: 0; color: var(--text-muted); line-height: 1.7; }.settings-grid { display: grid; grid-template-columns: 1.2fr 1fr; gap: 18px; }.setting-card { display: flex; align-items: flex-start; justify-content: space-between; gap: 28px; }.setting-card h3, .info-card h3, .warning-card h3 { margin: 0 0 10px; }.warning-card { grid-column: 1 / -1; border-color: color-mix(in srgb, var(--color-warning), transparent 65%); }.warning-card code { color: var(--color-primary); } @media (max-width: 760px) { .settings-grid { grid-template-columns: 1fr; }.warning-card { grid-column: auto; } }
</style>
