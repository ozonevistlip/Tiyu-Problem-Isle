<template>
  <div class="variable-list">
    <article v-for="item in variables" :key="`${item.scopeId}-${item.name}`" class="variable-item" :class="{ 'is-changed': item.changed, 'is-read': item.read }" :data-runtime-name="item.name">
      <div><span>{{ item.name }}</span><small>{{ item.dataType }}</small></div>
      <strong><i v-if="item.previousValue">{{ format(item.previousValue) }} → </i>{{ format(item.value) }}</strong>
      <em>{{ item.scopeName }}</em>
    </article>
    <p v-if="!variables.length" class="empty-note">执行到变量声明后，这里会出现它们的值。</p>
  </div>
</template>

<script setup lang="ts">
import type { RuntimeValue, RuntimeVariable } from '@/engine/types'

defineProps<{ variables: RuntimeVariable[] }>()

function format(value: RuntimeValue): string {
  if (value.kind === 'array') return `[${value.values.map(format).join(', ')}]`
  if (value.kind === 'array2d') return '二维数组'
  if (value.kind === 'void') return 'void'
  return String(value.value)
}
</script>

<style scoped lang="scss">
.variable-list { display: grid; grid-template-columns: repeat(auto-fit, minmax(118px, 1fr)); gap: 10px; }
.variable-item { display: grid; gap: 7px; padding: 11px; background: var(--surface-soft); border: 1px solid var(--border-color); border-radius: 12px; transition: transform .24s ease, border-color .24s ease, box-shadow .24s ease; }
.variable-item.is-changed { border-color: var(--color-primary); box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-primary), transparent 85%); }
.variable-item.is-read { border-color: var(--color-accent); }
.variable-item > div { display: flex; justify-content: space-between; gap: 6px; color: var(--text-primary); font-size: 12px; font-weight: 800; }
small, em { color: var(--text-muted); font-size: 10px; font-style: normal; }
strong { color: var(--color-primary); font-size: 19px; line-height: 1; word-break: break-word; }
strong i { color: var(--text-muted); font-size: 12px; font-style: normal; text-decoration: line-through; }
.empty-note { grid-column: 1 / -1; margin: 0; padding: 16px; color: var(--text-muted); text-align: center; border: 1px dashed var(--border-color); border-radius: 12px; font-size: 12px; }
</style>
