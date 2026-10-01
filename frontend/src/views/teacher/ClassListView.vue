<template>
  <div class="page">
    <PageHeader title="班级管理" subtitle="创建班级并维护学生名单">
      <template #actions><el-button type="primary" @click="openCreate">新建班级</el-button></template>
    </PageHeader>
    <el-table v-loading="loading" :data="classes" class="panel">
      <el-table-column prop="className" label="班级名称" />
      <el-table-column prop="classTypeName" label="班级类型" />
      <el-table-column prop="description" label="说明" />
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100"><template #default><el-tag type="success">正常</el-tag></template></el-table-column>
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button text type="primary" @click="$router.push(`/teacher/classes/${row.id}`)">详情</el-button>
          <el-button text @click="openEdit(row)">编辑</el-button>
          <el-button text type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="visible" :title="editingId ? '编辑班级' : '新建班级'" width="460px">
      <el-form :model="form" label-position="top">
        <el-form-item label="班级名称"><el-input v-model="form.className" /></el-form-item>
        <el-form-item label="班级类型"><el-select v-model="form.classTypeId" placeholder="请选择管理员创建的类型" style="width:100%"><el-option v-for="type in types" :key="type.id" :label="type.name" :value="type.id" /></el-select></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { createClassApi, deleteClassApi, listClassesApi, updateClassApi } from '@/api/teacherClass'
import type { ClassInfo } from '@/api/types'
import { formatDateTime } from '@/utils/time'
import { listTypesApi } from '@/api/learning'
import type { ClassType } from '@/api/learning'

const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const editingId = ref<number | null>(null)
const classes = ref<ClassInfo[]>([])
const types = ref<ClassType[]>([])
const form = reactive({ className: '', classTypeId: 0, description: '' })

async function load() {
  loading.value = true
  try {
    classes.value = await listClassesApi()
  } finally {
    loading.value = false
  }
}
function openCreate() {
  editingId.value = null
  form.className = ''
  form.classTypeId = types.value[0]?.id || 0
  form.description = ''
  visible.value = true
}
function openEdit(row: ClassInfo) {
  editingId.value = row.id
  form.className = row.className
  form.classTypeId = row.classTypeId || 0
  form.description = row.description || ''
  visible.value = true
}
async function save() {
  if (!form.className.trim() || !form.classTypeId) { ElMessage.warning('请输入班级名称并选择班级类型'); return }
  saving.value = true
  try {
    if (editingId.value) await updateClassApi(editingId.value, form)
    else await createClassApi(form)
    ElMessage.success('保存成功')
    visible.value = false
    await load()
  } finally {
    saving.value = false
  }
}
async function remove(id: number) {
  await ElMessageBox.confirm('确定删除这个班级吗？', '删除确认')
  await deleteClassApi(id)
  ElMessage.success('已删除')
  await load()
}
onMounted(async () => { types.value = await listTypesApi(); await load() })
</script>
