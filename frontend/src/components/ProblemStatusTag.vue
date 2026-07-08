<template>
  <el-tag :type="type" effect="light">{{ label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { problemStatusText } from '@/utils/status'

const props = defineProps<{ status?: string }>()
const label = computed(() => (props.status ? problemStatusText[props.status] || props.status : '未尝试'))
const type = computed(() => {
  if (props.status === 'ACCEPTED') return 'success'
  if (props.status === 'PARTIAL') return 'primary'
  if (props.status === 'TRIED') return 'warning'
  return 'info'
})
</script>
