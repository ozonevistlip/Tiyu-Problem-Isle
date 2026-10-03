<template>
  <Teleport to="body">
    <PetBubble
      :visible="store.visible && store.bubbleVisible && !store.isDragging"
      :message="store.bubbleMessage"
      :type="store.bubbleType"
      :anchor="store.position"
      :pet-width="dimensions.width"
      :pet-height="dimensions.height"
      :action-label="store.bubbleActionLabel"
      @close="store.hideBubble"
      @action="handleBubbleAction"
    />

    <PetControlMenu
      :open="store.visible && menuOpen"
      :anchor="store.position"
      :pet-width="dimensions.width"
      :muted="store.muted"
      :auto-behavior-enabled="store.autoBehaviorEnabled"
      :low-performance-mode="store.lowPerformanceMode"
      :pet-name="petName"
      :can-chat="Boolean(petId)"
      @close="menuOpen = false"
      @toggle-muted="store.toggleMuted()"
      @toggle-auto="store.toggleAutoBehavior()"
      @toggle-low-performance="store.toggleLowPerformanceMode()"
      @reset="resetPosition"
      @chat="openChat"
      @hide="hidePet"
    />

    <div
      v-if="store.visible"
      ref="widget"
      class="pet-widget"
      :class="{ 'is-dragging': store.isDragging, 'is-snapping': snapping, 'is-ready': ready }"
      :style="widgetSizeStyle"
      aria-label="互动学习宠物。拖动可以移动，点击会和你互动，右键打开设置。"
      @mouseenter="hovered = true"
      @mouseleave="hovered = false"
    >
      <PetCanvas
        ref="canvas"
        :key="petId || 'preview'"
        :pet-id="petId"
        :state="store.currentState"
        :low-performance="store.lowPerformanceMode"
        :reduced-motion="reducedMotion"
        @ready="handleReady"
        @pointer-down="handlePointerDown"
        @animation-complete="handleAnimationComplete"
        @open-menu="openMenu"
      />
      <button
        v-show="hovered || menuOpen"
        type="button"
        class="pet-widget__settings"
        aria-label="打开宠物设置"
        @pointerdown.stop
        @click.stop="openMenu"
      >
        <el-icon><Setting /></el-icon>
      </button>
    </div>

    <aside v-if="props.debug && store.visible" class="pet-debug" data-pet-exclusion="pet-debug">
      <strong>宠物运动调试</strong>
      <span>状态：{{ store.currentState }}</span>
      <span>平滑速度：{{ Math.round(debugMetrics.speed) }} px/s</span>
      <span>释放速度：{{ Math.round(debugMetrics.releaseSpeed) }} px/s</span>
      <span>朝向：{{ debugMetrics.facing > 0 ? '右' : '左' }}</span>
      <span>性能：{{ store.lowPerformanceMode ? '低性能' : '完整' }} / {{ reducedMotion ? '减少动态' : '正常动态' }}</span>
    </aside>

    <button v-if="!store.visible" type="button" class="pet-return" @click="showPet">
      <el-icon><Opportunity /></el-icon>
      <span>唤回学习伙伴</span>
    </button>
    <PetChatDialog v-if="petId && petName" v-model="chatOpen" :pet-id="petId" :pet-name="petName" />
  </Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { Opportunity, Setting } from '@element-plus/icons-vue'
import { usePetStore } from '@/stores/petStore'
import { PetDragController } from '@/pet/PetDragController'
import { emitPetEvent, onPetEvent } from '@/pet/PetEventBus'
import { PetMovementController } from '@/pet/PetMovementController'
import { PetSafeAreaManager } from '@/pet/PetSafeAreaManager'
import { PetStateMachine } from '@/pet/PetStateMachine'
import {
  PET_AUTO_BEHAVIOR_MAX_INTERVAL,
  PET_AUTO_BEHAVIOR_MIN_INTERVAL,
  PET_SLEEP_AFTER,
  getPetDimensions
} from '@/pet/petConfig'
import { petDialogues, pickPetDialogue } from '@/pet/petDialogues'
import type { PetBusinessEvent, PetDragFrame, PetPointerStart, PetPosition, PetState } from '@/pet/petTypes'
import PetBubble from './PetBubble.vue'
import PetCanvas from './PetCanvas.vue'
import PetControlMenu from './PetControlMenu.vue'
import PetChatDialog from './PetChatDialog.vue'

