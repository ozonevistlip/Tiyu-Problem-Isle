<template>
  <div class="page">
    <PageHeader title="班级学生" subtitle="添加或移除班级学生" back>
      <template #actions>
        <el-input v-model.trim="studentKeyword" clearable placeholder="学生账号或 ID" class="student-input" />
        <el-button type="primary" @click="add">添加学生</el-button>
      </template>
    </PageHeader>
    <el-table v-loading="loading" :data="students" class="panel">
      <el-table-column prop="id" label="ID" width="100" />
      <el-table-column prop="username" label="账号" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }"><el-button text type="danger" @click="remove(row.id)">移除</el-button></template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { addStudentApi, listClassStudentsApi, removeStudentApi } from '@/api/teacherClass'
import type { UserInfo } from '@/api/types'

const route = useRoute()
const classId = computed(() => Number(route.params.classId))
const loading = ref(false)
const studentKeyword = ref('')
const students = ref<UserInfo[]>([])

async function load() {
  loading.value = true
  try {
    students.value = await listClassStudentsApi(classId.value)
  } catch {
    students.value = []
  } finally {
    loading.value = false
  }
}
async function add() {
  if (!studentKeyword.value) {
    ElMessage.warning('请输入学生账号或 ID')
    return
  }
  try {
    await addStudentApi(classId.value, studentKeyword.value)
    studentKeyword.value = ''
    ElMessage.success('已添加')
    await load()
  } catch {
    // Request interceptor already shows the server message.
  }
}
async function remove(id: number) {
  try {
    await ElMessageBox.confirm('确定移除该学生吗？', '移除确认')
    await removeStudentApi(classId.value, id)
    ElMessage.success('已移除')
    await load()
  } catch {
    // User cancelled, or the request interceptor has already shown the failure.
  }
}
onMounted(load)
</script>

<style scoped lang="scss">
.student-input {
  width: 180px;
}
</style>
