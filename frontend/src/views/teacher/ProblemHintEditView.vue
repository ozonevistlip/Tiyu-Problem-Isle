<template>
  <div class="page">
    <PageHeader title="题目提示" subtitle="提示会按比赛题目的解锁时间自动展示给学生" back>
      <template #actions><el-button type="primary" @click="openCreate">添加提示</el-button></template>
    </PageHeader>
    <el-alert title="这些提示不会一开始展示给学生，而是会在比赛中按解锁时间自动显示。" type="warning" show-icon :closable="false" />
    <el-table v-loading="loading" :data="items" class="panel">
      <el-table-column prop="hintLevel" label="等级" width="90" />
      <el-table-column prop="hintTitle" label="标题" width="180" />
      <el-table-column prop="hintContent" label="内容" show-overflow-tooltip />
      <el-table-column label="操作" width="160">
        <template #default="{ row }"><el-button text @click="openEdit(row)">编辑</el-button><el-button text type="danger" @click="remove(row.id)">删除</el-button></template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="visible" :title="editingId ? '编辑提示' : '添加提示'" width="640px">
      <el-form :model="form" label-position="top">
        <el-form-item label="提示标题"><el-input v-model="form.hintTitle" /></el-form-item>
        <el-form-item label="提示内容"><el-input v-model="form.hintContent" type="textarea" :rows="6" /></el-form-item>
        <el-form-item label="提示等级"><el-radio-group v-model="form.hintLevel"><el-radio-button :label="1">基础提示</el-radio-button><el-radio-button :label="2">关键思路</el-radio-button><el-radio-button :label="3">完整解法</el-radio-button></el-radio-group></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { createHintApi, deleteHintApi, listHintsApi, updateHintApi, type HintPayload } from '@/api/teacherProblem'
import type { HintInfo } from '@/api/types'

const route = useRoute()
const problemId = computed(() => Number(route.params.problemId))
const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const editingId = ref<number | null>(null)
const items = ref<HintInfo[]>([])
const form = reactive<HintPayload>({ hintTitle: '', hintContent: '', hintLevel: 1 })

async function load() {
  loading.value = true
  try {
    items.value = await listHintsApi(problemId.value)
  } finally {
    loading.value = false
  }
}
function openCreate() {
  editingId.value = null
  Object.assign(form, { hintTitle: '', hintContent: '', hintLevel: 1 })
  visible.value = true
}
function openEdit(row: HintInfo) {
  editingId.value = row.id
  Object.assign(form, row)
  visible.value = true
}
async function save() {
  saving.value = true
  try {
    if (editingId.value) await updateHintApi(editingId.value, form)
    else await createHintApi(problemId.value, form)
    ElMessage.success('保存成功')
    visible.value = false
    await load()
  } finally {
    saving.value = false
  }
}
async function remove(id: number) {
  await ElMessageBox.confirm('确定删除该提示吗？', '删除确认')
  await deleteHintApi(id)
  ElMessage.success('已删除')
  await load()
}
onMounted(load)
</script>
