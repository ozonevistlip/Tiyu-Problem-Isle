<template>
  <div class="array-visualizer">
    <div class="array-name">{{ arrayName }}</div>
    <div class="array-arrow">→</div>
    <div class="array-cells" :style="{ '--array-length': normalizedLength }">
      <div v-for="(value, index) in normalizedValues" :key="index" class="array-slot">
        <div class="array-cell" :class="{ 'is-filled-default': index >= normalizedInitializedCount }">
          {{ value }}
        </div>
        <div class="array-index">{{ index }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    arrayName: string
    length: number
    values: number[]
    initializedCount?: number
  }>(),
  {
    initializedCount: undefined
  }
)

const normalizedLength = computed(() => Math.max(0, props.length))
const normalizedInitializedCount = computed(() =>
  Math.min(props.initializedCount ?? props.values.length, normalizedLength.value)
)
const normalizedValues = computed(() => {
  const values = props.values.slice(0, normalizedLength.value)
  while (values.length < normalizedLength.value) values.push(0)
  return values
})
</script>

<style scoped lang="scss">
.array-visualizer {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  min-width: 0;
  padding: 18px;
  overflow-x: auto;
  background: linear-gradient(135deg, var(--surface-card), var(--surface-soft));
  border: 1px solid var(--border-color);
  border-radius: 14px;
}

.array-name {
  flex: 0 0 auto;
  min-width: 54px;
  padding: 12px 14px;
  color: var(--color-primary);
  text-align: center;
  background: color-mix(in srgb, var(--color-primary), transparent 88%);
  border: 1px solid color-mix(in srgb, var(--color-primary), transparent 48%);
  border-radius: 12px;
  box-shadow: 0 10px 22px color-mix(in srgb, var(--color-primary), transparent 86%);
  font-size: 18px;
  font-weight: 900;
  line-height: 1;
}

.array-arrow {
  flex: 0 0 auto;
  padding-top: 13px;
  color: var(--color-accent);
  font-size: 22px;
  font-weight: 900;
  line-height: 1;
}

.array-cells {
  display: grid;
  grid-template-columns: repeat(var(--array-length), 54px);
  gap: 10px;
  min-width: max-content;
}

.array-slot {
  display: grid;
  justify-items: center;
  gap: 7px;
}

.array-cell {
  display: grid;
  width: 54px;
  height: 54px;
  place-items: center;
  color: var(--text-primary);
  background: var(--surface-raised);
  border: 1px solid color-mix(in srgb, var(--color-primary), transparent 58%);
  border-radius: 10px;
  box-shadow: 0 8px 18px rgba(40, 67, 112, 0.1);
  font-size: 18px;
  font-weight: 900;
}

.array-cell.is-filled-default {
  color: var(--text-muted);
  background: color-mix(in srgb, var(--surface-soft), var(--surface-card) 40%);
  border-style: dashed;
}

.array-index {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 800;
  line-height: 1;
}
</style>
