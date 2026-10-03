<template><img v-if="url" :src="url" :alt="alt" class="pet-image" /><div v-else class="pet-image pet-image--empty">{{ failed ? '图片加载失败' : '加载中…' }}</div></template>

<script setup lang="ts">
import { onUnmounted, ref, watch } from 'vue'
import { petMediaUrl } from '@/api/pets'

const props = defineProps<{ petId: number; kind: 'preview' | 'atlas'; alt: string }>()
const url = ref('')
const failed = ref(false)
let generation = 0
watch(() => [props.petId, props.kind], async () => {
  const current = ++generation
  if (url.value) URL.revokeObjectURL(url.value)
  url.value = ''; failed.value = false
  try {
    const next = await petMediaUrl(props.petId, props.kind)
    if (current !== generation) URL.revokeObjectURL(next)
    else url.value = next
  } catch { if (current === generation) failed.value = true }
}, { immediate: true })
onUnmounted(() => { generation++; if (url.value) URL.revokeObjectURL(url.value) })
</script>

<style scoped>
.pet-image { width: 100%; height: 100%; object-fit: contain; }
.pet-image--empty { display: grid; place-items: center; color: var(--text-muted); }
</style>
