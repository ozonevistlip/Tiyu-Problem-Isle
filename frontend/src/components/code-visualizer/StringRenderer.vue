<template>
  <section class="string-renderer" :data-runtime-name="name">
    <header><strong>{{ name }}</strong><span>string</span></header>
    <div class="string-scroll"><div class="string-track">
      <div v-if="activeIndex >= 0" class="index-pointer" data-runtime-pointer :style="{ transform: `translateX(${activeIndex * 49}px)` }"><strong>{{ pointerText }}</strong><i /></div>
      <div class="string-cells"><div v-for="(char, index) in value.value.split('')" :key="index" :class="{ 'is-active': index === activeIndex }"><b>{{ char }}</b><small>{{ index }}</small></div></div>
    </div></div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { RuntimeString } from '@/engine/types'
const props = defineProps<{ name: string; value: RuntimeString; activeIndices?: number[]; pointerLabels?: string[] }>()
const activeIndex = computed(() => props.activeIndices?.[0] ?? -1)
const pointerText = computed(() => props.pointerLabels?.join(', ') || '下标')
</script>

<style scoped lang="scss">
.string-renderer { display: grid; gap: 10px; padding: 14px; background: var(--surface-soft); border: 1px solid var(--border-color); border-radius: 16px; } header { display: flex; justify-content: space-between; color: var(--text-primary); } header span { color: var(--text-muted); font-size: 12px; }.string-scroll { overflow-x: auto; padding: 2px; }.string-track { position: relative; width: max-content; padding-top: 38px; }.string-cells { display: flex; gap: 7px; }.string-cells > div { display: grid; gap: 5px; place-items: center; }.string-cells b { display: grid; width: 42px; height: 42px; place-items: center; background: var(--surface-raised); border: 1px solid var(--border-color); border-radius: 10px; transition: transform .25s ease, background .25s ease; }.string-cells small { color: var(--text-muted); font-size: 11px; }.string-cells .is-active b { color: var(--text-inverse); background: linear-gradient(135deg, var(--color-primary), var(--color-accent)); border-color: transparent; transform: translateY(-4px); }.index-pointer { position: absolute; top: 0; left: 0; z-index: 2; display: grid; width: 42px; place-items: center; color: var(--color-accent); transition: transform .38s cubic-bezier(.22, 1, .36, 1); pointer-events: none; }.index-pointer strong { max-width: 64px; overflow: hidden; font-size: 12px; line-height: 18px; text-overflow: ellipsis; white-space: nowrap; }.index-pointer i { width: 0; height: 0; border-top: 9px solid var(--color-accent); border-right: 6px solid transparent; border-left: 6px solid transparent; filter: drop-shadow(0 3px 4px color-mix(in srgb, var(--color-accent), transparent 50%)); }
</style>
