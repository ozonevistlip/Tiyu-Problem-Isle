<template>
  <div class="page">
    <PageHeader title="学生首页" subtitle="查看自己的班级比赛和最近练习状态" />
    <div class="metric-grid">
      <div class="metric"><span class="muted">我的比赛</span><strong>{{ contests.length }}</strong></div>
      <div class="metric"><span class="muted">进行中</span><strong>{{ runningCount }}</strong></div>
      <div class="metric"><span class="muted">即将开始</span><strong>{{ upcomingCount }}</strong></div>
      <div class="metric"><span class="muted">已完成挑战</span><strong>{{ endedCount }}</strong></div>
    </div>
    <section class="panel progress-panel">
      <div>
        <span class="muted">学习能量</span>
        <strong>{{ learningPower }}%</strong>
      </div>
      <el-progress :percentage="learningPower" :stroke-width="14" striped striped-flow />
      <p class="muted">完成的挑战越多，能量条越亮。今天也可以从一个小目标开始。</p>
    </section>
    <el-table v-loading="loading" :data="contests" class="panel" empty-text="暂时没有比赛，等老师发布后这里会亮起来">
      <el-table-column prop="title" label="比赛" />
      <el-table-column label="倒计时" width="220"><template #default="{ row }"><ContestCountdown :start-time="row.startTime" :end-time="row.endTime" /></template></el-table-column>
      <el-table-column label="操作" width="130"><template #default="{ row }"><el-button text type="primary" @click="$router.push(`/student/contests/${row.id}`)">进入</el-button></template></el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import ContestCountdown from '@/components/ContestCountdown.vue'
import { listStudentContestsApi } from '@/api/studentContest'
import type { ContestInfo } from '@/api/types'
import { contestPhase } from '@/utils/time'

const loading = ref(false)
const contests = ref<ContestInfo[]>([])
const phase = (item: ContestInfo) => contestPhase(item.startTime, item.endTime)
const runningCount = computed(() => contests.value.filter((item) => phase(item) === 'running').length)
const upcomingCount = computed(() => contests.value.filter((item) => phase(item) === 'upcoming').length)
const endedCount = computed(() => contests.value.filter((item) => phase(item) === 'ended').length)
const learningPower = computed(() => {
  if (!contests.value.length) return 0
  return Math.round(((endedCount.value + runningCount.value * 0.6) / contests.value.length) * 100)
})
onMounted(async () => {
  loading.value = true
  try {
    contests.value = await listStudentContestsApi()
  } finally {
    loading.value = false
  }
})
</script>
