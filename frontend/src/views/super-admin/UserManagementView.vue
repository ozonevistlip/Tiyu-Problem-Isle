<template>
  <section class="page">
    <div class="panel heading">
      <div><h1>用户管理</h1><p>新增老师账号，并查看全站老师与学生账号。</p></div>
      <el-button type="primary" @click="openCreate">新增老师</el-button>
    </div>
    <div class="panel">
      <div class="filters">
        <el-input v-model="query.keyword" clearable placeholder="搜索账号、姓名或昵称" @keyup.enter="search" />
        <el-select v-model="query.role" clearable placeholder="全部角色">
          <el-option label="老师" value="teacher" /><el-option label="学生" value="student" /><el-option label="超级管理员" value="SUPER_ADMIN" />
        </el-select>
        <el-select v-model="query.status" clearable placeholder="全部状态">
          <el-option label="正常" :value="1" /><el-option label="已禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button><el-button @click="resetFilters">重置</el-button>
      </div>
      <el-table :data="pageData.records" v-loading="loading" @row-dblclick="showDetail">
        <el-table-column prop="id" label="ID" width="76" />
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }"><div class="user-cell"><span>{{ initials(row) }}</span><div><strong>{{ row.realName || row.nickname || row.username }}</strong><small>@{{ row.username }}</small></div></div></template>
        </el-table-column>
        <el-table-column label="角色" width="120"><template #default="{ row }"><el-tag :type="roleTag(row.role)">{{ roleName(row.role) }}</el-tag></template></el-table-column>
        <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '已禁用' }}</el-tag></template></el-table-column>
        <el-table-column label="最后登录" min-width="170"><template #default="{ row }">{{ formatDateTime(row.lastLoginAt) }}</template></el-table-column>
        <el-table-column label="创建时间" min-width="170"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">详情</el-button>
            <template v-if="row.role === 'teacher'">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">{{ row.status === 1 ? '禁用' : '解封' }}</el-button>
              <el-dropdown trigger="click">
                <el-button link type="primary">更多</el-button>
                <template #dropdown><el-dropdown-menu>
                  <el-dropdown-item @click="resetPassword(row)">重置密码</el-dropdown-item>
                  <el-dropdown-item @click="forceLogout(row)">强制退出</el-dropdown-item>
                  <el-dropdown-item divided class="danger-action" @click="remove(row)">删除账号</el-dropdown-item>
                </el-dropdown-menu></template>
              </el-dropdown>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="pageData.total" :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next" @current-change="load" @size-change="search" />
    </div>

    <el-dialog v-model="editorVisible" :title="editingId ? '修改用户' : '新增用户'" width="min(92vw, 560px)" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid"><el-form-item label="登录账号" prop="username"><el-input v-model.trim="form.username" /></el-form-item><el-form-item label="角色"><el-input :model-value="form.role === 'teacher' ? '老师' : '学生'" disabled /></el-form-item></div>
        <el-form-item v-if="!editingId" label="初始密码" prop="password"><el-input v-model="form.password" type="password" show-password autocomplete="new-password" /></el-form-item>
        <div class="form-grid"><el-form-item label="真实姓名"><el-input v-model="form.realName" /></el-form-item><el-form-item label="昵称"><el-input v-model="form.nickname" /></el-form-item></div>
      </el-form>
      <template #footer><el-button @click="editorVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveUser">保存</el-button></template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="用户详细资料" size="min(92vw, 480px)">
      <div v-if="detail" class="detail-card">
        <div class="detail-head"><span>{{ initials(detail) }}</span><div><h2>{{ detail.realName || detail.username }}</h2><p>@{{ detail.username }}</p></div></div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户 ID">{{ detail.id }}</el-descriptions-item><el-descriptions-item label="角色">{{ roleName(detail.role) }}</el-descriptions-item><el-descriptions-item label="状态">{{ detail.status === 1 ? '正常' : '已禁用' }}</el-descriptions-item><el-descriptions-item label="真实姓名">{{ detail.realName || '-' }}</el-descriptions-item><el-descriptions-item label="昵称">{{ detail.nickname || '-' }}</el-descriptions-item><el-descriptions-item label="最后登录">{{ formatDateTime(detail.lastLoginAt) }}</el-descriptions-item><el-descriptions-item label="最后活跃">{{ formatDateTime(detail.lastActiveAt) }}</el-descriptions-item><el-descriptions-item label="创建时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item><el-descriptions-item label="更新时间">{{ formatDateTime(detail.updatedAt) }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { createUserApi, deleteUserApi, forceLogoutUserApi, resetPasswordApi, updateUserApi, updateUserStatusApi, userDetailApi, usersApi } from '@/api/superAdmin'
import type { PageResult, UserInfo, UserRole } from '@/api/types'
import { formatDateTime } from '@/utils/time'

const loading = ref(false), saving = ref(false), editorVisible = ref(false), detailVisible = ref(false)
const editingId = ref<number>(), detail = ref<UserInfo>(), formRef = ref<FormInstance>()
const query = reactive<{ page: number; size: number; keyword: string; role: string; status?: number }>({ page: 1, size: 20, keyword: '', role: '' })
const pageData = reactive<PageResult<UserInfo>>({ records: [], total: 0, current: 1, size: 20, pages: 0 })
const emptyForm = () => ({ username: '', password: '', role: 'teacher' as 'teacher' | 'student', realName: '', nickname: '' })
const form = reactive(emptyForm())
const rules: FormRules = { username: [{ required: true, message: '请输入账号', trigger: 'blur' }, { min: 3, max: 30, message: '长度为 3-30 个字符', trigger: 'blur' }], password: [{ validator: (_r, value, callback) => !editingId.value && (!value || value.length < 6) ? callback(new Error('初始密码至少 6 个字符')) : callback(), trigger: 'blur' }], role: [{ required: true, message: '请选择角色' }] }

async function load() { loading.value = true; try { Object.assign(pageData, await usersApi({ ...query, role: query.role || undefined })) } finally { loading.value = false } }
function search() { query.page = 1; void load() }
function resetFilters() { Object.assign(query, { page: 1, keyword: '', role: '', status: undefined }); void load() }
function openCreate() { editingId.value = undefined; Object.assign(form, emptyForm()); editorVisible.value = true }
function openEdit(row: UserInfo) { editingId.value = row.id; Object.assign(form, { username: row.username, password: '', role: row.role, realName: row.realName || '', nickname: row.nickname || '' }); editorVisible.value = true }
async function saveUser() { if (!await formRef.value?.validate().catch(() => false)) return; saving.value = true; try { if (editingId.value) await updateUserApi(editingId.value, { username: form.username, role: form.role, realName: form.realName, nickname: form.nickname }); else await createUserApi(form); ElMessage.success('用户信息已保存'); editorVisible.value = false; await load() } finally { saving.value = false } }
async function showDetail(row: UserInfo) { detailVisible.value = true; detail.value = await userDetailApi(row.id) }
async function toggleStatus(row: UserInfo) { const action = row.status === 1 ? '禁用' : '解封'; await ElMessageBox.confirm(`确认${action}账号“${row.username}”吗？${row.status === 1 ? '该用户会立即下线且无法登录。' : ''}`, `${action}账号`, { type: 'warning' }); await updateUserStatusApi(row.id, row.status === 1 ? 0 : 1); ElMessage.success(`账号已${action}`); await load() }
async function resetPassword(row: UserInfo) { const { value } = await ElMessageBox.prompt(`为“${row.username}”设置新密码，保存后其所有会话会失效。`, '重置密码', { inputType: 'password', inputPattern: /^.{6,64}$/, inputErrorMessage: '密码长度必须为 6-64 个字符' }); await resetPasswordApi(row.id, value); ElMessage.success('密码已重置，用户已下线') }
async function forceLogout(row: UserInfo) { await ElMessageBox.confirm(`强制账号“${row.username}”的所有设备退出登录？`, '强制退出', { type: 'warning' }); await forceLogoutUserApi(row.id); ElMessage.success('该账号的在线会话已终止') }
async function remove(row: UserInfo) { await ElMessageBox.confirm(`确定永久删除账号“${row.username}”吗？此操作不可撤销。`, '删除账号', { type: 'error', confirmButtonText: '永久删除' }); await deleteUserApi(row.id); ElMessage.success('账号已删除'); await load() }
const roleName = (role: UserRole) => role === 'SUPER_ADMIN' ? '超级管理员' : role === 'teacher' ? '老师' : '学生'
const roleTag = (role: UserRole) => role === 'SUPER_ADMIN' ? 'danger' : role === 'teacher' ? 'warning' : 'primary'
const initials = (row: UserInfo) => (row.realName || row.nickname || row.username).slice(0, 1).toUpperCase()
void load()
</script>

<style scoped lang="scss">
.heading { display: flex; align-items: center; justify-content: space-between; }.heading h1 { margin: 0 0 6px; }.heading p { margin: 0; color: var(--text-muted); }.filters { display: grid; grid-template-columns: minmax(220px, 1fr) 160px 140px auto auto; gap: 10px; margin-bottom: 18px; }.user-cell { display: flex; gap: 10px; align-items: center; }.user-cell > span, .detail-head > span { display: grid; width: 36px; height: 36px; flex: none; place-items: center; color: white; background: linear-gradient(135deg, var(--color-primary), var(--color-accent)); border-radius: 11px; font-weight: 900; }.user-cell strong, .user-cell small { display: block; }.user-cell small { margin-top: 2px; color: var(--text-muted); }.el-pagination { justify-content: flex-end; margin-top: 18px; }.el-dropdown { margin-left: 12px; }.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }.form-grid .el-select { width: 100%; }.detail-head { display: flex; align-items: center; gap: 14px; margin-bottom: 24px; }.detail-head > span { width: 54px; height: 54px; border-radius: 16px; font-size: 20px; }.detail-head h2, .detail-head p { margin: 0; }.detail-head p { color: var(--text-muted); }.danger-action { color: var(--color-danger); }
@media (max-width: 850px) { .filters { grid-template-columns: 1fr 1fr; }.heading { align-items: flex-start; gap: 12px; flex-direction: column; } } @media (max-width: 520px) { .filters, .form-grid { grid-template-columns: 1fr; } }
</style>
