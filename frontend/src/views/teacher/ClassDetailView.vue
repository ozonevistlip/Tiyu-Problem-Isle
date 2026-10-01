<template>
  <div class="page">
    <PageHeader title="班级学生" subtitle="每节课分别为每名学生设置基础、成长或挑战类别" back>
      <template #actions>
        <el-button @click="$router.push('/teacher/students')">管理学生账号</el-button>
        <el-input v-model.trim="studentKeyword" clearable placeholder="学生账号或 ID" class="student-input" />
        <el-button type="primary" @click="add">添加学生</el-button>
      </template>
    </PageHeader>
    <div class="panel lesson-choice">
      <span>选择课次</span>
      <el-select v-model="selectedLessonId" placeholder="请选择课次" class="lesson-select">
        <el-option v-for="lesson in lessons" :key="lesson.id" :label="`第 ${lesson.lessonOrder} 课 · ${lesson.title}`" :value="lesson.id" />
      </el-select>
      <span v-if="!lessons.length" class="hint">此班级类型尚无课次，请联系超级管理员添加。</span>
      <span v-else class="hint">类别只对当前选中的课次生效。</span>
    </div>
    <el-table v-loading="loading" :data="students" class="panel">
      <el-table-column prop="id" label="ID" width="100" />
      <el-table-column prop="username" label="账号" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column label="本课视频及作业类别" width="210"><template #default="{ row }"><el-select :model-value="tierFor(row.id)" :disabled="!selectedLessonId" placeholder="未设置" @change="(value: Tier) => changeTier(row.id, value)"><el-option v-for="(name, key) in tierNames" :key="key" :label="name" :value="key" /></el-select></template></el-table-column>
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
import { addStudentApi, listClassStudentsApi, listClassesApi, removeStudentApi } from '@/api/teacherClass'
import type { UserInfo } from '@/api/types'
import { listLessonsApi, tierNames } from '@/api/learning'
import type { Lesson, Tier } from '@/api/learning'
import { lessonTiersApi, setLessonTierApi } from '@/api/teacherStudents'

const route = useRoute()
const classId = computed(() => Number(route.params.classId))
const loading = ref(false)
const studentKeyword = ref('')
const students = ref<UserInfo[]>([])
const lessons = ref<Lesson[]>([])
const selectedLessonId = ref<number>()
const tiers = ref<Record<string, Tier>>({})
function tierFor(studentId: number) { return selectedLessonId.value ? tiers.value[`${selectedLessonId.value}:${studentId}`] : undefined }

async function load() {
  loading.value = true
  try {
    const currentClass = (await listClassesApi()).find(item => item.id === classId.value)
    lessons.value = currentClass?.classTypeId ? await listLessonsApi(currentClass.classTypeId) : []
    selectedLessonId.value = lessons.value[0]?.id
    students.value = await listClassStudentsApi(classId.value)
    tiers.value = Object.fromEntries((await lessonTiersApi(classId.value)).map(item => [`${item.lessonId}:${item.studentId}`, item.tier]))
  } catch {
    students.value = []
  } finally {
    loading.value = false
  }
}
async function changeTier(studentId: number, tier: Tier) {
  if (!selectedLessonId.value) return
  await setLessonTierApi(classId.value, selectedLessonId.value, studentId, tier)
  tiers.value[`${selectedLessonId.value}:${studentId}`] = tier
  ElMessage.success('本课类别已更新')
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
.lesson-choice { display: flex; align-items: center; gap: 12px; padding: 16px; margin-bottom: 16px; flex-wrap: wrap; }
.lesson-select { width: min(340px, 100%); }
.hint { color: var(--text-muted); font-size: 13px; }
</style>
