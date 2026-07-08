<template>
  <div class="page">
    <PageHeader title="我的提交" subtitle="查看本场比赛自己的提交记录" back />
    <el-table v-loading="loading" :data="submissions" class="panel">
      <el-table-column prop="id" label="提交 ID" width="100" />
      <el-table-column prop="problemId" label="题目 ID" width="100" />
      <el-table-column label="状态" width="140"><template #default="{ row }"><SubmissionStatusTag :status="row.status" /></template></el-table-column>
      <el-table-column prop="score" label="分数" width="90" />
      <el-table-column prop="timeUsedMs" label="耗时 ms" width="110" />
      <el-table-column label="提交时间"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></el-table-column>
      <el-table-column label="操作" width="120"><template #default="{ row }"><el-button text type="primary" @click="openCases(row.id)">测试点</el-button></template></el-table-column>
    </el-table>
    <el-dialog v-model="visible" title="测试点结果" width="820px">
      <el-table v-loading="caseLoading" :data="cases">
        <el-table-column prop="testcaseId" label="测试点" width="90" />
        <el-table-column label="状态" width="130"><template #default="{ row }"><SubmissionStatusTag :status="row.status" /></template></el-table-column>
        <el-table-column prop="timeUsedMs" label="耗时 ms" width="100" />
        <el-table-column prop="expectedOutputPreview" label="期望输出" />
        <el-table-column prop="actualOutputPreview" label="实际输出" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import SubmissionStatusTag from '@/components/SubmissionStatusTag.vue'
import { studentSubmissionsApi } from '@/api/studentContest'
import { getSubmissionCasesApi } from '@/api/submission'
import type { SubmissionCaseInfo, SubmissionInfo } from '@/api/types'
import { formatDateTime } from '@/utils/time'

const route = useRoute()
const contestId = computed(() => Number(route.params.contestId))
const loading = ref(false)
const caseLoading = ref(false)
const visible = ref(false)
const submissions = ref<SubmissionInfo[]>([])
const cases = ref<SubmissionCaseInfo[]>([])

async function openCases(id: number) {
  visible.value = true
  caseLoading.value = true
  try {
    cases.value = await getSubmissionCasesApi(id)
  } finally {
    caseLoading.value = false
  }
}
onMounted(async () => {
  loading.value = true
  try {
    submissions.value = await studentSubmissionsApi(contestId.value)
  } finally {
    loading.value = false
  }
})
</script>