const props = withDefaults(defineProps<{ debug?: boolean; petId?: number; petName?: string }>(), { debug: false })

interface PetCanvasExpose {
  setMotionFrame: (frame: PetDragFrame) => void
  getFacing: () => -1 | 1
  setTickHandler: (handler: ((deltaMs: number) => void) | null) => void
  pause: () => void
  resume: () => void
}

const store = usePetStore()
store.initialize()

const widget = ref<HTMLElement | null>(null)
const canvas = ref<PetCanvasExpose | null>(null)
const dimensions = reactive(getPetDimensions())
const menuOpen = ref(false)
const chatOpen = ref(false)
const hovered = ref(false)
const ready = ref(false)
const snapping = ref(false)
const motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
const reducedMotion = ref(motionQuery.matches)
const widgetSizeStyle = computed(() => ({ width: `${dimensions.width}px`, height: `${dimensions.height}px` }))
const debugMetrics = reactive({ speed: 0, releaseSpeed: 0, facing: 1 as -1 | 1 })

let visualPosition: PetPosition = { x: 0, y: 0 }
let resizeFrame = 0
let activityThrottleAt = 0
let autoTimer: number | undefined
let snapTimer: number | undefined
let nextAutoAt = getNextAutoTime()
let previousAutoAction = -1
let lastMotionPosition: PetPosition = { x: 0, y: 0 }
let lastMotionAt = performance.now()
let unmounting = false

const safeArea = new PetSafeAreaManager(() => dimensions)
const stateMachine = new PetStateMachine(store.visible ? 'idle' : 'hidden', {
  onTransition(next) {
    store.setState(next)
    if (['pointerDown', 'dragging', 'released', 'gliding', 'flyingToEdge', 'landing', 'hidden'].includes(next)) store.hideBubble()
  }
})

const movement = new PetMovementController({
  clampPosition: (position) => safeArea.clamp(position),
  resolveTarget: (position) => safeArea.findSafeEdgeTarget(position),
  onPosition(position) {
    if (unmounting) return
    updateVisualPosition(position)
    pushMovementFrame(position)
  },
  onPhase(phase) {
    if (unmounting) return
    stateMachine.request(phase, { force: true })
  },
  onSettled(position) {
    if (unmounting) return
    visualPosition = position
    applyWidgetPosition(position)
    store.updatePosition(position)
    debugMetrics.speed = 0
    stateMachine.request('idle', { force: true })
  }
})
movement.setReducedMotion(reducedMotion.value)

const dragController = new PetDragController({
  canStart: () => store.visible && !['hidden', 'success', 'error'].includes(store.currentState),
  getPosition: () => visualPosition,
  clampPosition: (position) => safeArea.clamp(position),
  onPointerDown() {
    movement.cancel()
    snapping.value = false
    menuOpen.value = false
    store.touch()
    stateMachine.request('pointerDown', { force: true })
  },
  onDragStart(frame) {
    store.setDragging(true)
    stateMachine.request('dragging', { force: true })
    applyDragFrame(frame)
  },
  onDragMove: applyDragFrame,
  onRelease(release) {
    if (unmounting) return
    store.setDragging(false)
    canvas.value?.setMotionFrame(release)
    debugMetrics.speed = release.velocity.speed
    debugMetrics.releaseSpeed = release.velocity.speed
    debugMetrics.facing = canvas.value?.getFacing() ?? debugMetrics.facing
    stateMachine.request('released', { force: true })
    movement.startRelease(visualPosition, release.velocity)
  },
  onClick() {
    stateMachine.request('idle', { force: true })
    emitPetEvent('PET_CLICK')
  },
  onLongPress() {
    menuOpen.value = true
  },
  onCandidateCancel() {
    if (store.currentState === 'pointerDown') stateMachine.request('idle', { force: true })
  }
})

