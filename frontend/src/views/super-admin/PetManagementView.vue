<template>
  <div class="page">
    <PageHeader title="宠物管理" subtitle="统一上传宠物，检查图片和动画后发布给老师">
      <template #actions><el-button type="primary" @click="edit()">新增宠物</el-button></template>
    </PageHeader>
    <el-table v-loading="loading" :data="pets" class="panel">
      <el-table-column label="预览" width="96"><template #default="{ row }"><div class="thumb"><PetImage v-if="row.hasPreview" :pet-id="row.id" kind="preview" :alt="row.name" /></div></template></el-table-column>
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="description" label="简介" show-overflow-tooltip />
      <el-table-column label="资源" width="180"><template #default="{ row }">{{ row.hasPreview ? '预览图 ✓' : '缺预览图' }} · {{ row.hasAnimation ? '动画 ✓' : '缺动画' }}</template></el-table-column>
      <el-table-column label="状态" width="90"><template #default="{ row }">{{ row.published ? '已发布' : '草稿' }}</template></el-table-column>
      <el-table-column label="操作" width="260"><template #default="{ row }">
        <el-button link @click="edit(row)">编辑与上传</el-button>
        <el-button link @click="preview(row)">预览</el-button>
        <el-button link :type="row.published ? 'warning' : 'primary'" @click="toggle(row)">{{ row.published ? '撤回' : '发布' }}</el-button>
      </template></el-table-column>
    </el-table>
    <el-dialog v-model="editorOpen" :title="form.id ? '编辑宠物' : '新增宠物'" width="min(600px, 94vw)">
      <el-form label-position="top">
        <el-form-item label="名称"><el-input v-model.trim="form.name" maxlength="80" /></el-form-item>
        <el-form-item label="简介"><el-input v-model.trim="form.description" maxlength="500" /></el-form-item>
        <el-form-item label="性格提示词"><el-input v-model="form.personalityPrompt" type="textarea" :rows="5" maxlength="5000" show-word-limit placeholder="描述宠物的性格、说话风格及学习引导方式" /></el-form-item>
        <el-form-item label="静态预览图（WebP，最大 8MB，宽高不超过 2048）"><input type="file" accept="image/webp,.webp" @change="chooseFile($event, 'preview')" /><el-button :disabled="!form.id || !previewFile" :loading="uploading" @click="upload('preview')">单独上传预览图</el-button></el-form-item>
        <el-form-item label="动画图集（静态 WebP，1536×1872，8 列×9 行）"><input type="file" accept="image/webp,.webp" @change="chooseFile($event, 'atlas')" /><el-button :disabled="!form.id || !atlasFile" :loading="uploading" @click="upload('atlas')">单独上传动画</el-button></el-form-item>
        <p v-if="currentPet" class="muted">已上传：预览图 {{ currentPet.hasPreview ? '✓' : '—' }} · 动画图集 {{ currentPet.hasAnimation ? '✓' : '—' }}</p>
      </el-form>
      <template #footer><el-button @click="editorOpen = false">关闭</el-button><el-button type="primary" :loading="saving" @click="save">{{ previewFile || atlasFile ? '保存资料并上传图片' : '保存资料' }}</el-button></template>
    </el-dialog>
    <el-dialog v-model="previewOpen" :title="`预览 · ${previewPet?.name || ''}`" width="min(560px, 94vw)">
      <div v-if="previewPet" class="preview-content">
        <div class="preview-picture"><PetImage v-if="previewPet.hasPreview" :pet-id="previewPet.id" kind="preview" :alt="previewPet.name" /></div>
        <p>{{ previewPet.description }}</p>
        <div v-if="previewPet.hasAnimation" class="animation-preview"><PetCanvas :key="previewPet.id" :pet-id="previewPet.id" state="idle" :low-performance="false" :reduced-motion="false" /></div>
        <p v-else class="muted">尚未上传动画资源。</p>
        <details><summary>查看性格提示词</summary><pre>{{ previewPet.personalityPrompt }}</pre></details>
        <el-button @click="chatPreview">试聊宠物</el-button>
      </div>
    </el-dialog>
    <PetChatDialog v-if="previewPet" v-model="chatOpen" :pet-id="previewPet.id" :pet-name="previewPet.name" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import PetImage from '@/components/pet/PetImage.vue'
import PetCanvas from '@/components/pet/PetCanvas.vue'
import PetChatDialog from '@/components/pet/PetChatDialog.vue'
import { createPetApi, listAdminPetsApi, publishPetApi, updatePetApi, uploadPetAssetApi, type AdminPetInfo } from '@/api/pets'

