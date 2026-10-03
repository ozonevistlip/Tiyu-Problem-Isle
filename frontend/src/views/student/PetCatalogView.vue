<template>
  <div class="page">
    <PageHeader title="宠物伙伴" :subtitle="user.role === 'teacher' ? '老师可以使用全部已发布宠物' : '看看所有宠物，老师发放后就能与它互动和聊天'" />
    <div v-loading="catalog.loading" class="pet-grid">
      <article v-for="pet in catalog.pets" :key="pet.id" class="panel pet-card">
        <div class="pet-preview"><PetImage v-if="pet.hasPreview" :pet-id="pet.id" kind="preview" :alt="pet.name" /><span v-else>暂无预览图</span></div>
        <h2>{{ pet.name }}</h2><p>{{ pet.description }}</p>
        <div class="pet-actions" v-if="pet.owned">
          <el-button :type="catalog.selectedId === pet.id ? 'success' : 'primary'" :disabled="catalog.selectedId === pet.id" @click="catalog.select(pet.id)">{{ catalog.selectedId === pet.id ? '正在陪伴' : '设为悬浮宠物' }}</el-button>
          <el-button @click="openChat(pet)">聊天</el-button>
        </div>
        <el-tag v-else type="info">等待老师发放</el-tag>
      </article>
      <p v-if="!catalog.loading && !catalog.pets.length">还没有已发布的宠物。</p>
    </div>
    <PetChatDialog v-if="chatPet" v-model="chatOpen" :pet-id="chatPet.id" :pet-name="chatPet.name" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import PetImage from '@/components/pet/PetImage.vue'
import PetChatDialog from '@/components/pet/PetChatDialog.vue'
import { usePetCatalogStore } from '@/stores/petCatalog'
import { useUserStore } from '@/stores/user'
import type { PetInfo } from '@/api/pets'

const catalog = usePetCatalogStore()
const user = useUserStore()
const chatPet = ref<PetInfo | null>(null), chatOpen = ref(false)
function openChat(pet: PetInfo) { chatPet.value = pet; chatOpen.value = true }
watch(() => catalog.pets, pets => {
  if (chatPet.value && !pets.some(pet => pet.id === chatPet.value?.id && pet.owned)) chatOpen.value = false
})
onMounted(() => { void catalog.refresh().catch(() => undefined) })
</script>

<style scoped>
.pet-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(230px, 1fr)); gap: 18px; margin-top: 18px; }
.pet-card { padding: 18px; }
.pet-card h2 { margin: 12px 0 4px; font-size: 19px; }
.pet-card p { min-height: 48px; color: var(--text-muted); }
.pet-preview { height: 200px; display: grid; place-items: center; background: var(--surface-soft); border-radius: 14px; overflow: hidden; }
.pet-actions { display: flex; flex-wrap: wrap; gap: 8px; }
.pet-actions .el-button { margin: 0; }
</style>
