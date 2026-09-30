<template>
  <transition-group name="notice" tag="div" class="notice-stack">
    <el-alert
      v-for="item in visibleItems"
      :key="item.id"
      :title="item.title"
      :description="item.content"
      type="warning"
      show-icon
      @close="dismiss(item.id)"
    />
  </transition-group>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { publicAnnouncementsApi } from '@/api/public'
import type { Announcement } from '@/api/types'

const items = ref<Announcement[]>([])
const dismissed = ref<number[]>(readDismissed())
const visibleItems = computed(() => items.value.filter(item => !dismissed.value.includes(item.id)).slice(0, 3))
function dismiss(id: number) {
  dismissed.value.push(id)
  sessionStorage.setItem('cppkid_dismissed_announcements', JSON.stringify(dismissed.value))
}
onMounted(async () => { try { items.value = await publicAnnouncementsApi() } catch { /* announcements must not block the app */ } })

function readDismissed(): number[] {
  try {
    const value = JSON.parse(sessionStorage.getItem('cppkid_dismissed_announcements') || '[]')
    return Array.isArray(value) ? value.filter(Number.isInteger) : []
  } catch { return [] }
}
</script>

<style scoped>
.notice-stack { position: fixed; z-index: 3000; top: 86px; right: 22px; display: grid; width: min(420px, calc(100vw - 32px)); gap: 10px; pointer-events: none; }
.notice-stack :deep(.el-alert) { align-items: flex-start; pointer-events: auto; border: 1px solid color-mix(in srgb, var(--color-warning), transparent 55%); border-radius: 14px; box-shadow: var(--shadow-hover); }
.notice-stack :deep(.el-alert__description) { max-height: 96px; overflow: auto; white-space: pre-wrap; line-height: 1.55; }
.notice-enter-active, .notice-leave-active { transition: .25s ease; }.notice-enter-from, .notice-leave-to { opacity: 0; transform: translateX(20px); }
@media (max-width: 620px) { .notice-stack { top: 12px; right: 16px; } }
</style>
