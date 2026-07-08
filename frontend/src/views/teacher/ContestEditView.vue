<template>
  <div class="page">
    <PageHeader :title="isEdit ? '编辑比赛' : '创建比赛'" subtitle="选择班级并设置比赛时间" back />
    <div class="panel form-card">
      <el-form :model="form" label-position="top">
        <el-form-item label="比赛标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="比赛说明"><el-input v-model="form.description" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="班级"><el-select v-model="form.classId" placeholder="请选择班级"><el-option v-for="item in classes" :key="item.id" :label="item.className" :value="item.id" /></el-select></el-form-item>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="开始时间"><el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="结束时间"><el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="比赛设置"><el-checkbox v-model="form.showRank">显示排行榜</el-checkbox><el-checkbox v-model="form.allowSubmitAfterEnd">允许赛后提交</el-checkbox></el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存比赛</el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { listClassesApi } from '@/api/teacherClass'
import { createContestApi, getTeacherContestApi, updateContestApi, type ContestPayload } from '@/api/teacherContest'
import type { ClassInfo } from '@/api/types'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => Boolean(route.params.contestId))
const contestId = computed(() => Number(route.params.contestId))
const saving = ref(false)
const classes = ref<ClassInfo[]>([])
const form = reactive<ContestPayload>({ classId: 0, title: '', description: '', startTime: '', endTime: '', showRank: true, allowSubmitAfterEnd: false })

async function load() {
  classes.value = await listClassesApi()
  if (!isEdit.value) return
  Object.assign(form, await getTeacherContestApi(contestId.value))
}
async function save() {
  saving.value = true
  try {
    if (isEdit.value) await updateContestApi(contestId.value, form)
    else await createContestApi(form)
    ElMessage.success('保存成功')
    await router.push('/teacher/contests')
  } finally {
    saving.value = false
  }
}
onMounted(load)
</script>
