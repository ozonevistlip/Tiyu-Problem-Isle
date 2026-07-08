<template>
  <div class="page">
    <PageHeader title="测试点管理" subtitle="配置输入、标准输出、分数和顺序" back>
      <template #actions><el-button type="primary" @click="openCreate">添加测试点</el-button></template>
    </PageHeader>
    <el-table v-loading="loading" :data="items" class="panel">
      <el-table-column prop="sortOrder" label="顺序" width="80" />
      <el-table-column prop="score" label="分数" width="80" />
      <el-table-column label="样例" width="90"><template #default="{ row }"><el-tag v-if="row.sample">是</el-tag><span v-else>否</span></template></el-table-column>
      <el-table-column prop="inputData" label="输入" show-overflow-tooltip />
      <el-table-column prop="outputData" label="输出" show-overflow-tooltip />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button text @click="openEdit(row)">编辑</el-button>
          <el-button text type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="visible" :title="editingId ? '编辑测试点' : '添加测试点'" width="720px">
      <el-form :model="form" label-position="top">
        <el-form-item label="输入数据"><el-input v-model="form.inputData" type="textarea" :rows="5" /></el-form-item>
        <el-form-item label="标准输出"><el-input v-model="form.outputData" type="textarea" :rows="5" /></el-form-item>
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="分数"><el-input-number v-model="form.score" :min="0" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="顺序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="样例"><el-switch v-model="form.sample" /></el-form-item></el-col>
        </el-row>
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
import { createTestcaseApi, deleteTestcaseApi, listTestcasesApi, updateTestcaseApi, type TestcasePayload } from '@/api/teacherProblem'
import type { TestcaseInfo } from '@/api/types'

const route = useRoute()
const problemId = computed(() => Number(route.params.problemId))
const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const editingId = ref<number | null>(null)
const items = ref<TestcaseInfo[]>([])
const form = reactive<TestcasePayload>({ inputData: '', outputData: '', score: 100, sortOrder: 0, sample: false })

async function load() {
  loading.value = true
  try {
    items.value = await listTestcasesApi(problemId.value)
  } finally {
    loading.value = false
  }
}
function openCreate() {
  editingId.value = null
  Object.assign(form, { inputData: '', outputData: '', score: 100, sortOrder: items.value.length + 1, sample: false })
  visible.value = true
}
function openEdit(row: TestcaseInfo) {
  editingId.value = row.id
  Object.assign(form, row)
  visible.value = true
}
async function save() {
  saving.value = true
  try {
    if (editingId.value) await updateTestcaseApi(editingId.value, form)
    else await createTestcaseApi(problemId.value, form)
    ElMessage.success('保存成功')
    visible.value = false
    await load()
  } finally {
    saving.value = false
  }
}
async function remove(id: number) {
  await ElMessageBox.confirm('确定删除该测试点吗？', '删除确认')
  await deleteTestcaseApi(id)
  ElMessage.success('已删除')
  await load()
}
onMounted(load)
</script>
