<template>
  <section class="array-renderer" :data-runtime-name="name">
    <header><strong>{{ name }}</strong><span>{{ value.elementType }}[{{ value.length }}]</span></header>
    <div class="array-scroll">
      <div class="array-track">
        <div
          v-if="activeIndex >= 0"
          class="index-pointer"
          data-runtime-pointer
          :style="{ transform: `translateX(${activeIndex * 36}px)` }"
        >
          <strong>{{ pointerText }}</strong><i />
        </div>
        <div class="array-cells">
          <div
            v-for="(item, index) in value.values"
            :key="index"
            class="array-slot"
            :class="{ 'is-active': index === activeIndex }"
            data-runtime-array-slot
            :data-array-index="index"
          >
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
function format(value: RuntimeValue) {
  if (value.kind === 'void') return ''
  if (value.kind === 'char' && value.value === '\0') return '\\0'
  return value.kind === 'array' || value.kind === 'array2d' ? '…' : String(value.value)
}
</script>

<style scoped lang="scss">
.array-renderer { display: grid; gap: 4px; min-width: 0; padding: 6px 8px; background: color-mix(in srgb, var(--surface-card), var(--surface-soft) 42%); border: 1px solid var(--border-color); border-radius: 10px; }
header { display: flex; justify-content: space-between; gap: 10px; color: var(--text-primary); }
header span { color: var(--text-muted); font-size: 14px; font-weight: 700; }
.array-scroll { min-width: 0; overflow-x: auto; }
.array-track { position: relative; width: max-content; padding-top: 21px; }
.array-cells { display: flex; gap: 4px; padding: 1px 0; }
.index-pointer { position: absolute; top: 0; left: 0; z-index: 2; display: grid; width: 32px; place-items: center; color: var(--color-accent); transition: transform .38s cubic-bezier(.22, 1, .36, 1); pointer-events: none; }
.index-pointer strong { max-width: 52px; overflow: hidden; font-size: 10px; line-height: 13px; text-overflow: ellipsis; white-space: nowrap; }
.index-pointer i { width: 0; height: 0; border-top: 6px solid var(--color-accent); border-right: 4px solid transparent; border-left: 4px solid transparent; filter: drop-shadow(0 3px 4px color-mix(in srgb, var(--color-accent), transparent 50%)); }
.array-slot { display: grid; min-width: 32px; gap: 2px; place-items: center; transition: transform .25s ease, filter .25s ease; }
.array-slot b { display: grid; width: 32px; height: 32px; place-items: center; color: var(--text-primary); background: var(--surface-raised); border: 1px solid color-mix(in srgb, var(--color-primary), transparent 62%); border-radius: 7px; }
.array-slot small { color: var(--text-muted); font-size: 9px; }
.array-slot.is-active { transform: translateY(-2px); }
.array-slot.is-active b { color: var(--text-inverse); background: linear-gradient(135deg, var(--color-primary), var(--color-accent)); border-color: transparent; box-shadow: 0 0 0 2px color-mix(in srgb, var(--color-primary), transparent 82%); }
</style>
