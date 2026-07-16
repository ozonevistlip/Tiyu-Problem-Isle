<template>
  <section ref="stageRoot" class="visualization-stage" :class="{ 'has-diagnostic': diagnostic }">
    <header class="stage-header"><div><span>执行动画</span><h2>{{ event ? event.description : '点击运行，观察程序如何一步步思考' }}</h2></div><div class="stage-header-actions"><el-tag effect="plain">{{ event ? `第 ${event.line} 行` : '尚未开始' }}</el-tag><el-button circle size="small" :icon="props.isFullscreen ? Close : FullScreen" :title="props.isFullscreen ? '退出全屏' : '进入全屏'" @click="emit('toggle-fullscreen')" /></div></header>
    <div v-if="diagnostic" class="stage-diagnostic" :class="diagnostic.type" role="alert"><b>第 {{ diagnostic.line }} 行</b>{{ diagnostic.message }}</div>
    <div class="stage-viewport">
      <svg class="stage-orbits" viewBox="0 0 640 480" aria-hidden="true"><circle cx="320" cy="240" r="126" /><circle cx="320" cy="240" r="192" /></svg>
      <div v-if="snapshot" class="stage-content">
        <div v-if="operationText" class="operation-chip">{{ operationText }}</div>
        <div v-if="functionResults.length" class="function-results" aria-label="函数计算结果">
          <span v-for="item in functionResults" :key="item">{{ item }}</span>
        </div>
        <div v-if="branch" data-runtime-role="branch" class="branch-card" :class="branchResult ? 'is-true' : 'is-false'"><span>{{ branch }}</span><small v-if="decisionValues.length">{{ decisionValues.join('，') }}</small><div><b>真</b><i /><b>假</b></div><strong>{{ branchAction }}</strong></div>
        <div v-if="loops.length" class="loop-stack"><article v-for="loop in loops" :key="loop.id" data-runtime-role="loop" :class="{ 'is-false': !loop.result }"><span>{{ loop.type }} 循环 · 第 {{ loop.iteration }} 轮</span><b>{{ loop.condition }}</b><small v-if="loopValues(loop.id).length">{{ loopValues(loop.id).join('，') }}</small><strong>{{ loopAction(loop.id) || `条件为${loop.result ? '真，继续执行' : '假，结束循环'}` }}</strong></article></div>
        <VariableRenderer :variables="plainVariables" />
        <div class="container-list">
          <ArrayRenderer v-for="item in arrays" :key="item.name" :name="item.name" :value="item.value" :active-indices="activeIndices(item.name)" :pointer-labels="pointerLabels(item.name)" />
          <MatrixRenderer v-for="item in matrices" :key="item.name" :name="item.name" :value="item.value" :active-indices="activeIndices(item.name)" :pointer-labels="pointerLabels(item.name)" />
          <StringRenderer v-for="item in strings" :key="item.name" :name="item.name" :value="item.value" :active-indices="activeIndices(item.name)" :pointer-labels="pointerLabels(item.name)" />
        </div>
      </div>
      <div v-else class="stage-empty"><span>⌁</span><strong>代码会在这里变成动态画面</strong><p>变量、数组、条件与循环都会使用同一套事件渲染。</p></div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { Close, FullScreen } from '@element-plus/icons-vue'
import ArrayRenderer from './ArrayRenderer.vue'
import MatrixRenderer from './MatrixRenderer.vue'
import StringRenderer from './StringRenderer.vue'
import VariableRenderer from './VariableRenderer.vue'
import { animateTraceEvent } from '@/visualization/traceAnimations'
import type { BaseTraceEvent, ExecutionSnapshot, RuntimeArray, RuntimeArray2D, RuntimeString, RuntimeVariable } from '@/engine/types'

type StageDiagnostic = { type: string; line: number; message: string }
const props = defineProps<{ event: BaseTraceEvent | null; snapshot: ExecutionSnapshot | null; diagnostic?: StageDiagnostic | null; isFullscreen?: boolean }>()
const emit = defineEmits<{ 'toggle-fullscreen': [] }>()
const stageRoot = ref<HTMLElement | null>(null)

const plainVariables = computed(() => props.snapshot?.variables.filter((item) => !['array', 'array2d', 'string'].includes(item.value.kind)) ?? [])
const arrays = computed(() => (props.snapshot?.variables ?? []).filter((item): item is RuntimeVariable & { value: RuntimeArray } => item.value.kind === 'array'))
const matrices = computed(() => (props.snapshot?.variables ?? []).filter((item): item is RuntimeVariable & { value: RuntimeArray2D } => item.value.kind === 'array2d'))
const strings = computed(() => (props.snapshot?.variables ?? []).filter((item): item is RuntimeVariable & { value: RuntimeString } => item.value.kind === 'string'))
const loops = computed(() => props.snapshot?.loops ?? [])
const branch = computed(() => props.event?.type === 'branch' ? String(props.event.data?.expression ?? '') : '')
const branchResult = computed(() => Boolean(props.event?.data?.result))
const decisionValues = computed(() => {
  const values = props.event?.data?.values
  return Array.isArray(values) ? values.map(String) : []
})
const branchAction = computed(() => String(props.event?.data?.nextAction ?? (branchResult.value ? '继续执行 if 分支' : '不执行 if 分支')))
const operationText = computed(() => ['binary_operation', 'comparison'].includes(props.event?.type ?? '') ? String(props.event?.data?.expression ?? '') : '')
const traceFacts = computed(() => [props.event?.data?.reads, props.event?.data?.values]
  .flatMap((item) => Array.isArray(item) ? item.map(String) : []))
