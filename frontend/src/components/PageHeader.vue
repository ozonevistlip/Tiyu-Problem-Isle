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
  padding: 4px 0 6px;
}

h1 {
  margin: 0;
  color: var(--text-primary);
  font-size: clamp(24px, 3vw, 32px);
  line-height: 1.15;
}

p {
  margin: 6px 0 0;
  color: var(--text-muted);
  line-height: 1.6;
}

.page-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

@media (max-width: 680px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .page-actions {
    justify-content: flex-start;
  }
}
</style>
