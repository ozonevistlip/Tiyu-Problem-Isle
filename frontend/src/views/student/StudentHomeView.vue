<template>
  <div class="page">
    <PageHeader title="学生首页" subtitle="查看自己的班级比赛和最近练习状态" />
    <div class="metric-grid">
      <div class="metric"><span class="muted">我的比赛</span><strong>{{ contests.length }}</strong></div>
      <div class="metric"><span class="muted">进行中</span><strong>{{ contests.filter((item) => phase(item) === 'running').length }}</strong></div>
      <div class="metric"><span class="muted">即将开始</span><strong>{{ contests.filter((item) => phase(item) === 'upcoming').length }}</strong></div>
      <div class="metric"><span class="muted">已结束</span><strong>{{ contests.filter((item) => phase(item) === 'ended').length }}</strong></div>
    </div>
    <el-table v-loading="loading" :data="contests" class="panel">
      <el-table-column prop="title" label="比赛" />
      <el-table-column label="倒计时" width="220"><template #default="{ row }"><ContestCountdown :start-time="row.startTime" :end-time="row.endTime" /></template></el-table-column>
      <el-table-column label="操作" width="130"><template #default="{ row }"><el-button text type="primary" @click="$router.push(`/student/contests/${row.id}`)">进入</el-button></template></el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import ContestCountdown from '@/components/ContestCountdown.vue'
import { listStudentContestsApi } from '@/api/studentContest'
import type { ContestInfo } from '@/api/types'
import { contestPhase } from '@/utils/time'

const loading = ref(false)
const contests = ref<ContestInfo[]>([])
const phase = (item: ContestInfo) => contestPhase(item.startTime, item.endTime)
onMounted(async () => {
  loading.value = true
  try {
    contests.value = await listStudentContestsApi()
  } finally {
    loading.value = false
  }
})
</script>