const removeEventListener = onPetEvent(handleBusinessEvent)

function handleBusinessEvent(event: PetBusinessEvent) {
  const payload = event.payload || {}
  if (!store.visible && !['PET_SHOW', 'PET_HIDE'].includes(event.type)) return
  store.touch()

  if (['PET_CODE_ERROR', 'PET_CODE_SUCCESS', 'PET_EXERCISE_COMPLETE', 'PET_HIDE'].includes(event.type)) cancelInteractiveMotion()

  switch (event.type) {
    case 'PET_WELCOME':
      stateMachine.request('happy', { force: true, priority: payload.priority })
      showDialogue(payload.message || pickPetDialogue(petDialogues.welcome, store.lastDialogue), 'welcome', payload.duration)
      break
    case 'PET_CLICK':
      if (store.currentState === 'sleeping') {
        emitPetEvent('PET_WAKE_UP')
        break
      }
      stateMachine.request('surprised', { priority: payload.priority })
      if (Date.now() - store.lastBubbleTime > 18_000) showDialogue(pickPetDialogue(petDialogues.idle, store.lastDialogue), 'idle')
      playTone('click')
      break
    case 'PET_CODE_START':
      stateMachine.request('thinking', { priority: payload.priority })
      if (Date.now() - store.lastBubbleTime > 28_000) showDialogue('我在认真看你写代码。', 'idle', 2800)
      break
    case 'PET_CODE_ERROR':
      stateMachine.request('error', { priority: payload.priority })
      showDialogue(payload.message || pickPetDialogue(petDialogues.error, store.lastDialogue), 'error', payload.duration)
      playTone('error')
      break
    case 'PET_CODE_SUCCESS':
      stateMachine.request('happy', { force: true, priority: payload.priority })
      showDialogue(payload.message || pickPetDialogue(petDialogues.encourage, store.lastDialogue), 'encourage', payload.duration)
      playTone('success')
      break
    case 'PET_EXERCISE_COMPLETE':
      stateMachine.request('success', { force: true, priority: payload.priority ?? 95 })
      showDialogue(payload.message || '太棒了，这一关完成啦！', 'success', payload.duration)
      playTone('success')
      break
    case 'PET_THINKING':
      stateMachine.request('thinking', { priority: payload.priority })
      break
    case 'PET_IDLE':
      cancelInteractiveMotion()
      stateMachine.request('idle', { force: true })
      break
    case 'PET_SLEEP':
      if (!store.isDragging && !isFlightState(store.currentState)) stateMachine.request('sleeping', { priority: payload.priority })
      break
    case 'PET_WAKE_UP':
      stateMachine.request('happy', { force: true })
      showDialogue('回来啦，我们继续吧！', 'encourage', 2800)
      break
    case 'PET_SHOW_MESSAGE':
      stateMachine.request('speaking', { priority: payload.priority })
      showDialogue(payload.message || '', payload.bubbleType || 'tip', payload.duration, payload.actionLabel, payload.actionEvent)
      break
    case 'PET_HIDE':
      stateMachine.request('hidden', { force: true })
      store.hidePet()
      menuOpen.value = false
      break
    case 'PET_SHOW':
      store.showPet()
      stateMachine.request('idle', { force: true })
      void nextTick(initializePosition)
      break
  }
}

function applyDragFrame(frame: PetDragFrame) {
  updateVisualPosition(frame.position)
  canvas.value?.setMotionFrame(frame)
  debugMetrics.speed = frame.velocity.speed
  debugMetrics.facing = canvas.value?.getFacing() ?? debugMetrics.facing
}