const functionResults = computed(() => traceFacts.value.filter((item) => /^[A-Za-z_]\w*(?:\.[A-Za-z_]\w*)?\([^)]*\)\s*=/.test(item)))

function loopValues(loopId: string) {
  if (props.event?.type !== 'loop_condition' || props.event.data?.loopId !== loopId) return []
  return Array.isArray(props.event.data.values) ? props.event.data.values.map(String) : []
}

function loopAction(loopId: string) {
  if (props.event?.type !== 'loop_condition' || props.event.data?.loopId !== loopId) return ''
  return String(props.event.data.nextAction ?? '')
}

interface IndexBinding { indices: number[]; labels: string[] }

function scalarIndexCandidates() {
  const candidates = new Map<string, number>()
  traceFacts.value.forEach((fact) => {
    const match = fact.match(/^([A-Za-z_]\w*)\s*=\s*(-?\d+)$/)
    if (match) candidates.set(match[1], Number(match[2]))
  })
  plainVariables.value.forEach((variable) => {
    if ((variable.value.kind === 'int' || variable.value.kind === 'double') && Number.isInteger(variable.value.value)) {
      candidates.set(variable.name, variable.value.value)
    }
  })
  const preferred = /^(?:i|j|k|idx|index|row|col|left|right|low|high|pos|position)$/i
  return [...candidates.entries()]
    .filter(([label]) => preferred.test(label))
    .map(([label, value]) => ({ label, value }))
    .sort((a, b) => indexPriority(a.label) - indexPriority(b.label))
}

function indexPriority(label: string) {
  const order = ['i', 'row', 'j', 'col', 'k', 'idx', 'index', 'pos', 'position', 'left', 'right', 'low', 'high']
  const index = order.indexOf(label.toLowerCase())
  return index < 0 ? order.length : index
}

function labelsForIndices(indices: number[]) {
  const candidates = scalarIndexCandidates()
  const used = new Set<string>()
  return indices.map((index) => {
    const match = candidates.find((candidate) => candidate.value === index && !used.has(candidate.label))
    if (!match) return `下标 ${index}`
    used.add(match.label)
    return match.label
  })
}

function indicesInBounds(name: string, indices: number[]) {
  const array = arrays.value.find((item) => item.name === name)
  if (array) return indices.length === 1 && indices[0] >= 0 && indices[0] < array.value.length
  const matrix = matrices.value.find((item) => item.name === name)
  if (matrix) return indices.length === 2 && indices[0] >= 0 && indices[0] < matrix.value.rows && indices[1] >= 0 && indices[1] < matrix.value.columns
  const string = strings.value.find((item) => item.name === name)
  if (string) return indices.length === 1 && indices[0] >= 0 && indices[0] < string.value.value.length
  return false
}

function inferredBinding(name: string): IndexBinding {
  if (props.event?.data?.name === name) {
    const raw = String(props.event.data.indices ?? '')
    const indices = raw ? raw.split(',').map(Number).filter(Number.isInteger) : []
    if (indices.length) return { indices, labels: labelsForIndices(indices) }
  }
  const source = traceFacts.value.find((item) => item.startsWith(`${name}[`))
  if (source) {
    const indices = [...source.matchAll(/\[(-?\d+)\]/g)].map((match) => Number(match[1])).filter(Number.isInteger)
    if (indices.length) return { indices, labels: labelsForIndices(indices) }
  }

  const isLoopStep = props.event?.type === 'loop_condition' || loops.value.length > 0
  if (!isLoopStep) return { indices: [], labels: [] }
  const expression = String(props.event?.data?.expression ?? loops.value[loops.value.length - 1]?.condition ?? '')
  const containerCount = arrays.value.length + matrices.value.length + strings.value.length
  const mentionsContainer = new RegExp(`\\b${name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\b`).test(expression)
  if (!mentionsContainer && containerCount !== 1) return { indices: [], labels: [] }
  const dimensions = matrices.value.some((item) => item.name === name) ? 2 : 1
  const candidates = scalarIndexCandidates().slice(0, dimensions)
  const indices = candidates.map((item) => item.value)
  return indicesInBounds(name, indices)
    ? { indices, labels: candidates.map((item) => item.label) }
    : { indices: [], labels: [] }
}

function activeIndices(name: string) { return inferredBinding(name).indices }
function pointerLabels(name: string) { return inferredBinding(name).labels }

watch(
  () => props.event?.id,
  async () => { await nextTick(); animateTraceEvent(stageRoot.value, props.event) }
)
</script>

