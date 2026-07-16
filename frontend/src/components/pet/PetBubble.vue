<template>
  <transition name="pet-bubble">
    <aside
      v-if="visible"
      class="pet-bubble"
      :class="[`is-${type}`, `arrow-${placement}`]"
      :style="bubbleStyle"
      role="status"
      aria-live="polite"
    >
      <div class="pet-bubble__icon" aria-hidden="true">
        <el-icon><component :is="bubbleIcon" /></el-icon>
      </div>
      <div class="pet-bubble__content">
        <p>{{ message }}</p>
        <button v-if="actionLabel" type="button" class="pet-bubble__action" @click="emit('action')">
          {{ actionLabel }}
        </button>
      </div>
      <button type="button" class="pet-bubble__close" aria-label="关闭宠物消息" @click="emit('close')">
        <el-icon><Close /></el-icon>
      </button>
    </aside>
  </transition>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ChatDotRound, CircleCheckFilled, Close, InfoFilled, WarningFilled } from '@element-plus/icons-vue'
import type { PetBubbleType, PetPosition } from '@/pet/petTypes'

const props = defineProps<{
  visible: boolean
  message: string
  type: PetBubbleType
  anchor: PetPosition
  petWidth: number
  petHeight: number
  actionLabel?: string
}>()

const emit = defineEmits<{ close: []; action: [] }>()
const bubbleWidth = computed(() => Math.min(286, window.innerWidth - 24))
const placement = computed(() => props.anchor.x + props.petWidth / 2 > window.innerWidth / 2 ? 'right' : 'left')
const bubbleIcon = computed(() => {
  if (props.type === 'success' || props.type === 'encourage') return CircleCheckFilled
  if (props.type === 'error') return WarningFilled
  if (props.type === 'tip') return InfoFilled
  return ChatDotRound
})
const bubbleStyle = computed(() => {
  const width = bubbleWidth.value
  const preferredLeft = placement.value === 'right'
    ? props.anchor.x - width - 12
    : props.anchor.x + props.petWidth + 12
  const left = Math.min(window.innerWidth - width - 12, Math.max(12, preferredLeft))
  const top = Math.min(window.innerHeight - 150, Math.max(82, props.anchor.y + Math.min(18, props.petHeight * 0.15)))
  return { width: `${width}px`, left: `${left}px`, top: `${top}px` }
})
</script>

<style scoped lang="scss">
.pet-bubble {
  position: fixed;
  z-index: 1002;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 10px;
  align-items: start;
  min-height: 70px;
  padding: 14px 12px 13px 14px;
  color: var(--text-primary);
  background: color-mix(in srgb, var(--surface-raised), transparent 4%);
  border: 1px solid var(--border-color);
  border-radius: 18px;
  box-shadow: 0 18px 44px rgba(40, 67, 112, 0.2);
  backdrop-filter: blur(16px);
}

.pet-bubble::after {
  position: absolute;
  top: 28px;
  width: 14px;
  height: 14px;
  content: '';
  background: inherit;
  border: solid var(--border-color);
  transform: rotate(45deg);
}

.arrow-right::after {
  right: -8px;
  border-width: 1px 1px 0 0;
}

.arrow-left::after {
  left: -8px;
  border-width: 0 0 1px 1px;
}

.pet-bubble__icon {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  color: var(--color-primary);
  background: var(--color-primary-soft);
  border-radius: 11px;
  font-size: 17px;
}

.is-success .pet-bubble__icon,
.is-encourage .pet-bubble__icon {
  color: var(--color-success);
  background: color-mix(in srgb, var(--color-success), transparent 88%);
}

.is-error .pet-bubble__icon {
  color: var(--color-danger);
  background: color-mix(in srgb, var(--color-danger), transparent 90%);
}

.pet-bubble__content p {
  margin: 3px 0 0;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.65;
  overflow-wrap: anywhere;
}

.pet-bubble__action {
  padding: 6px 12px;
  margin-top: 9px;
  color: var(--text-inverse);
  background: var(--color-primary);
  border: 0;
  border-radius: 999px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 800;
}

.pet-bubble__close {
  display: grid;
  width: 26px;
  height: 26px;
  padding: 0;
  place-items: center;
  color: var(--text-muted);
  background: transparent;
  border: 0;
  border-radius: 8px;
  cursor: pointer;
}

.pet-bubble__close:hover {
  color: var(--text-primary);
  background: var(--surface-soft);
}

.pet-bubble-enter-active,
.pet-bubble-leave-active {
  transition: opacity 0.2s ease, transform 0.24s var(--ease-out);
}

.pet-bubble-enter-from,
.pet-bubble-leave-to {
  opacity: 0;
  transform: translateY(6px) scale(0.94);
}

@media (max-width: 620px) {
  .pet-bubble {
    min-height: 62px;
    padding: 11px;
    border-radius: 15px;
  }

  .pet-bubble__content p {
    font-size: 13px;
  }
}
</style>