function pushMovementFrame(position: PetPosition) {
  const now = performance.now()
  const seconds = Math.max(0.008, (now - lastMotionAt) / 1000)
  const x = (position.x - lastMotionPosition.x) / seconds
  const y = (position.y - lastMotionPosition.y) / seconds
  const speed = Math.min(2400, Math.hypot(x, y))
  const limiter = Math.hypot(x, y) > speed ? speed / Math.hypot(x, y) : 1
  const frame: PetDragFrame = {
    position,
    velocity: { x: x * limiter, y: y * limiter, speed },
    acceleration: 0,
    sharpTurn: false,
    pointerType: 'animation',
    timestamp: now
  }
  canvas.value?.setMotionFrame(frame)
  debugMetrics.speed = speed
  debugMetrics.facing = canvas.value?.getFacing() ?? debugMetrics.facing
  lastMotionPosition = { ...position }
  lastMotionAt = now
}

function updateVisualPosition(position: PetPosition) {
  visualPosition = position
  applyWidgetPosition(position)
}

function cancelInteractiveMotion() {
  dragController.cancel()
  movement.cancel()
  if (store.isDragging) store.setDragging(false)
}

function showDialogue(
  message: string,
  type: 'welcome' | 'tip' | 'success' | 'error' | 'encourage' | 'idle',
  duration?: number,
  actionLabel = '',
  actionEvent = null as PetBusinessEvent['type'] | null
) {
  if (message) store.showBubble(message, type, duration, actionLabel, actionEvent)
}

function handlePointerDown(event: PetPointerStart) {
  dragController.handlePointerDown(event)
}

function handleReady() {
  ready.value = true
  initializePosition()
  canvas.value?.setTickHandler((deltaMs) => stateMachine.update(deltaMs))
}

function handleAnimationComplete(state: PetState) {
  stateMachine.complete(state)
}

function initializePosition() {
  Object.assign(dimensions, getPetDimensions())
  const saved = store.position
  const initial = saved.x < 0 || saved.y < 0 ? defaultPosition() : saved
  visualPosition = safeArea.findSafePosition(safeArea.clamp(initial))
  lastMotionPosition = { ...visualPosition }
  lastMotionAt = performance.now()
  applyWidgetPosition(visualPosition)
  store.updatePosition(visualPosition)
}

function resetPosition() {
  cancelInteractiveMotion()
  store.resetPosition()
  menuOpen.value = false
  snapping.value = true
  visualPosition = safeArea.findSafePosition(defaultPosition())
  applyWidgetPosition(visualPosition)
  store.updatePosition(visualPosition)
  window.clearTimeout(snapTimer)
  snapTimer = window.setTimeout(() => { snapping.value = false }, 300)
}

function defaultPosition(): PetPosition {
  return {
    x: window.innerWidth - dimensions.width - 24,
    y: window.innerHeight - dimensions.height - dimensions.bottomInset
  }
}

function applyWidgetPosition(position: PetPosition) {
  if (widget.value) widget.value.style.transform = `translate3d(${Math.round(position.x)}px, ${Math.round(position.y)}px, 0)`
}

function openMenu() {
  menuOpen.value = true
}

function openChat() {
  menuOpen.value = false
  chatOpen.value = true
}

function hidePet() {
  emitPetEvent('PET_HIDE')
}

function showPet() {
  emitPetEvent('PET_SHOW')
}

function handleBubbleAction() {
  const actionEvent = store.bubbleActionEvent
  store.hideBubble()
  if (actionEvent) emitPetEvent(actionEvent)
}

function handleWindowActivity() {
  const now = Date.now()
  if (now - activityThrottleAt < 900) return
  activityThrottleAt = now
  if (store.currentState === 'sleeping') emitPetEvent('PET_WAKE_UP')
  else store.touch()
}

