<template>
  <section class="page">
    <div class="panel heading"><div><h1>全站公告</h1><p>创建草稿或立即向全站用户发布通知。</p></div><el-button type="primary" @click="openCreate">发布公告</el-button></div>
    <div class="announcement-grid" v-loading="loading">
      <article v-for="item in items" :key="item.id" class="panel announcement">
        <div class="announcement-head"><el-tag :type="item.status === 1 ? 'success' : 'info'">{{ item.status === 1 ? '已发布' : '草稿' }}</el-tag><span>{{ formatDateTime(item.publishedAt || item.createdAt) }}</span></div>
        <h3>{{ item.title }}</h3><p>{{ item.content }}</p>
        <div class="actions"><el-button link type="primary" @click="openEdit(item)">编辑</el-button><el-button link type="danger" @click="remove(item)">删除</el-button></div>
      </article>
      <el-empty v-if="!loading && !items.length" description="暂时没有公告" />
    </div>
    <el-dialog v-model="visible" :title="editingId ? '编辑公告' : '新建公告'" width="min(92vw, 620px)">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top"><el-form-item label="公告标题" prop="title"><el-input v-model="form.title" maxlength="150" show-word-limit /></el-form-item><el-form-item label="公告内容" prop="content"><el-input v-model="form.content" type="textarea" :rows="7" /></el-form-item><el-form-item label="发布状态"><el-radio-group v-model="form.status"><el-radio-button :value="0">保存草稿</el-radio-button><el-radio-button :value="1">立即发布</el-radio-button></el-radio-group></el-form-item></el-form>
      <template #footer><el-button @click="visible = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { announcementsApi, createAnnouncementApi, deleteAnnouncementApi, updateAnnouncementApi } from '@/api/superAdmin'
import type { Announcement } from '@/api/types'
import { formatDateTime } from '@/utils/time'

const items = ref<Announcement[]>([]), loading = ref(false), saving = ref(false), visible = ref(false), editingId = ref<number>(), formRef = ref<FormInstance>()
const form = reactive({ title: '', content: '', status: 1 })
const rules: FormRules = { title: [{ required: true, message: '请输入公告标题' }], content: [{ required: true, message: '请输入公告内容' }] }
async function load() { loading.value = true; try { items.value = await announcementsApi() } finally { loading.value = false } }
function openCreate() { editingId.value = undefined; Object.assign(form, { title: '', content: '', status: 1 }); visible.value = true }
function openEdit(item: Announcement) { editingId.value = item.id; Object.assign(form, { title: item.title, content: item.content, status: item.status }); visible.value = true }
async function save() { if (!await formRef.value?.validate().catch(() => false)) return; saving.value = true; try { if (editingId.value) await updateAnnouncementApi(editingId.value, form); else await createAnnouncementApi(form); ElMessage.success(form.status === 1 ? '公告已发布' : '草稿已保存'); visible.value = false; await load() } finally { saving.value = false } }
async function remove(item: Announcement) { await ElMessageBox.confirm(`删除公告“${item.title}”？`, '删除公告', { type: 'warning' }); await deleteAnnouncementApi(item.id); ElMessage.success('公告已删除'); await load() }
onMounted(load)
</script>

<style scoped lang="scss">
.heading { display: flex; align-items: center; justify-content: space-between; }.heading h1 { margin: 0 0 6px; }.heading p { margin: 0; color: var(--text-muted); }.announcement-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; }.announcement { display: flex; min-height: 220px; flex-direction: column; }.announcement-head { display: flex; align-items: center; justify-content: space-between; color: var(--text-muted); font-size: 12px; }.announcement h3 { margin: 18px 0 8px; }.announcement p { margin: 0; color: var(--text-secondary); line-height: 1.7; white-space: pre-wrap; }.actions { margin-top: auto; padding-top: 18px; border-top: 1px solid var(--border-color); } @media (max-width: 760px) { .announcement-grid { grid-template-columns: 1fr; }.heading { align-items: flex-start; gap: 12px; flex-direction: column; } }
</style>