const pets = ref<AdminPetInfo[]>([])
const loading = ref(false), uploading = ref(false), saving = ref(false), editorOpen = ref(false), previewOpen = ref(false), chatOpen = ref(false)
const previewPet = ref<AdminPetInfo | null>(null)
const previewFile = ref<File>(), atlasFile = ref<File>()
const form = reactive({ id: 0, name: '', description: '', personalityPrompt: '' })
const currentPet = computed(() => pets.value.find(pet => pet.id === form.id))
async function load() { loading.value = true; try { pets.value = await listAdminPetsApi() } finally { loading.value = false } }
function edit(pet?: AdminPetInfo) {
  Object.assign(form, { id: pet?.id || 0, name: pet?.name || '', description: pet?.description || '', personalityPrompt: pet?.personalityPrompt || '' })
  previewFile.value = undefined; atlasFile.value = undefined; editorOpen.value = true
}
async function save() {
  if (!form.name.trim() || !form.personalityPrompt.trim()) return ElMessage.warning('请填写名称和性格提示词')
  saving.value = true
  let metadataSaved = false
  try {
    const result = form.id ? await updatePetApi(form.id, form) : await createPetApi(form)
    form.id = result.id
    metadataSaved = true
    let uploaded = 0
    if (previewFile.value) {
      await uploadPetAssetApi(form.id, 'preview', previewFile.value)
      previewFile.value = undefined
      uploaded++
    }
    if (atlasFile.value) {
      await uploadPetAssetApi(form.id, 'atlas', atlasFile.value)
      atlasFile.value = undefined
      uploaded++
    }
    await load()
    ElMessage.success(uploaded ? `资料已保存，${uploaded} 张图片已上传` : '宠物资料已保存')
  } catch {
    if (metadataSaved) {
      await load().catch(() => undefined)
      ElMessage.warning('文字资料已保存，但图片上传未完成，请重试')
    }
  } finally { saving.value = false }
}
async function chooseFile(event: Event, kind: 'preview' | 'atlas') {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (kind === 'preview') previewFile.value = undefined
  else atlasFile.value = undefined
  if (!file) return
  if (!file.name.toLowerCase().endsWith('.webp') || (file.type && file.type !== 'image/webp')) {
    ElMessage.warning('请选择 WebP 图片'); input.value = ''; return
  }
  if (file.size === 0 || file.size > 8 * 1024 * 1024) {
    ElMessage.warning('图片必须小于 8MB'); input.value = ''; return
  }
  try {
    const bitmap = await createImageBitmap(file)
    const valid = kind === 'atlas'
      ? bitmap.width === 1536 && bitmap.height === 1872
      : bitmap.width <= 2048 && bitmap.height <= 2048
    bitmap.close()
    if (!valid) {
      ElMessage.warning(kind === 'atlas' ? '动画图集必须为 1536×1872 像素' : '预览图宽高不能超过 2048 像素')
      input.value = ''; return
    }
    const webpFile = file.type ? file : new File([file], file.name, { type: 'image/webp' })
    if (kind === 'preview') previewFile.value = webpFile
    else atlasFile.value = webpFile
  } catch {
    ElMessage.warning('WebP 图片无法读取'); input.value = ''
  }
}
async function upload(kind: 'preview' | 'atlas') {
  const file = kind === 'preview' ? previewFile.value : atlasFile.value
  if (!form.id || !file) return
  uploading.value = true
  try {
    await uploadPetAssetApi(form.id, kind, file)
    if (kind === 'preview') previewFile.value = undefined
    else atlasFile.value = undefined
    await load(); ElMessage.success('图片已上传')
  } finally { uploading.value = false }
}
function preview(pet: AdminPetInfo) { previewPet.value = pet; previewOpen.value = true }
function chatPreview() { previewOpen.value = false; chatOpen.value = true }
async function toggle(pet: AdminPetInfo) {
  await ElMessageBox.confirm(`${pet.published ? '撤回' : '发布'}“${pet.name}”？`, '确认操作')
  await publishPetApi(pet.id, !pet.published); await load()
}
onMounted(load)
</script>

<style scoped>
.thumb { width: 68px; height: 68px; }
.preview-content { display: grid; gap: 12px; }
.preview-picture { height: 240px; }
.preview-content pre { white-space: pre-wrap; overflow-wrap: anywhere; }
.animation-preview { width: 240px; height: 180px; margin: 0 auto; }
.muted { color: var(--text-muted); }
</style>
