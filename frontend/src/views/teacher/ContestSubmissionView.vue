<template>
  <div class="page">
    <PageHeader title="提交记录" subtitle="查看本场比赛所有提交及测试点结果" back />
    <div class="panel toolbar">
      <el-input v-model="filters.student" placeholder="按学生 ID 筛选" clearable />
      <el-input v-model="filters.problem" placeholder="按题目 ID 筛选" clearable />
      <el-select v-model="filters.status" placeholder="按结果筛选" clearable>
        <el-option label="通过" value="ACCEPTED" />
        <el-option label="答案错误" value="WRONG_ANSWER" />
        <el-option label="编译错误" value="COMPILE_ERROR" />
      </el-select>
    </div>
    <el-table v-loading="loading" :data="filtered" class="panel">
      <el-table-column prop="id" label="提交 ID" width="100" />
      <el-table-column prop="userId" label="学生 ID" width="100" />
      <el-table-column prop="problemId" label="题目 ID" width="100" />
      <el-table-column label="状态" width="130"><template #default="{ row }"><SubmissionStatusTag :status="row.status" /></template></el-table-column>
      <el-table-column prop="score" label="分数" width="90" />
      <el-table-column label="时间" width="180"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></el-table-column>
      <el-table-column label="操作" width="120"><template #default="{ row }"><el-button text type="primary" @click="openCases(row.id)">测试点</el-button></template></el-table-column>
    </el-table>
    <el-dialog v-model="caseVisible" title="测试点结果" width="820px">
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
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import SubmissionStatusTag from '@/components/SubmissionStatusTag.vue'
import { teacherSubmissionsApi } from '@/api/teacherContest'
import { getSubmissionCasesApi } from '@/api/submission'
import type { SubmissionCaseInfo, SubmissionInfo } from '@/api/types'
import { formatDateTime } from '@/utils/time'

const route = useRoute()
const contestId = computed(() => Number(route.params.contestId))
const loading = ref(false)
const caseLoading = ref(false)
const caseVisible = ref(false)
const submissions = ref<SubmissionInfo[]>([])
const cases = ref<SubmissionCaseInfo[]>([])
const filters = reactive({ student: '', problem: '', status: '' })
const filtered = computed(() =>
  submissions.value.filter((item) => {
    if (filters.student && String(item.userId) !== filters.student) return false
    if (filters.problem && String(item.problemId) !== filters.problem) return false
    if (filters.status && item.status !== filters.status) return false
    return true
  })
)

async function openCases(submissionId: number) {
  caseVisible.value = true
  caseLoading.value = true
  try {
    cases.value = await getSubmissionCasesApi(submissionId)
  } finally {
    caseLoading.value = false
  }
}
onMounted(async () => {
  loading.value = true
  try {
    submissions.value = await teacherSubmissionsApi(contestId.value)
  } finally {
    loading.value = false
  }
})
</script>