function handleVisibilityChange() {
  if (document.hidden) {
    canvas.value?.pause()
    dragController.clearVelocity()
  } else {
    canvas.value?.resume()
    dragController.clearVelocity()
    store.touch()
  }
}

function handleResize() {
  cancelAnimationFrame(resizeFrame)
  resizeFrame = requestAnimationFrame(() => {
    cancelInteractiveMotion()
    Object.assign(dimensions, getPetDimensions())
    visualPosition = safeArea.findSafePosition(safeArea.clamp(visualPosition))
    applyWidgetPosition(visualPosition)
    store.updatePosition(visualPosition)
    if (isFlightState(store.currentState)) stateMachine.request('idle', { force: true })
  })
}

function handleMotionPreference(event: MediaQueryListEvent) {
  reducedMotion.value = event.matches
  movement.setReducedMotion(event.matches)
}

function handleDocumentPointer(event: PointerEvent) {
  const target = event.target
  if (!(target instanceof Element) || target.closest('.pet-menu') || target.closest('.pet-widget')) return
  menuOpen.value = false
}

function runAutomaticBehavior() {
  const now = Date.now()
  if (!store.visible || !store.autoBehaviorEnabled || document.hidden || store.isDragging || isFlightState(store.currentState)) return
  if (now - store.lastInteractionTime >= PET_SLEEP_AFTER) {
    if (store.currentState === 'idle') emitPetEvent('PET_SLEEP')
    return
  }
  if (now < nextAutoAt || store.currentState !== 'idle' || store.bubbleVisible) return

  const actions = [
    () => stateMachine.request('happy'),
    () => stateMachine.request('thinking'),
    () => showDialogue(pickPetDialogue(petDialogues.idle, store.lastDialogue), 'idle')
  ]
  let actionIndex = Math.floor(Math.random() * actions.length)
  if (actionIndex === previousAutoAction) actionIndex = (actionIndex + 1) % actions.length
  previousAutoAction = actionIndex
  actions[actionIndex]()
  nextAutoAt = getNextAutoTime()
}

function isFlightState(state: PetState) {
  return ['pointerDown', 'dragging', 'released', 'gliding', 'flyingToEdge', 'landing'].includes(state)
}

function getNextAutoTime() {
  return Date.now() + PET_AUTO_BEHAVIOR_MIN_INTERVAL + Math.random() * (PET_AUTO_BEHAVIOR_MAX_INTERVAL - PET_AUTO_BEHAVIOR_MIN_INTERVAL)
}

let audioContext: AudioContext | null = null
function playTone(kind: 'click' | 'success' | 'error') {
  if (store.muted || reducedMotion.value) return
  try {
    audioContext ||= new AudioContext()
    if (audioContext.state === 'suspended') void audioContext.resume()
    const oscillator = audioContext.createOscillator()
    const gain = audioContext.createGain()
    oscillator.type = 'sine'
    oscillator.frequency.value = kind === 'success' ? 660 : kind === 'error' ? 260 : 420
    gain.gain.setValueAtTime(0.0001, audioContext.currentTime)
    gain.gain.exponentialRampToValueAtTime(0.035, audioContext.currentTime + 0.015)
    gain.gain.exponentialRampToValueAtTime(0.0001, audioContext.currentTime + 0.16)
    oscillator.connect(gain).connect(audioContext.destination)
    oscillator.start()
    oscillator.stop(audioContext.currentTime + 0.18)
  } catch {
    // 浏览器不允许自动音频时保持静默，不影响视觉反馈。
  }
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  window.addEventListener('pointermove', handleWindowActivity, { passive: true })
  window.addEventListener('keydown', handleWindowActivity)
  window.addEventListener('touchstart', handleWindowActivity, { passive: true })
  document.addEventListener('visibilitychange', handleVisibilityChange)
  document.addEventListener('pointerdown', handleDocumentPointer)
  motionQuery.addEventListener('change', handleMotionPreference)
  autoTimer = window.setInterval(runAutomaticBehavior, 4000)
  if (!store.visible) stateMachine.request('hidden', { force: true })
})

