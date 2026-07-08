<template>
  <div class="page">
    <PageHeader title="比赛排行" subtitle="查看本场比赛排行榜" back />
    <el-table v-loading="loading" :data="ranks" class="panel">
      <el-table-column type="index" label="排名" width="80" />
      <el-table-column prop="studentName" label="学生" />
      <el-table-column prop="totalScore" label="总分" width="100" />
      <el-table-column prop="acceptedCount" label="通过题数" width="120" />
      <el-table-column prop="submitCount" label="提交次数" width="120" />
      <el-table-column label="最后提交"><template #default="{ row }">{{ formatDateTime(row.lastSubmitAt) }}</template></el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import { studentRankApi } from '@/api/studentContest'
import type { RankInfo } from '@/api/types'
import { formatDateTime } from '@/utils/time'

const route = useRoute()
const contestId = computed(() => Number(route.params.contestId))
const loading = ref(false)
const ranks = ref<RankInfo[]>([])
onMounted(async () => {
  loading.value = true
  try {
    ranks.value = await studentRankApi(contestId.value)
  } finally {
    loading.value = false
  }
})
</script>
