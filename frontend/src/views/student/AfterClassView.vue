<template>
  <div class="page">
    <PageHeader title="课后学习" subtitle="每节课是独立文件夹，只显示老师为你指定的类别" />
    <div class="panel block">
      <el-empty v-if="!classes.length" description="暂时没有已加入的班级" />
      <el-select v-else v-model="selectedClassId" placeholder="选择班级" @change="loadLessons">
        <el-option v-for="item in classes" :key="item.classId" :label="`${item.className} · ${item.classTypeName}`" :value="item.classId" />
      </el-select>
    </div>
    <div class="folders">
      <button v-for="lesson in lessons" :key="lesson.id" class="panel folder" :class="{ active: selectedLessonId === lesson.id }" @click="selectedLessonId = lesson.id">
        <span class="folder-icon">📁</span><strong>第 {{ lesson.lessonOrder }} 课</strong><small>{{ lesson.title }}</small><small>{{ lesson.tier ? tierNames[lesson.tier] : '老师尚未设置类别' }}</small>
      </button>
    </div>
    <el-empty v-if="selectedClassId && !lessons.length" description="这个班级还没有已发布的课次" />
    <div v-if="selectedLesson" class="panel block">
      <h2>第 {{ selectedLesson.lessonOrder }} 课 · {{ selectedLesson.title }}</h2>
      <el-empty v-if="!selectedLesson.tier" description="老师尚未为你设置本课类别" />
      <el-empty v-else-if="!selectedLesson.materials.length" description="本课暂未发布该类别的内容" />
      <div v-for="material in selectedLesson.materials" :key="material.id" class="material">
        <strong>{{ material.kind === 'VIDEO' ? '讲解视频' : '课后作业' }} · {{ material.title }}</strong>
        <p v-if="material.kind === 'HOMEWORK'">{{ material.content }}</p>
        <el-button v-else type="primary" link @click="play(material.id)">播放视频</el-button>
      </div>
    </div>
    <el-dialog v-model="playerVisible" title="课后讲解视频" width="min(90vw, 900px)" @closed="closePlayer">
      <video v-if="videoUrl" :src="videoUrl" controls autoplay class="player" />
      <p v-else>视频加载中…</p>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { tierNames, myClassesApi, myLessonsApi, videoStreamUrl } from '@/api/learning'
import type { MyClass, MyLesson } from '@/api/learning'

const classes = ref<MyClass[]>([]), lessons = ref<MyLesson[]>([])
const selectedClassId = ref<number>(), selectedLessonId = ref<number>(), playerVisible = ref(false), videoUrl = ref('')
const selectedLesson = computed(() => lessons.value.find(item => item.id === selectedLessonId.value))
let playRequest = 0
async function loadLessons() { closePlayer(); playerVisible.value = false; lessons.value = selectedClassId.value ? await myLessonsApi(selectedClassId.value) : []; selectedLessonId.value = undefined }
async function play(id: number) {
  closePlayer(); const requestId = playRequest; playerVisible.value = true
  try { const url = await videoStreamUrl(id); if (requestId === playRequest && playerVisible.value) videoUrl.value = url }
  catch { if (requestId === playRequest) { playerVisible.value = false; ElMessage.error('视频无权查看或加载失败') } }
}
function closePlayer() { playRequest++; videoUrl.value = '' }
onMounted(async () => { classes.value = await myClassesApi(); selectedClassId.value = classes.value[0]?.classId; await loadLessons() })
</script>

<style scoped>
.block { padding: 20px; margin-top: 18px; }
.block h2 { margin: 0 0 14px; font-size: 18px; }
.material { padding: 14px 0; border-top: 1px solid var(--border-color); }
.material p { white-space: pre-wrap; }
.player { width: 100%; max-height: 70vh; }
.folders { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 14px; margin-top: 18px; }
.folder { display: flex; flex-direction: column; align-items: flex-start; gap: 6px; padding: 20px; text-align: left; cursor: pointer; color: var(--text-primary); border: 1px solid var(--border-color); }
.folder:hover, .folder.active { border-color: var(--color-primary); }
.folder-icon { font-size: 30px; }
.folder small { color: var(--text-muted); }
</style>
