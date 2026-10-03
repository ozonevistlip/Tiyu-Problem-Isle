<template>
  <div class="page">
    <PageHeader title="学生账号" subtitle="创建和管理自己名下的学生账号">
      <template #actions><el-button type="primary" @click="openCreate">新增学生</el-button></template>
    </PageHeader>
    <el-table :data="students" class="panel">
      <el-table-column prop="username" label="账号" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column label="操作" width="340"><template #default="{ row }">
        <el-button link type="primary" @click="managePets(row)">逐只管理宠物</el-button>
        <el-button link @click="openEdit(row)">编辑</el-button>
        <el-button link @click="resetPassword(row)">重置密码</el-button>
        <el-button link type="danger" @click="remove(row)">删除</el-button>
      </template></el-table-column>
    </el-table>
    <el-dialog v-model="visible" :title="form.id ? '编辑学生账号' : '新增学生账号'" width="460px">
      <el-form label-position="top"><el-form-item label="登录账号"><el-input v-model.trim="form.username" /></el-form-item><el-form-item v-if="!form.id" label="初始密码"><el-input v-model="form.password" type="password" show-password /></el-form-item><el-form-item label="姓名"><el-input v-model.trim="form.realName" /></el-form-item><el-form-item label="昵称"><el-input v-model.trim="form.nickname" /></el-form-item></el-form>
      <template #footer><el-button @click="visible = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="petsVisible" :title="`管理 ${petStudent?.realName || petStudent?.username || ''} 的宠物`" width="min(520px, 94vw)">
      <p>每只宠物单独发放或收回。操作当前宠物不会改变其他宠物的权限。</p>
      <div v-for="pet in petOptions" :key="pet.id" class="pet-grant-row">
        <span>{{ pet.name }}</span>
        <el-switch :model-value="grantIds.includes(pet.id)" :loading="savingPetId === pet.id" active-text="已发放" inactive-text="未发放" @change="setGrant(pet.id, Boolean($event))" />
      </div>
      <p v-if="!petOptions.length">暂无已发布宠物，请联系超级管理员。</p>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import type { UserInfo } from '@/api/types'
import { listTeacherStudentsApi, createTeacherStudentApi, updateTeacherStudentApi, resetTeacherStudentPasswordApi, deleteTeacherStudentApi } from '@/api/teacherStudents'
import { listPetsApi, petGrantsApi, setPetGrantApi, type PetInfo } from '@/api/pets'

const students = ref<UserInfo[]>([]), visible = ref(false)
const form = reactive({ id: 0, username: '', password: '', realName: '', nickname: '' })
const petsVisible = ref(false), petStudent = ref<UserInfo | null>(null)
const petOptions = ref<PetInfo[]>([]), grantIds = ref<number[]>([]), savingPetId = ref<number | null>(null)
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
async function managePets(row: UserInfo) {
  petStudent.value = row
  const [pets, grants] = await Promise.all([listPetsApi(), petGrantsApi(row.id)])
  petOptions.value = pets; grantIds.value = grants; petsVisible.value = true
}
async function setGrant(petId: number, granted: boolean) {
  if (!petStudent.value || savingPetId.value) return
  savingPetId.value = petId
  try {
    await setPetGrantApi(petStudent.value.id, petId, granted)
    grantIds.value = granted ? [...grantIds.value, petId] : grantIds.value.filter(id => id !== petId)
  } finally { savingPetId.value = null }
}
onMounted(load)
</script>

<style scoped>
.pet-grant-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid var(--border-color); }
</style>
