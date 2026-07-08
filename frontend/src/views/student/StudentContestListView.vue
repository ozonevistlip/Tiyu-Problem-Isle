<template>
  <div class="page">
    <PageHeader title="我的比赛" subtitle="只显示你所在班级已发布的比赛" />
    <el-table v-loading="loading" :data="contests" class="panel">
      <el-table-column prop="title" label="比赛标题" min-width="180" />
      <el-table-column label="时间" width="310"><template #default="{ row }">{{ formatDateTime(row.startTime) }} - {{ formatDateTime(row.endTime) }}</template></el-table-column>
      <el-table-column label="状态" width="180"><template #default="{ row }"><ContestCountdown :start-time="row.startTime" :end-time="row.endTime" /></template></el-table-column>
      <el-table-column label="操作" width="130"><template #default="{ row }"><el-button text type="primary" @click="$router.push(`/student/contests/${row.id}`)">进入比赛</el-button></template></el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import ContestCountdown from '@/components/ContestCountdown.vue'
import { listStudentContestsApi } from '@/api/studentContest'
import type { ContestInfo } from '@/api/types'
import { formatDateTime } from '@/utils/time'

const loading = ref(false)
const contests = ref<ContestInfo[]>([])
onMounted(async () => {
  loading.value = true
  try {
    contests.value = await listStudentContestsApi()
  } finally {
    loading.value = false
  }
})
</script>
