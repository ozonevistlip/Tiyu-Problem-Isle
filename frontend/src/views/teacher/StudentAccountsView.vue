<template>
  <div class="page">
    <PageHeader title="学生账号" subtitle="创建和管理自己名下的学生账号">
      <template #actions><el-button type="primary" @click="openCreate">新增学生</el-button></template>
    </PageHeader>
    <el-table :data="students" class="panel">
      <el-table-column prop="username" label="账号" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column label="操作" width="270"><template #default="{ row }">
        <el-button link @click="openEdit(row)">编辑</el-button>
        <el-button link @click="resetPassword(row)">重置密码</el-button>
        <el-button link type="danger" @click="remove(row)">删除</el-button>
      </template></el-table-column>
    </el-table>
    <el-dialog v-model="visible" :title="form.id ? '编辑学生账号' : '新增学生账号'" width="460px">
      <el-form label-position="top"><el-form-item label="登录账号"><el-input v-model.trim="form.username" /></el-form-item><el-form-item v-if="!form.id" label="初始密码"><el-input v-model="form.password" type="password" show-password /></el-form-item><el-form-item label="姓名"><el-input v-model.trim="form.realName" /></el-form-item><el-form-item label="昵称"><el-input v-model.trim="form.nickname" /></el-form-item></el-form>
      <template #footer><el-button @click="visible = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import type { UserInfo } from '@/api/types'
import { listTeacherStudentsApi, createTeacherStudentApi, updateTeacherStudentApi, resetTeacherStudentPasswordApi, deleteTeacherStudentApi } from '@/api/teacherStudents'

const students = ref<UserInfo[]>([]), visible = ref(false)
const form = reactive({ id: 0, username: '', password: '', realName: '', nickname: '' })
async function load() { students.value = await listTeacherStudentsApi() }
function openCreate() { Object.assign(form, { id: 0, username: '', password: '', realName: '', nickname: '' }); visible.value = true }
function openEdit(row: UserInfo) { Object.assign(form, { id: row.id, username: row.username, password: '', realName: row.realName || '', nickname: row.nickname || '' }); visible.value = true }
async function save() {
  if (form.username.length < 3 || form.username.length > 30 || (!form.id && (form.password.length < 6 || form.password.length > 64))) return ElMessage.warning('账号需 3–30 字符，初始密码需 6–64 字符')
  if (form.id) await updateTeacherStudentApi(form.id, form)
  else await createTeacherStudentApi(form)
  visible.value = false; await load(); ElMessage.success('学生账号已保存')
}
async function resetPassword(row: UserInfo) {
  const { value } = await ElMessageBox.prompt(`为“${row.username}”设置新密码，原登录会失效`, '重置密码', { inputType: 'password', inputPattern: /^.{6,64}$/, inputErrorMessage: '密码需 6–64 字符' })
  await resetTeacherStudentPasswordApi(row.id, value); ElMessage.success('密码已重置')
}
async function remove(row: UserInfo) {
  await ElMessageBox.confirm(`删除“${row.username}”账号？该学生会从所有班级移除。`, '确认删除', { type: 'warning' })
  await deleteTeacherStudentApi(row.id); await load(); ElMessage.success('学生账号已删除')
}
onMounted(load)
</script>
