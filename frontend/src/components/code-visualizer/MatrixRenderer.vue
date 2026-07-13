<template>
  <section class="matrix-renderer" :data-runtime-name="name">
    <header><strong>{{ name }}</strong><span>{{ value.elementType }}[{{ value.rows }}][{{ value.columns }}]</span></header>
    <div class="matrix-wrap"><div class="matrix" :style="{ '--columns': value.columns }">
      <template v-for="(row, rowIndex) in value.values" :key="rowIndex">
        <b v-for="(cell, columnIndex) in row" :key="`${rowIndex}-${columnIndex}`" :class="{ 'is-active': rowIndex === activeRow && columnIndex === activeColumn }">
          <span v-if="rowIndex === activeRow && columnIndex === activeColumn" class="index-pointer" data-runtime-pointer>{{ pointerText }}<i /></span>
          {{ format(cell) }}<small>{{ rowIndex }},{{ columnIndex }}</small>
        </b>
      </template>
    </div></div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { RuntimeArray2D, RuntimeValue } from '@/engine/types'
const props = defineProps<{ name: string; value: RuntimeArray2D; activeIndices?: number[]; pointerLabels?: string[] }>()
const activeRow = computed(() => props.activeIndices?.[0] ?? -1)
const activeColumn = computed(() => props.activeIndices?.[1] ?? -1)
const pointerText = computed(() => props.pointerLabels?.join(', ') || '下标')
function format(value: RuntimeValue) { return value.kind === 'void' ? '' : value.kind === 'array' || value.kind === 'array2d' ? '…' : String(value.value) }
</script>

<style scoped lang="scss">
.matrix-renderer { display: grid; gap: 10px; padding: 14px; background: var(--surface-soft); border: 1px solid var(--border-color); border-radius: 16px; }
header { display: flex; justify-content: space-between; gap: 10px; color: var(--text-primary); }
header span { color: var(--text-muted); font-size: 12px; }
.matrix-wrap { overflow: auto; padding-top: 26px; }.matrix { display: grid; grid-template-columns: repeat(var(--columns), 54px); gap: 6px; width: max-content; }
.matrix b { position: relative; display: grid; width: 54px; height: 48px; place-items: center; color: var(--text-primary); background: var(--surface-raised); border: 1px solid var(--border-color); border-radius: 9px; font-size: 14px; transition: transform .25s ease, background .25s ease; }
.matrix small { display: block; color: var(--text-muted); font-size: 9px; font-weight: 500; }.matrix b.is-active { color: var(--text-inverse); background: linear-gradient(135deg, var(--color-primary), var(--color-accent)); border-color: transparent; transform: scale(1.07); }
.index-pointer { position: absolute; top: -28px; left: 50%; display: grid; min-width: 38px; justify-items: center; color: var(--color-accent); font-size: 11px; font-style: normal; transform: translateX(-50%); white-space: nowrap; pointer-events: none; }.index-pointer i { width: 0; height: 0; border-top: 8px solid var(--color-accent); border-right: 5px solid transparent; border-left: 5px solid transparent; }
</style>
