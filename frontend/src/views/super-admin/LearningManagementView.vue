<template>
  <div class="page">
    <PageHeader title="班级类型与课后内容" subtitle="同类型班级共用课次、作业与讲解视频">
      <template #actions><el-button type="primary" @click="editType()">新增类型</el-button></template>
    </PageHeader>
    <div class="panel block">
      <el-table :data="types">
        <el-table-column prop="name" label="班级类型" />
        <el-table-column prop="description" label="说明" />
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button link type="primary" @click="chooseType(row)">管理课次</el-button>
            <el-button link @click="editType(row)">编辑</el-button>
            <el-button link type="danger" @click="removeType(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <section v-if="selectedType" class="panel block">
      <div class="section-head"><h2>{{ selectedType.name }} 的课次</h2><el-button type="primary" @click="editLesson()">新增课次</el-button></div>
      <el-table :data="lessons">
        <el-table-column prop="lessonOrder" label="序号" width="90" />
        <el-table-column prop="title" label="课次文件夹" />
        <el-table-column label="状态" width="110"><template #default="{ row }">{{ row.status === 1 ? '已发布' : '草稿' }}</template></el-table-column>
        <el-table-column label="操作" width="260"><template #default="{ row }">
          <el-button link type="primary" @click="chooseLesson(row)">课后内容</el-button>
          <el-button link @click="editLesson(row)">编辑</el-button>
          <el-button link type="danger" @click="removeLesson(row)">删除</el-button>
        </template></el-table-column>
      </el-table>
    </section>
    <section v-if="selectedLesson" class="panel block">
      <div class="section-head"><h2>{{ selectedLesson.title }} · 课后内容</h2><span>每个类别独立授权给学生</span></div>
      <div class="content-actions"><el-button @click="openHomework">新增作业</el-button><el-button type="primary" @click="videoVisible = true">上传讲解视频</el-button></div>
      <el-table :data="materials">
        <el-table-column prop="title" label="标题" />
        <el-table-column label="类别" width="100"><template #default="{ row }">{{ tierNames[row.tier as Tier] }}</template></el-table-column>
        <el-table-column label="形式" width="100"><template #default="{ row }">{{ row.kind === 'VIDEO' ? '视频' : '作业' }}</template></el-table-column>
        <el-table-column prop="content" label="作业内容" show-overflow-tooltip />
        <el-table-column label="操作" width="100"><template #default="{ row }"><el-button link type="danger" @click="removeMaterial(row)">删除</el-button></template></el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="typeVisible" :title="typeForm.id ? '编辑班级类型' : '新增班级类型'" width="440px">
      <el-form label-position="top"><el-form-item label="类型名称"><el-input v-model.trim="typeForm.name" placeholder="例如 4.0" /></el-form-item><el-form-item label="说明"><el-input v-model="typeForm.description" /></el-form-item></el-form>
      <template #footer><el-button @click="typeVisible = false">取消</el-button><el-button type="primary" @click="saveType">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="lessonVisible" :title="lessonForm.id ? '编辑课次' : '新增课次'" width="440px">
      <el-form label-position="top"><el-form-item label="课次名称"><el-input v-model.trim="lessonForm.title" placeholder="例如 第 1 课" /></el-form-item><el-form-item label="课次序号"><el-input-number v-model="lessonForm.lessonOrder" :min="1" /></el-form-item><el-form-item label="发布状态"><el-switch v-model="lessonForm.published" active-text="发布给学生" /></el-form-item></el-form>
      <template #footer><el-button @click="lessonVisible = false">取消</el-button><el-button type="primary" @click="saveLesson">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="homeworkVisible" title="新增课后作业" width="500px">
      <el-form label-position="top"><el-form-item label="类别"><el-select v-model="homeworkForm.tier"><el-option v-for="(name, key) in tierNames" :key="key" :label="name" :value="key" /></el-select></el-form-item><el-form-item label="标题"><el-input v-model.trim="homeworkForm.title" /></el-form-item><el-form-item label="作业内容"><el-input v-model="homeworkForm.content" type="textarea" :rows="5" /></el-form-item></el-form>
      <template #footer><el-button @click="homeworkVisible = false">取消</el-button><el-button type="primary" @click="saveHomework">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="videoVisible" title="上传课后讲解视频" width="500px">
      <el-form label-position="top"><el-form-item label="类别"><el-select v-model="videoForm.tier"><el-option v-for="(name, key) in tierNames" :key="key" :label="name" :value="key" /></el-select></el-form-item><el-form-item label="标题"><el-input v-model.trim="videoForm.title" /></el-form-item><el-form-item label="视频文件（最大 500MB）"><input type="file" accept="video/*" @change="onVideoChange" /></el-form-item></el-form>
      <template #footer><el-button @click="videoVisible = false">取消</el-button><el-button type="primary" :loading="uploading" @click="saveVideo">上传</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { tierNames, listTypesApi, createTypeApi, updateTypeApi, deleteTypeApi, listLessonsApi, createLessonApi, updateLessonApi, deleteLessonApi, listMaterialsApi, createHomeworkApi, uploadVideoApi, deleteMaterialApi } from '@/api/learning'