onUnmounted(() => {
  unmounting = true
  removeEventListener()
  dragController.destroy()
  movement.destroy()
  stateMachine.destroy()
  store.dispose()
  window.clearTimeout(snapTimer)
  window.clearInterval(autoTimer)
  cancelAnimationFrame(resizeFrame)
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('pointermove', handleWindowActivity)
  window.removeEventListener('keydown', handleWindowActivity)
  window.removeEventListener('touchstart', handleWindowActivity)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  document.removeEventListener('pointerdown', handleDocumentPointer)
  motionQuery.removeEventListener('change', handleMotionPreference)
  canvas.value?.setTickHandler(null)
  if (audioContext) void audioContext.close()
})
</script>

<style scoped lang="scss">
.pet-widget {
  position: fixed;
  top: 0;
  left: 0;
  z-index: 1001;
  opacity: 0;
  filter: drop-shadow(0 13px 15px rgba(31, 48, 74, 0.1));
  transition: opacity 0.32s ease, filter 0.2s ease;
  will-change: transform;
}

.pet-widget.is-ready { opacity: 1; }
.pet-widget.is-snapping { transition: opacity 0.32s ease, filter 0.2s ease, transform 0.28s var(--ease-out); }
.pet-widget.is-dragging {
  z-index: 1004;
  filter: drop-shadow(0 18px 22px rgba(31, 48, 74, 0.18));
}

.pet-widget__settings {
  position: absolute;
  top: 4px;
  right: 7px;
  display: grid;
  width: 30px;
  height: 30px;
  padding: 0;
  place-items: center;
  color: var(--text-secondary);
  background: color-mix(in srgb, var(--surface-raised), transparent 8%);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  box-shadow: var(--shadow-soft);
  cursor: pointer;
  backdrop-filter: blur(10px);
}

.pet-widget__settings:hover { color: var(--color-primary); border-color: var(--color-primary); }

.pet-debug {
  position: fixed;
  bottom: 18px;
  left: 18px;
  z-index: 1000;
  display: grid;
  gap: 4px;
  min-width: 190px;
  padding: 12px 14px;
  color: #dce9ff;
  background: rgba(14, 25, 43, 0.88);
  border: 1px solid rgba(126, 170, 232, 0.35);
  border-radius: 12px;
  box-shadow: 0 12px 30px rgba(13, 25, 44, 0.2);
  font: 12px/1.45 Consolas, monospace;
  pointer-events: none;
  backdrop-filter: blur(10px);
}

.pet-debug strong { margin-bottom: 2px; color: #8fc1ff; }

.pet-return {
  position: fixed;
  right: 0;
  bottom: 86px;
  z-index: 1001;
  display: flex;
  gap: 7px;
  align-items: center;
  padding: 10px 12px 10px 10px;
  color: var(--color-primary);
  background: color-mix(in srgb, var(--surface-raised), transparent 5%);
  border: 1px solid var(--border-color);
  border-right: 0;
  border-radius: 14px 0 0 14px;
  box-shadow: var(--shadow-soft);
  cursor: pointer;
  font-size: 12px;
  font-weight: 800;
  backdrop-filter: blur(14px);
}

@media (max-width: 620px) {
  .pet-widget__settings { top: -2px; right: 0; width: 28px; height: 28px; }
  .pet-return span { display: none; }
  .pet-return { bottom: 76px; padding: 10px; }
  .pet-debug { right: 12px; bottom: 12px; left: 12px; min-width: 0; }
}

@media (prefers-reduced-motion: reduce) {
  .pet-widget,
  .pet-widget.is-snapping { transition-duration: 0.01ms; }
}
</style>
