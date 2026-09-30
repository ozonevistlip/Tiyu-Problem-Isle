<template>
  <section class="page" v-loading="loading">
    <div class="hero panel">
      <div>
        <span class="eyebrow">SYSTEM OVERVIEW</span>
        <h1>系统运行总览</h1>
        <p>实时掌握用户规模、在线状态、访问量与服务器资源。</p>
      </div>
      <el-button type="primary" @click="load">刷新数据</el-button>
    </div>
    <div class="metric-grid" v-if="data">
      <article class="metric"><span>全部账号</span><strong>{{ data.totalUsers }}</strong><small>老师 {{ data.teachers }} · 学生 {{ data.students }}</small></article>
      <article class="metric"><span>当前在线</span><strong>{{ data.onlineUsers }}</strong><small>最近 15 分钟活跃会话</small></article>
      <article class="metric"><span>今日访问</span><strong>{{ data.todayVisits }}</strong><small>累计 {{ data.totalVisits }} 次页面访问</small></article>
      <article class="metric danger"><span>已禁用账号</span><strong>{{ data.disabledUsers }}</strong><small>这些账号无法继续登录</small></article>
    </div>
    <div class="server-grid" v-if="data">
      <article class="panel">
        <div class="panel-title"><div><h3>服务器状态</h3><p>JVM 运行环境与资源占用</p></div><el-tag type="success">运行中</el-tag></div>
        <div class="server-row"><span>持续运行</span><strong>{{ uptime }}</strong></div>
        <div class="server-row"><span>逻辑处理器</span><strong>{{ data.processors }} 核</strong></div>
        <div class="server-row"><span>系统平均负载</span><strong>{{ data.systemLoadAverage < 0 ? '不可用' : data.systemLoadAverage.toFixed(2) }}</strong></div>
        <div class="memory-label"><span>JVM 内存</span><span>{{ data.usedMemoryMb }} / {{ data.maxMemoryMb }} MB</span></div>
        <el-progress :percentage="memoryPercent" :stroke-width="12" />
      </article>
      <article class="panel safety">
        <div class="panel-title"><div><h3>账号安全</h3><p>关键安全能力已启用</p></div></div>
        <ul>
          <li><span>✓</span><div><strong>实时账号状态校验</strong><small>封禁后旧 Token 立即失效</small></div></li>
          <li><span>✓</span><div><strong>服务端会话控制</strong><small>支持按账号或会话强制下线</small></div></li>
          <li><span>✓</span><div><strong>后端角色鉴权</strong><small>管理接口仅允许 SUPER_ADMIN</small></div></li>
        </ul>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { dashboardApi } from '@/api/superAdmin'
import type { AdminDashboard } from '@/api/types'

const loading = ref(false)
const data = ref<AdminDashboard>()
const memoryPercent = computed(() => data.value ? Math.min(100, Math.round(data.value.usedMemoryMb / Math.max(data.value.maxMemoryMb, 1) * 100)) : 0)
const uptime = computed(() => {
  const seconds = data.value?.uptimeSeconds || 0
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor(seconds % 86400 / 3600)
  const minutes = Math.floor(seconds % 3600 / 60)
  return `${days} 天 ${hours} 小时 ${minutes} 分`
})

async function load() {
  loading.value = true
  try { data.value = await dashboardApi() } catch { ElMessage.error('无法读取系统运行数据') } finally { loading.value = false }
}
onMounted(load)
</script>

<style scoped lang="scss">
.hero { display: flex; align-items: center; justify-content: space-between; background: linear-gradient(135deg, color-mix(in srgb, var(--color-primary), transparent 88%), var(--surface-card)); }
.eyebrow { color: var(--color-primary); font-size: 12px; font-weight: 900; letter-spacing: .14em; }
h1 { margin: 8px 0 6px; font-size: 28px; } p { margin: 0; color: var(--text-muted); }
.metric small { display: block; margin-top: 10px; color: var(--text-muted); }.metric.danger strong { color: var(--color-danger); }
.server-grid { display: grid; grid-template-columns: 1.2fr 1fr; gap: 18px; }.panel-title { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 18px; }.panel-title h3 { margin: 0 0 5px; }.panel-title p { font-size: 13px; }
.server-row, .memory-label { display: flex; justify-content: space-between; padding: 11px 0; border-bottom: 1px solid var(--border-color); }.memory-label { margin-top: 4px; border: 0; }
.safety ul { display: grid; gap: 16px; margin: 0; padding: 0; list-style: none; }.safety li { display: flex; gap: 12px; align-items: center; }.safety li > span { display: grid; width: 30px; height: 30px; place-items: center; color: white; background: var(--color-success); border-radius: 50%; font-weight: 900; }.safety strong, .safety small { display: block; }.safety small { margin-top: 3px; color: var(--text-muted); }
@media (max-width: 850px) { .server-grid { grid-template-columns: 1fr; }.hero { align-items: flex-start; gap: 14px; flex-direction: column; } }
</style>
