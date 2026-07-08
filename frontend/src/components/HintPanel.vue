<template>
  <section class="hint-panel" :class="panelClass">
    <div class="hint-head">
      <div>
        <span class="eyebrow">{{ title }}</span>
        <h3>{{ canShowHint ? hintTitle || '解题提示' : '提示暂未解锁' }}</h3>
      </div>
      <el-tag v-if="!canShowHint" type="info">剩余 {{ remainLabel }}</el-tag>
      <el-tag v-else type="warning">提示已解锁</el-tag>
    </div>
    <p v-if="!canShowHint" class="muted">先独立思考一下，倒计时结束后会自动展示老师配置的思路。</p>
    <div v-else class="hint-content">{{ hintContent || '老师暂未配置提示内容。' }}</div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { formatDuration } from '@/utils/time'

const props = defineProps<{
  canShowHint: boolean
  unlockRemainSeconds: number
  hintTitle?: string
  hintContent?: string
  hintLevel?: number
  problemStatus?: string
  contestEnded?: boolean
}>()

const emit = defineEmits<{ unlock: [] }>()
const remain = ref(props.unlockRemainSeconds)
let timer = 0

watch(
  () => props.unlockRemainSeconds,
  (value) => {
    remain.value = value
  }
)

const remainLabel = computed(() => formatDuration(remain.value))
const title = computed(() => {
  if (props.problemStatus === 'ACCEPTED') return '复盘提示'
  if (props.contestEnded) return '赛后讲解'
  return '解题提示'
})
const panelClass = computed(() => (props.canShowHint ? 'is-open' : 'is-locked'))

onMounted(() => {
  timer = window.setInterval(() => {
    if (props.canShowHint) return
    remain.value = Math.max(0, remain.value - 1)
    if (remain.value === 0) emit('unlock')
  }, 1000)
})
onUnmounted(() => window.clearInterval(timer))
</script>

<style scoped lang="scss">
.hint-panel {
  padding: 16px;
  border: 1px solid #d8e2ec;
  border-radius: 8px;
}

.hint-panel.is-locked {
  background: #f7f8fa;
}

.hint-panel.is-open {
  background: #fff8e6;
  border-color: #f6d488;
}

.hint-head {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
}

.eyebrow {
  color: #f59e0b;
  font-size: 12px;
  font-weight: 700;
}

h3 {
  margin: 4px 0 0;
}

.hint-content {
  margin-top: 12px;
  line-height: 1.7;
}
</style>
