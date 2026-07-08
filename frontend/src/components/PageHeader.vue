<template>
  <div class="page-header">
    <div>
      <el-button v-if="back" text :icon="ArrowLeft" @click="$router.back()">返回</el-button>
      <h1>{{ title }}</h1>
      <p v-if="subtitle">{{ subtitle }}</p>
    </div>
    <div class="page-actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue'

defineProps<{
  title: string
  subtitle?: string
  back?: boolean
}>()
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  gap: 16px;
  align-items: flex-end;
  justify-content: space-between;
  min-height: 58px;
}

h1 {
  margin: 0;
  font-size: 26px;
}

p {
  margin: 6px 0 0;
  color: #667085;
}

.page-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}
</style>
