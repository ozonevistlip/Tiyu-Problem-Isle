<template>
  <transition name="pet-menu">
    <section v-if="open" class="pet-menu" :style="menuStyle" aria-label="宠物设置">
      <header>
        <div>
          <strong>{{ petName || '学习伙伴' }}</strong>
          <small>按你的节奏陪伴</small>
        </div>
        <button type="button" aria-label="关闭宠物设置" @click="emit('close')">
          <el-icon><Close /></el-icon>
        </button>
      </header>
      <button type="button" class="pet-menu__row" @click="emit('toggleMuted')">
        <span>{{ muted ? '开启轻提示音' : '关闭轻提示音' }}</span>
        <span class="pet-menu__switch" :class="{ active: !muted }" />
      </button>
      <button type="button" class="pet-menu__row" @click="emit('toggleAuto')">
        <span>自动互动</span>
        <span class="pet-menu__switch" :class="{ active: autoBehaviorEnabled }" />
      </button>
      <button type="button" class="pet-menu__row" @click="emit('toggleLowPerformance')">
        <span>简洁动画</span>
        <span class="pet-menu__switch" :class="{ active: lowPerformanceMode }" />
      </button>
      <button type="button" class="pet-menu__row is-action" @click="emit('reset')">回到右下角</button>
      <button v-if="canChat" type="button" class="pet-menu__row is-action" @click="emit('chat')">和宠物聊天</button>
      <button type="button" class="pet-menu__row is-danger" @click="emit('hide')">暂时隐藏伙伴</button>
    </section>
  </transition>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Close } from '@element-plus/icons-vue'
import type { PetPosition } from '@/pet/petTypes'

const props = defineProps<{
  open: boolean
  anchor: PetPosition
  petWidth: number
  muted: boolean
  autoBehaviorEnabled: boolean
  lowPerformanceMode: boolean
  petName?: string
  canChat?: boolean
}>()

const emit = defineEmits<{
  close: []
  toggleMuted: []
  toggleAuto: []
  toggleLowPerformance: []
  reset: []
  chat: []
  hide: []
}>()

const menuStyle = computed(() => {
  const width = 220
  const preferLeft = props.anchor.x + props.petWidth / 2 > window.innerWidth / 2
  const rawLeft = preferLeft ? props.anchor.x - width - 10 : props.anchor.x + props.petWidth + 10
  return {
    left: `${Math.max(12, Math.min(window.innerWidth - width - 12, rawLeft))}px`,
    top: `${Math.max(82, Math.min(window.innerHeight - 300, props.anchor.y - 16))}px`
  }
})
</script>

<style scoped lang="scss">
.pet-menu {
  position: fixed;
  z-index: 1003;
  width: 220px;
  padding: 10px;
  color: var(--text-primary);
  background: color-mix(in srgb, var(--surface-raised), transparent 7%);
  border: 1px solid var(--border-color);
  border-radius: 18px;
  box-shadow: 0 20px 50px rgba(28, 47, 78, 0.22);
  backdrop-filter: blur(18px);
}

.pet-menu header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 8px 10px;
  border-bottom: 1px solid var(--border-color);
}

.pet-menu header strong,
.pet-menu header small {
  display: block;
}

.pet-menu header small {
  margin-top: 3px;
  color: var(--text-muted);
  font-size: 11px;
}

.pet-menu header button {
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

.pet-menu__row {
  display: flex;
  width: 100%;
  min-height: 40px;
  padding: 8px;
  align-items: center;
  justify-content: space-between;
  color: var(--text-secondary);
  background: transparent;
  border: 0;
  border-radius: 10px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
  text-align: left;
}

.pet-menu__row:hover {
  color: var(--color-primary);
  background: var(--color-primary-soft);
}

.pet-menu__row.is-action {
  margin-top: 3px;
  color: var(--color-primary);
}

.pet-menu__row.is-danger {
  color: var(--color-danger);
}

.pet-menu__switch {
  position: relative;
  width: 30px;
  height: 18px;
  background: var(--border-color);
  border-radius: 999px;
}

.pet-menu__switch::after {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 12px;
  height: 12px;
  content: '';
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.18);
  transition: transform 0.2s ease;
}

.pet-menu__switch.active {
  background: var(--color-primary);
}

.pet-menu__switch.active::after {
  transform: translateX(12px);
}

.pet-menu-enter-active,
.pet-menu-leave-active {
  transition: opacity 0.18s ease, transform 0.22s var(--ease-out);
}

.pet-menu-enter-from,
.pet-menu-leave-to {
  opacity: 0;
  transform: translateY(5px) scale(0.96);
}
</style>
