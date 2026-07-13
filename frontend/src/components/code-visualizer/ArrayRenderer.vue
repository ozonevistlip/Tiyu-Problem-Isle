<template>
  <section class="array-renderer" :data-runtime-name="name">
    <header><strong>{{ name }}</strong><span>{{ value.elementType }}[{{ value.length }}]</span></header>
    <div class="array-scroll">
      <div class="array-track">
        <div
          v-if="activeIndex >= 0"
          class="index-pointer"
          data-runtime-pointer
          :style="{ transform: `translateX(${activeIndex * 50}px)` }"
        >
          <strong>{{ pointerText }}</strong><i />
        </div>
        <div class="array-cells">
          <div v-for="(item, index) in value.values" :key="index" class="array-slot" :class="{ 'is-active': index === activeIndex }">
            <b>{{ format(item) }}</b><small>{{ index }}</small>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { RuntimeArray, RuntimeValue } from '@/engine/types'

const props = defineProps<{ name: string; value: RuntimeArray; activeIndices?: number[]; pointerLabels?: string[] }>()
const activeIndex = computed(() => props.activeIndices?.[props.activeIndices.length - 1] ?? -1)
const pointerText = computed(() => props.pointerLabels?.join(', ') || '下标')
function format(value: RuntimeValue) { return value.kind === 'void' ? '' : value.kind === 'array' || value.kind === 'array2d' ? '…' : String(value.value) }
</script>

<style scoped lang="scss">
.array-renderer { display: grid; gap: 10px; min-width: 0; padding: 14px; background: color-mix(in srgb, var(--surface-card), var(--surface-soft) 42%); border: 1px solid var(--border-color); border-radius: 16px; }
header { display: flex; justify-content: space-between; gap: 10px; color: var(--text-primary); }
header span { color: var(--text-muted); font-size: 12px; }
.array-scroll { min-width: 0; overflow-x: auto; padding: 2px; }
.array-track { position: relative; width: max-content; padding-top: 38px; }
.array-cells { display: flex; gap: 7px; padding: 6px 0; }
.index-pointer { position: absolute; top: 0; left: 0; z-index: 2; display: grid; width: 43px; place-items: center; color: var(--color-accent); transition: transform .38s cubic-bezier(.22, 1, .36, 1); pointer-events: none; }
.index-pointer strong { max-width: 64px; overflow: hidden; font-size: 12px; line-height: 18px; text-overflow: ellipsis; white-space: nowrap; }
.index-pointer i { width: 0; height: 0; border-top: 9px solid var(--color-accent); border-right: 6px solid transparent; border-left: 6px solid transparent; filter: drop-shadow(0 3px 4px color-mix(in srgb, var(--color-accent), transparent 50%)); }
.array-slot { display: grid; min-width: 43px; gap: 5px; place-items: center; transition: transform .25s ease, filter .25s ease; }
.array-slot b { display: grid; width: 43px; height: 43px; place-items: center; color: var(--text-primary); background: var(--surface-raised); border: 1px solid color-mix(in srgb, var(--color-primary), transparent 62%); border-radius: 10px; }
.array-slot small { color: var(--text-muted); font-size: 11px; }
.array-slot.is-active { transform: translateY(-5px); }
.array-slot.is-active b { color: var(--text-inverse); background: linear-gradient(135deg, var(--color-primary), var(--color-accent)); border-color: transparent; box-shadow: 0 0 0 4px color-mix(in srgb, var(--color-primary), transparent 82%); }
</style>
