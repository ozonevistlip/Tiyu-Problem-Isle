<template>
  <section class="page">
    <div class="panel heading"><div><h1>在线用户</h1><p>展示最近 15 分钟内活跃的服务端会话。</p></div><div><el-tag type="success" size="large">{{ sessions.length }} 个在线会话</el-tag><el-button @click="load">刷新</el-button></div></div>
    <div class="panel">
      <el-table :data="sessions" v-loading="loading">
        <el-table-column label="用户" min-width="180"><template #default="{ row }"><strong>{{ row.realName || row.username }}</strong><small class="block">@{{ row.username }}</small></template></el-table-column>
        <el-table-column label="角色" width="110"><template #default="{ row }">{{ roleName(row.role) }}</template></el-table-column>
        <el-table-column prop="ipAddress" label="IP 地址" min-width="145" />
        <el-table-column label="设备" min-width="260"><template #default="{ row }"><span class="agent">{{ row.userAgent || '-' }}</span></template></el-table-column>
        <el-table-column label="登录时间" min-width="170"><template #default="{ row }">{{ formatDateTime(row.loginAt) }}</template></el-table-column>
        <el-table-column label="最后活跃" min-width="170"><template #default="{ row }">{{ formatDateTime(row.lastActiveAt) }}</template></el-table-column>
        <el-table-column label="操作" width="120" fixed="right"><template #default="{ row }"><el-button v-if="row.role !== 'SUPER_ADMIN'" link type="danger" @click="terminate(row)">强制下线</el-button><el-tag v-else size="small">当前管理员</el-tag></template></el-table-column>
      </el-table>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onlineUsersApi, terminateSessionApi } from '@/api/superAdmin'
import type { OnlineSession, UserRole } from '@/api/types'
import { formatDateTime } from '@/utils/time'

const sessions = ref<OnlineSession[]>([]), loading = ref(false)
async function load() { loading.value = true; try { sessions.value = await onlineUsersApi() } finally { loading.value = false } }
async function terminate(row: OnlineSession) { await ElMessageBox.confirm(`强制“${row.username}”在此设备退出登录？`, '终止会话', { type: 'warning' }); await terminateSessionApi(row.id); ElMessage.success('会话已终止'); await load() }
const roleName = (role: UserRole) => role === 'SUPER_ADMIN' ? '超级管理员' : role === 'teacher' ? '老师' : '学生'
onMounted(load)
</script>

<style scoped lang="scss">
.heading { display: flex; align-items: center; justify-content: space-between; }.heading h1 { margin: 0 0 6px; }.heading p { margin: 0; color: var(--text-muted); }.heading > div:last-child { display: flex; gap: 10px; align-items: center; }.block { display: block; margin-top: 3px; color: var(--text-muted); }.agent { display: block; overflow: hidden; color: var(--text-secondary); text-overflow: ellipsis; white-space: nowrap; } @media (max-width: 700px) { .heading { align-items: flex-start; gap: 12px; flex-direction: column; } }
</style>