import type { ClassType, Lesson, Material, Tier } from '@/api/learning'

const types = ref<ClassType[]>([]), lessons = ref<Lesson[]>([]), materials = ref<Material[]>([])
const selectedType = ref<ClassType>(), selectedLesson = ref<Lesson>()
const typeVisible = ref(false), lessonVisible = ref(false), homeworkVisible = ref(false), videoVisible = ref(false), uploading = ref(false)
const typeForm = reactive({ id: 0, name: '', description: '' })
const lessonForm = reactive({ id: 0, title: '', lessonOrder: 1, published: false })
const homeworkForm = reactive<{ title: string; tier: Tier; content: string }>({ title: '', tier: 'BASIC', content: '' })
const videoForm = reactive<{ title: string; tier: Tier; file?: File }>({ title: '', tier: 'BASIC' })

async function loadTypes() { types.value = await listTypesApi() }
async function chooseType(type: ClassType) { selectedType.value = type; selectedLesson.value = undefined; materials.value = []; lessons.value = await listLessonsApi(type.id) }
async function chooseLesson(lesson: Lesson) { selectedLesson.value = lesson; materials.value = await listMaterialsApi(lesson.id) }
function editType(type?: ClassType) { Object.assign(typeForm, { id: type?.id || 0, name: type?.name || '', description: type?.description || '' }); typeVisible.value = true }
async function saveType() { if (!typeForm.name) return ElMessage.warning('请输入类型名称'); if (typeForm.id) await updateTypeApi(typeForm.id, typeForm); else await createTypeApi(typeForm); typeVisible.value = false; await loadTypes(); ElMessage.success('已保存') }
async function removeType(type: ClassType) { await ElMessageBox.confirm(`删除班级类型“${type.name}”？`, '确认删除'); await deleteTypeApi(type.id); if (selectedType.value?.id === type.id) { selectedType.value = undefined; selectedLesson.value = undefined }; await loadTypes() }
function editLesson(lesson?: Lesson) { Object.assign(lessonForm, { id: lesson?.id || 0, title: lesson?.title || '', lessonOrder: lesson?.lessonOrder || lessons.value.length + 1, published: lesson?.status === 1 }); lessonVisible.value = true }
async function saveLesson() { if (!selectedType.value || !lessonForm.title) return ElMessage.warning('请输入课次名称'); const data = { title: lessonForm.title, lessonOrder: lessonForm.lessonOrder, status: lessonForm.published ? 1 : 0 }; if (lessonForm.id) await updateLessonApi(lessonForm.id, data); else await createLessonApi(selectedType.value.id, data); lessonVisible.value = false; await chooseType(selectedType.value); ElMessage.success('已保存') }
async function removeLesson(lesson: Lesson) { await ElMessageBox.confirm(`删除“${lesson.title}”及全部课后内容？`, '确认删除'); await deleteLessonApi(lesson.id); if (selectedLesson.value?.id === lesson.id) selectedLesson.value = undefined; if (selectedType.value) await chooseType(selectedType.value) }
function openHomework() { Object.assign(homeworkForm, { title: '', tier: 'BASIC', content: '' }); homeworkVisible.value = true }
async function saveHomework() { if (!selectedLesson.value || !homeworkForm.title || !homeworkForm.content) return ElMessage.warning('请填写标题和内容'); await createHomeworkApi(selectedLesson.value.id, homeworkForm); homeworkVisible.value = false; await chooseLesson(selectedLesson.value) }
function onVideoChange(event: Event) { videoForm.file = (event.target as HTMLInputElement).files?.[0] }
async function saveVideo() { if (!selectedLesson.value || !videoForm.file || !videoForm.title) return ElMessage.warning('请选择视频并填写标题'); const data = new FormData(); data.append('tier', videoForm.tier); data.append('title', videoForm.title); data.append('file', videoForm.file); uploading.value = true; try { await uploadVideoApi(selectedLesson.value.id, data); videoVisible.value = false; videoForm.file = undefined; videoForm.title = ''; await chooseLesson(selectedLesson.value); ElMessage.success('视频已上传') } finally { uploading.value = false } }
async function removeMaterial(material: Material) { await ElMessageBox.confirm(`删除“${material.title}”？`, '确认删除'); await deleteMaterialApi(material.id); if (selectedLesson.value) await chooseLesson(selectedLesson.value) }
onMounted(loadTypes)
</script>

<style scoped>
.block { padding: 18px; margin-top: 18px; }
.section-head, .content-actions { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.section-head { justify-content: space-between; }
.section-head h2 { margin: 0; font-size: 18px; }
.section-head span { color: var(--text-muted); }
</style>
