<template>
  <div ref="host" class="pet-canvas" @contextmenu.prevent="emit('openMenu')" />
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import petAssetUrl from '@/assets/pet/winged-kuriboh-spritesheet.webp'
import { PetRenderer } from '@/pet/PetRenderer'
import type { PetDragFrame, PetPointerStart, PetState } from '@/pet/petTypes'

const props = defineProps<{
  state: PetState
  lowPerformance: boolean
  reducedMotion: boolean
}>()

const emit = defineEmits<{
  ready: []
  animationComplete: [state: PetState]
  pointerDown: [event: PetPointerStart]
  openMenu: []
}>()

const host = ref<HTMLElement | null>(null)
let renderer: PetRenderer | null = null

watch(() => props.state, (state) => renderer?.play(state))
watch(() => props.lowPerformance, (value) => renderer?.setLowPerformance(value))
watch(() => props.reducedMotion, (value) => renderer?.setReducedMotion(value))

onMounted(async () => {
  if (!host.value) return
  const instance = new PetRenderer({
    onAnimationComplete: (state) => emit('animationComplete', state),
    onPointerDown: (event) => emit('pointerDown', event)
  })
  renderer = instance
  await instance.init(host.value, petAssetUrl)
  if (renderer !== instance) {
    instance.destroy()
    return
  }
  instance.setLowPerformance(props.lowPerformance)
  instance.setReducedMotion(props.reducedMotion)
  instance.play(props.state)
  emit('ready')
})

onUnmounted(() => {
  renderer?.destroy()
  renderer = null
})

defineExpose({
  setMotionFrame: (frame: PetDragFrame) => renderer?.setMotionFrame(frame),
  getFacing: () => renderer?.getFacing() ?? 1,
  setTickHandler: (handler: ((deltaMs: number) => void) | null) => renderer?.setTickHandler(handler),
  pause: () => renderer?.pause(),
  resume: () => renderer?.resume()
})
</script>

<style scoped>
.pet-canvas {
  width: 100%;
  height: 100%;
  touch-action: none;
  user-select: none;
}

:deep(.pet-pixi-canvas) {
  display: block;
  width: 100%;
  height: 100%;
  cursor: grab;
  image-rendering: pixelated;
}

:deep(.pet-pixi-canvas:active) {
  cursor: grabbing;
}
</style>
