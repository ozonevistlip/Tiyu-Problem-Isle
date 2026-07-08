<template>
  <div class="page">
    <PageHeader title="教师首页" subtitle="管理班级、题目和课堂小比赛" />
    <div class="metric-grid">
      <div class="metric"><span class="muted">我的班级</span><strong>{{ classes.length }}</strong></div>
      <div class="metric"><span class="muted">我的题目</span><strong>{{ problems.length }}</strong></div>
      <div class="metric"><span class="muted">比赛数量</span><strong>{{ contests.length }}</strong></div>
      <div class="metric"><span class="muted">已发布</span><strong>{{ contests.filter((item) => item.status === 'PUBLISHED').length }}</strong></div>
    </div>
    <div class="panel toolbar">
      <el-button type="primary" @click="$router.push('/teacher/classes')">创建班级</el-button>
      <el-button type="warning" @click="$router.push('/teacher/problems/create')">创建题目</el-button>
      <el-button @click="$router.push('/teacher/contests/create')">创建比赛</el-button>
    </div>
    <el-table v-loading="loading" :data="contests" class="panel">
      <el-table-column prop="title" label="最近比赛" />
      <el-table-column prop="status" label="状态" width="130" />
      <el-table-column label="开始时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button text type="primary" @click="$router.push(`/teacher/contests/${row.id}/rank`)">排行</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { listClassesApi } from '@/api/teacherClass'
import { listProblemsApi } from '@/api/teacherProblem'
import { listTeacherContestsApi } from '@/api/teacherContest'
import type { ClassInfo, ContestInfo, ProblemInfo } from '@/api/types'
import { formatDateTime } from '@/utils/time'

const loading = ref(false)
const classes = ref<ClassInfo[]>([])
const problems = ref<ProblemInfo[]>([])
const contests = ref<ContestInfo[]>([])

onMounted(async () => {
  loading.value = true
  try {
    const [classList, problemList, contestList] = await Promise.all([listClassesApi(), listProblemsApi(), listTeacherContestsApi()])
    classes.value = classList
    problems.value = problemList
    contests.value = contestList
  } finally {
    loading.value = false
  }
})
</script>
