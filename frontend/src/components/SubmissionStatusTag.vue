<template>
  <el-tag :type="type" effect="light">{{ submissionStatusText[status] || status }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { submissionStatusText } from '@/utils/status'

const props = defineProps<{ status: string }>()
const type = computed(() => {
  if (props.status === 'ACCEPTED') return 'success'
  if (props.status === 'JUDGING') return 'primary'
  if (props.status === 'PENDING') return 'info'
  if (props.status === 'COMPILE_ERROR') return 'warning'
  return 'danger'
})
</script>
