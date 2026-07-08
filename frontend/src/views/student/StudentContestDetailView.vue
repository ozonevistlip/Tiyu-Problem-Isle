<template>
  <div class="page">
    <PageHeader :title="contest?.title || '比赛详情'" :subtitle="contest?.description || '查看题目列表和当前状态'" back>
      <template #actions>
        <ContestCountdown v-if="contest" :start-time="contest.startTime" :end-time="contest.endTime" />
        <el-button @click="$router.push(`/student/contests/${contestId}/rank`)">排行榜</el-button>
        <el-button @click="$router.push(`/student/contests/${contestId}/submissions`)">我的提交</el-button>
      </template>
    </PageHeader>
    <el-table v-loading="loading" :data="problems" class="panel">
      <el-table-column prop="displayOrder" label="题号" width="80" />
      <el-table-column prop="title" label="题目" min-width="180" />
      <el-table-column prop="score" label="分数" width="90" />
      <el-table-column label="状态" width="110"><template #default="{ row }"><ProblemStatusTag :status="row.status" /></template></el-table-column>
      <el-table-column prop="submitCount" label="提交次数" width="110" />
      <el-table-column label="提示" width="160"><template #default="{ row }"><el-tag :type="row.canShowHint ? 'warning' : 'info'">{{ row.canShowHint ? '已解锁' : `剩余 ${row.hintUnlockRemainSeconds || 0}s` }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="130"><template #default="{ row }"><el-button text type="primary" @click="$router.push(`/student/contests/${contestId}/problems/${row.problemId}`)">进入答题</el-button></template></el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import ContestCountdown from '@/components/ContestCountdown.vue'
import ProblemStatusTag from '@/components/ProblemStatusTag.vue'
import { getStudentContestApi, listStudentContestProblemsApi } from '@/api/studentContest'
import type { ContestInfo, ContestProblemInfo } from '@/api/types'

const route = useRoute()
const contestId = computed(() => Number(route.params.contestId))
const loading = ref(false)
const contest = ref<ContestInfo | null>(null)
const problems = ref<ContestProblemInfo[]>([])
onMounted(async () => {
  loading.value = true
  try {
    const [contestInfo, problemList] = await Promise.all([getStudentContestApi(contestId.value), listStudentContestProblemsApi(contestId.value)])
    contest.value = contestInfo
    problems.value = problemList
  } finally {
    loading.value = false
  }
})
</script>
