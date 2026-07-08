<template>
  <div class="page">
    <PageHeader title="题库管理" subtitle="维护题目、测试点和解题提示">
      <template #actions><el-button type="primary" @click="$router.push('/teacher/problems/create')">新建题目</el-button></template>
    </PageHeader>
    <el-table v-loading="loading" :data="problems" class="panel">
      <el-table-column prop="title" label="题目标题" min-width="180" />
      <el-table-column prop="difficulty" label="难度" width="100">
        <template #default="{ row }">{{ difficultyText(row.difficulty) }}</template>
      </el-table-column>
      <el-table-column prop="visibility" label="可见性" width="100" />
      <el-table-column prop="submitCount" label="提交" width="90" />
      <el-table-column prop="acceptedCount" label="通过" width="90" />
      <el-table-column label="操作" width="360">
        <template #default="{ row }">
          <el-button text type="primary" @click="$router.push(`/teacher/problems/${row.id}/edit`)">编辑</el-button>
          <el-button text @click="$router.push(`/teacher/problems/${row.id}/testcases`)">测试点</el-button>
          <el-button text @click="$router.push(`/teacher/problems/${row.id}/hints`)">提示</el-button>
          <el-button text type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { deleteProblemApi, listProblemsApi } from '@/api/teacherProblem'
import type { ProblemInfo } from '@/api/types'
import { difficultyText } from '@/utils/status'

const loading = ref(false)
const problems = ref<ProblemInfo[]>([])
async function load() {
  loading.value = true
  try {
    problems.value = await listProblemsApi()
  } finally {
    loading.value = false
  }
}
async function remove(id: number) {
  await ElMessageBox.confirm('确定删除这个题目吗？', '删除确认')
  await deleteProblemApi(id)
  ElMessage.success('已删除')
  await load()
}
onMounted(load)
</script>