<style scoped lang="scss">
.visualization-stage { display: grid; grid-template-rows: auto minmax(0, 1fr); min-height: 100%; overflow: hidden; background: var(--surface-card); border: 1px solid var(--border-color); border-radius: 18px; box-shadow: var(--shadow-soft); }.visualization-stage.has-diagnostic { grid-template-rows: auto auto minmax(0, 1fr); }
.stage-header { display: flex; gap: 12px; align-items: center; justify-content: space-between; padding: 15px 18px; border-bottom: 1px solid var(--border-color); }.stage-header span { color: var(--color-primary); font-size: 11px; font-weight: 900; letter-spacing: .08em; }.stage-header h2 { max-width: 620px; margin: 4px 0 0; color: var(--text-primary); font-size: 15px; line-height: 1.45; }.stage-header-actions { display: flex; gap: 8px; align-items: center; }.stage-diagnostic { margin: 12px 18px 0; padding: 10px 12px; border-radius: 10px; font-size: 12px; line-height: 1.6; }.stage-diagnostic b { margin-right: 8px; }.stage-diagnostic.warning { color: #a46105; background: #fff7e8; border: 1px solid #f8d795; }.stage-diagnostic.error { color: #b4232a; background: #fff1f1; border: 1px solid #f6c5c7; }
 .stage-viewport { position: relative; min-height: 510px; overflow: auto; background: radial-gradient(circle at 50% 48%, color-mix(in srgb, var(--color-primary), transparent 94%), transparent 42%); }.stage-orbits { position: absolute; inset: 0; width: 100%; height: 100%; opacity: .4; pointer-events: none; }.stage-orbits circle { fill: none; stroke: var(--color-primary); stroke-dasharray: 4 10; stroke-width: 1; }.stage-content { position: relative; display: grid; gap: 14px; align-content: center; min-height: 510px; padding: 28px; }.container-list { display: grid; gap: 6px; }.operation-chip { justify-self: center; padding: 10px 14px; color: var(--color-primary); background: color-mix(in srgb, var(--color-primary), transparent 90%); border: 1px solid color-mix(in srgb, var(--color-primary), transparent 65%); border-radius: 999px; font-family: Consolas, monospace; font-size: 14px; font-weight: 800; }.function-results { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }.function-results span { padding: 8px 12px; color: var(--color-accent); background: color-mix(in srgb, var(--color-accent), transparent 90%); border: 1px solid color-mix(in srgb, var(--color-accent), transparent 64%); border-radius: 10px; font-family: Consolas, monospace; font-size: 13px; font-weight: 800; box-shadow: 0 6px 20px color-mix(in srgb, var(--color-accent), transparent 90%); }
.branch-card { display: grid; gap: 9px; justify-self: center; min-width: 250px; padding: 14px; text-align: center; background: var(--surface-soft); border: 1px solid var(--border-color); border-radius: 16px; }.branch-card > span { color: var(--text-secondary); font-family: Consolas, monospace; font-size: 13px; }.branch-card > small { color: var(--text-muted); font-size: 11px; }.branch-card > div { display: grid; grid-template-columns: 1fr 1fr 1fr; align-items: center; }.branch-card i { height: 1px; background: var(--border-color); }.branch-card b { color: var(--text-muted); }.branch-card.is-true b:first-child, .branch-card.is-false b:last-child { color: var(--color-success); }.branch-card strong { color: var(--color-success); font-size: 12px; }.branch-card.is-false strong { color: var(--color-warning); }
.loop-stack { display: grid; grid-template-columns: repeat(auto-fit, minmax(210px, 1fr)); gap: 9px; }.loop-stack article { display: grid; gap: 5px; padding: 11px; color: var(--text-primary); background: linear-gradient(135deg, color-mix(in srgb, var(--surface-card) 92%, var(--color-accent) 8%), var(--surface-soft)); border: 1px solid color-mix(in srgb, var(--color-accent), var(--border-color) 58%); border-radius: 13px; }.loop-stack article.is-false { background: linear-gradient(135deg, color-mix(in srgb, var(--surface-card) 92%, var(--color-warning) 8%), var(--surface-soft)); border-color: color-mix(in srgb, var(--color-warning), var(--border-color) 52%); }.loop-stack span, .loop-stack small { color: var(--text-secondary); font-size: 11px; }.loop-stack b { color: var(--text-primary); font-family: Consolas, monospace; font-size: 12px; }.loop-stack strong { color: var(--color-success); font-size: 11px; font-weight: 800; }.loop-stack article.is-false strong { color: var(--color-warning); }
.stage-empty { position: relative; display: grid; min-height: 510px; place-content: center; gap: 9px; padding: 20px; color: var(--text-secondary); text-align: center; }.stage-empty span { color: var(--color-primary); font-size: 60px; line-height: 1; }.stage-empty strong { color: var(--text-primary); }.stage-empty p { margin: 0; color: var(--text-muted); font-size: 13px; }
@media (max-width: 700px) { .stage-viewport, .stage-content, .stage-empty { min-height: 400px; }.stage-content { padding: 16px; } }
</style>
