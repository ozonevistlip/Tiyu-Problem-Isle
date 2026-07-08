<template>
  <div class="page">
    <PageHeader title="比赛管理" subtitle="创建班级小比赛并发布给学生">
      <template #actions><el-button type="primary" @click="$router.push('/teacher/contests/create')">创建比赛</el-button></template>
    </PageHeader>
    <el-table v-loading="loading" :data="contests" class="panel">
      <el-table-column prop="title" label="比赛标题" min-width="170" />
      <el-table-column prop="classId" label="班级 ID" width="100" />
      <el-table-column label="开始时间" width="180"><template #default="{ row }">{{ formatDateTime(row.startTime) }}</template></el-table-column>
      <el-table-column label="结束时间" width="180"><template #default="{ row }">{{ formatDateTime(row.endTime) }}</template></el-table-column>
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column label="操作" width="420">
        <template #default="{ row }">
          <el-button text type="primary" @click="$router.push(`/teacher/contests/${row.id}/edit`)">编辑</el-button>
          <el-button text @click="$router.push(`/teacher/contests/${row.id}/problems`)">选题</el-button>
          <el-button text type="success" @click="publish(row.id)">发布</el-button>
          <el-button text @click="$router.push(`/teacher/contests/${row.id}/rank`)">排行</el-button>
          <el-button text @click="$router.push(`/teacher/contests/${row.id}/submissions`)">提交</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { listTeacherContestsApi, publishContestApi } from '@/api/teacherContest'
import type { ContestInfo } from '@/api/types'
import { formatDateTime } from '@/utils/time'

const loading = ref(false)
const contests = ref<ContestInfo[]>([])
async function load() {
  loading.value = true
  try {
    contests.value = await listTeacherContestsApi()
  } finally {
    loading.value = false
  }
}
async function publish(id: number) {
  await ElMessageBox.confirm('发布后学生将可以看到比赛，确定发布吗？', '发布确认')
  await publishContestApi(id)
  ElMessage.success('已发布')
  await load()
}
onMounted(load)
</script>
