<template>
  <div class="page">
    <PageHeader title="比赛选题" subtitle="为每道题设置分数和提示解锁时间" back />
    <el-table v-loading="loading" :data="problems" class="panel">
      <el-table-column prop="title" label="题目" min-width="180" />
      <el-table-column prop="difficulty" label="难度" width="100" />
      <el-table-column label="顺序" width="120"><template #default="{ row }"><el-input-number v-model="settings[row.id].displayOrder" :min="0" size="small" /></template></el-table-column>
      <el-table-column label="分数" width="120"><template #default="{ row }"><el-input-number v-model="settings[row.id].score" :min="0" size="small" /></template></el-table-column>
      <el-table-column label="提示解锁分钟" width="160"><template #default="{ row }"><el-input-number v-model="settings[row.id].hintUnlockMinutes" :min="0" size="small" /></template></el-table-column>
      <el-table-column label="AC 后提示" width="120"><template #default="{ row }"><el-switch v-model="settings[row.id].showHintAfterAc" /></template></el-table-column>
      <el-table-column label="赛后提示" width="120"><template #default="{ row }"><el-switch v-model="settings[row.id].showHintAfterContest" /></template></el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button text type="primary" @click="add(row.id)">加入比赛</el-button>
          <el-button text type="danger" @click="remove(row.id)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { listProblemsApi } from '@/api/teacherProblem'
import { addContestProblemApi, deleteContestProblemApi, type AddContestProblemPayload } from '@/api/teacherContest'
import type { ProblemInfo } from '@/api/types'

const route = useRoute()
const contestId = computed(() => Number(route.params.contestId))
const loading = ref(false)
const problems = ref<ProblemInfo[]>([])
const settings = reactive<Record<number, AddContestProblemPayload>>({})

async function load() {
  loading.value = true
  try {
    problems.value = await listProblemsApi()
    problems.value.forEach((item, index) => {
      settings[item.id] = settings[item.id] || {
        problemId: item.id,
        displayOrder: index + 1,
        score: 100,
        hintUnlockMinutes: 10,
        showHintAfterAc: true,
        showHintAfterContest: true
      }
    })
  } finally {
    loading.value = false
  }
}
async function add(problemId: number) {
  await addContestProblemApi(contestId.value, { ...settings[problemId], problemId })
  ElMessage.success('已加入比赛')
}
async function remove(problemId: number) {
  await deleteContestProblemApi(contestId.value, problemId)
  ElMessage.success('已移除')
}
onMounted(load)
</script>
