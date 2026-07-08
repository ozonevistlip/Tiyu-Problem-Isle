<template>
  <div class="page">
    <PageHeader :title="isEdit ? '编辑题目' : '新建题目'" subtitle="题面信息会展示给学生" back />
    <div class="panel form-card">
      <el-form :model="form" label-position="top">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="题目描述"><el-input v-model="form.description" type="textarea" :rows="5" /></el-form-item>
        <el-form-item label="输入格式"><el-input v-model="form.inputFormat" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="输出格式"><el-input v-model="form.outputFormat" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="样例输入"><el-input v-model="form.sampleInput" type="textarea" /></el-form-item>
        <el-form-item label="样例输出"><el-input v-model="form.sampleOutput" type="textarea" /></el-form-item>
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="难度"><el-select v-model="form.difficulty"><el-option label="入门" value="easy" /><el-option label="进阶" value="medium" /><el-option label="挑战" value="hard" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="时间限制 ms"><el-input-number v-model="form.timeLimitMs" :min="500" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="内存限制 MB"><el-input-number v-model="form.memoryLimitMb" :min="16" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="可见性"><el-select v-model="form.visibility"><el-option label="私有" value="private" /><el-option label="公开" value="public" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="比较方式"><el-select v-model="form.compareMode"><el-option label="忽略行尾空白" value="ignore_trailing_space" /><el-option label="严格比较" value="strict" /></el-select></el-form-item></el-col>
        </el-row>
        <el-button type="primary" :loading="saving" @click="save">保存题目</el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { createProblemApi, getProblemApi, updateProblemApi, type ProblemPayload } from '@/api/teacherProblem'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => Boolean(route.params.problemId))
const problemId = computed(() => Number(route.params.problemId))
const saving = ref(false)
const form = reactive<ProblemPayload>({
  title: '',
  description: '',
  inputFormat: '',
  outputFormat: '',
  sampleInput: '',
  sampleOutput: '',
  difficulty: 'easy',
  timeLimitMs: 2000,
  memoryLimitMb: 128,
  compareMode: 'ignore_trailing_space',
  visibility: 'private'
})

async function load() {
  if (!isEdit.value) return
  Object.assign(form, await getProblemApi(problemId.value))
}
async function save() {
  saving.value = true
  try {
    if (isEdit.value) await updateProblemApi(problemId.value, form)
    else await createProblemApi(form)
    ElMessage.success('保存成功')
    await router.push('/teacher/problems')
  } finally {
    saving.value = false
  }
}
onMounted(load)
</script>
