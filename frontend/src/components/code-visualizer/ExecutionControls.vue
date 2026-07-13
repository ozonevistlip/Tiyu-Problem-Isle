<template>
  <footer class="execution-controls">
    <div class="control-buttons">
      <el-button circle :icon="DArrowLeft" title="从头开始" @click="emit('reset')" />
      <el-button circle :icon="ArrowLeft" :disabled="props.atStart" title="上一步" @click="emit('previous')" />
      <el-button v-if="!props.playing" type="primary" :icon="VideoPlay" @click="emit('play')">播放</el-button>
      <el-button v-else type="warning" :icon="VideoPause" @click="emit('pause')">暂停</el-button>
      <el-button circle :icon="ArrowRight" :disabled="props.atEnd" title="下一步" @click="emit('next')" />
    </div>
    <div class="control-progress"><div><span>步骤 <b>{{ currentStep }}</b> / {{ props.total }}</span><small>拖动进度条定位</small></div><el-slider :model-value="sliderIndex" :min="0" :max="lastIndex" :step="1" :disabled="props.total <= 1" :format-tooltip="formatStep" @update:model-value="seek" /></div>
    <div class="control-speed"><span>速度</span><el-slider :model-value="props.speed" :min="0.5" :max="3" :step="0.25" :show-tooltip="false" @update:model-value="updateSpeed" /><b>{{ props.speed }}×</b></div>
  </footer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ArrowLeft, ArrowRight, DArrowLeft, VideoPause, VideoPlay } from '@element-plus/icons-vue'
const props = defineProps<{ index: number; total: number; playing: boolean; speed: number; atStart: boolean; atEnd: boolean }>()
const emit = defineEmits<{ reset: []; previous: []; play: []; pause: []; next: []; seek: [value: number]; 'update:speed': [value: number] }>()
const currentStep = computed(() => props.total ? props.index + 1 : 0)
const lastIndex = computed(() => Math.max(0, props.total - 1))
const sliderIndex = computed(() => Math.min(lastIndex.value, Math.max(0, props.index)))
function formatStep(value: number) { return `第 ${value + 1} 步` }
function seek(value: number | number[]) { if (typeof value === 'number') emit('seek', value) }
function updateSpeed(value: number | number[]) { if (typeof value === 'number') emit('update:speed', value) }
</script>

<style scoped lang="scss">
.execution-controls { display: grid; grid-template-columns: auto minmax(220px, 1fr) minmax(170px, 230px); gap: 18px; align-items: center; padding: 12px 16px; background: color-mix(in srgb, var(--surface-glass), var(--surface-card) 55%); border: 1px solid var(--border-soft); border-radius: 16px; box-shadow: var(--shadow-hover); backdrop-filter: blur(14px); }.control-buttons, .control-speed { display: flex; gap: 8px; align-items: center; }.control-progress { display: grid; gap: 4px; color: var(--text-muted); font-size: 12px; font-weight: 700; }.control-progress > div { display: flex; justify-content: space-between; gap: 12px; }.control-progress small { color: var(--text-muted); font-size: 10px; font-weight: 500; }.control-progress b { color: var(--color-primary); }.control-progress :deep(.el-slider) { --el-slider-main-bg-color: var(--color-primary); margin: 0 8px; }.control-speed { color: var(--text-muted); font-size: 12px; font-weight: 700; }.control-speed .el-slider { width: 120px; }.control-speed b { color: var(--color-primary); white-space: nowrap; }
@media (max-width: 760px) { .execution-controls { grid-template-columns: 1fr; }.control-progress { order: 3; }.control-speed { justify-content: space-between; } }
</style>
