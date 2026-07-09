<template>
  <el-tag :type="tagType" effect="dark">{{ label }}</el-tag>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { contestPhase, formatDuration, secondsUntil } from '@/utils/time'

const props = defineProps<{ startTime: string; endTime: string }>()
const now = ref(Date.now())
let timer = 0

const phase = computed(() => contestPhase(props.startTime, props.endTime))
const label = computed(() => {
  if (phase.value === 'upcoming') return `准备出发 ${formatDuration(secondsUntil(props.startTime))}`
  if (phase.value === 'running') return `闯关中 ${formatDuration(secondsUntil(props.endTime))}`
  return '已完成挑战'
})
const tagType = computed(() => (phase.value === 'running' ? 'success' : phase.value === 'upcoming' ? 'warning' : 'info'))

onMounted(() => {
  timer = window.setInterval(() => {
    now.value = Date.now()
  }, 1000)
})
onUnmounted(() => window.clearInterval(timer))
</script>
