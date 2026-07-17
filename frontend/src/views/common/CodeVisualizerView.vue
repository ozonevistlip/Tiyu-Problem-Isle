<template>
  <main ref="workspaceRoot" class="cpp-workbench">
    <section class="workbench-main">
      <div class="workbench-left">
        <article class="code-panel">
          <header>
            <div><span>CPP SOURCE</span><h1>代码编辑器</h1></div>
            <div class="header-actions">
              <small class="font-size-hint">Ctrl + 滚轮调整字号</small>
              <small v-if="isDirty" class="dirty-hint">代码已修改，请重新运行</small>
              <el-button size="small" :icon="MagicStick" @click="formatCode">格式化</el-button>
              <el-button size="small" :loading="store.parsing" type="primary" :icon="VideoPlay" @click="runCode">运行</el-button>
            </div>
          </header>
          <CodeEditor v-model="code" :highlight-line="store.currentSnapshot?.currentLine" :height="editorHeight" :font-size="fontSize" />
        </article>
        <article data-runtime-role="console" class="console-panel">
          <header><span>输入 / 输出</span><small>cin 输入支持空格或换行分隔</small></header>
          <el-input v-model="inputText" type="textarea" :rows="3" resize="vertical" placeholder="例如：5 10 hello，也可以每行输入一项" />
          <pre>{{ consoleText }}</pre>
        </article>
      </div>
      <VisualizationStage :event="store.currentEvent" :snapshot="store.currentSnapshot" :diagnostic="diagnostic" :is-fullscreen="isFullscreen" :speed="store.speed" @toggle-fullscreen="toggleFullscreen" />
    </section>

    <ExecutionControls class="sticky-controls" :index="store.currentIndex" :total="store.totalSteps" :playing="store.playing" :speed="store.speed" :at-start="store.atStart" :at-end="store.atEnd" @reset="store.reset" @previous="store.previous" @play="store.play" @pause="store.pause" @next="store.next" @seek="store.seek" @update:speed="store.speed = $event" />
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { MagicStick, VideoPlay } from '@element-plus/icons-vue'
import CodeEditor from '@/components/CodeEditor.vue'
import ExecutionControls from '@/components/code-visualizer/ExecutionControls.vue'
import VisualizationStage from '@/components/code-visualizer/VisualizationStage.vue'
import { useExecutionStore } from '@/stores/executionStore'
import { useCodeVisualizerStore } from '@/stores/codeVisualizer'

const DEFAULT_CODE = `#include <iostream>
#include <string>
using namespace std;

int main() {
    int arr[10] = {1, 2};
    int number = 7;

    if (number % 2 == 0) {
        cout << "偶数";
    } else {
        cout << "奇数";
    }

    return 0;
}
`

const store = useExecutionStore()
const sharedCode = useCodeVisualizerStore()
const workspaceRoot = ref<HTMLElement | null>(null)
const code = ref('')
const inputText = ref('')
const fontSize = ref(14)
const isDirty = ref(false)
const isFullscreen = ref(false)
let debounceTimer: number | undefined

const editorHeight = computed(() => isFullscreen.value ? '100%' : '515px')
const diagnostic = computed(() => store.runtimeError ? { type: 'error', ...store.runtimeError } : store.parseErrors[0] ? { type: 'warning', ...store.parseErrors[0] } : null)
const consoleText = computed(() => store.currentSnapshot?.consoleOutput.length ? store.currentSnapshot.consoleOutput.join('') : '等待程序输出…')

function formatCppCode(source: string) {
  const lines: string[] = []
  const initializerBraces: boolean[] = []
  let current = ''
  let indent = 0
  let parentheses = 0
  let index = 0

  const normalize = (value: string) => value.trim()
    .replace(/\s+/g, ' ')
    .replace(/\(\s+/g, '(')
    .replace(/\s+\)/g, ')')
    .replace(/\[\s+/g, '[')
    .replace(/\s+\]/g, ']')
    .replace(/\s*,\s*/g, ', ')

  const flush = () => {
    const line = normalize(current)
    if (line) lines.push(`${'  '.repeat(indent)}${line}`)
    current = ''
  }

  while (index < source.length) {
    const character = source[index]
    const next = source[index + 1] ?? ''

    if (character === '\r') { index += 1; continue }
    if (character === '#' && !current.trim()) {
      const lineEnd = source.indexOf('\n', index)
      lines.push(source.slice(index, lineEnd < 0 ? source.length : lineEnd).trim())
      index = lineEnd < 0 ? source.length : lineEnd + 1
      continue
    }
    if (character === '/' && next === '/') {
      const lineEnd = source.indexOf('\n', index)
      const comment = source.slice(index, lineEnd < 0 ? source.length : lineEnd).trim()
      current = current.trim() ? `${current.trimEnd()} ${comment}` : comment
      flush()
      index = lineEnd < 0 ? source.length : lineEnd + 1
      continue
    }
    if (character === '"' || character === "'") {
      const quote = character
      current += character
      index += 1
      while (index < source.length) {
        const quoted = source[index]
        current += quoted
        index += 1
        if (quoted === '\\' && index < source.length) { current += source[index]; index += 1; continue }
        if (quoted === quote) break
      }
      continue
    }
    if (/\s/.test(character)) {
      if (current && !current.endsWith(' ')) current += ' '
      index += 1
      continue
    }
    if (character === '(') { parentheses += 1; current += character; index += 1; continue }
    if (character === ')') { parentheses = Math.max(0, parentheses - 1); current = current.trimEnd() + character; index += 1; continue }
    if (character === ',') { current = current.trimEnd() + ', '; index += 1; continue }
    if (character === '{') {
      const inheritedInitializer = initializerBraces[initializerBraces.length - 1] === true
      const isInitializer = inheritedInitializer || /(?:=|return)\s*$/.test(current.trim())
      initializerBraces.push(isInitializer)
      if (isInitializer) current = current.trimEnd() + '{'
      else { current = current.trimEnd() + ' {'; flush(); indent += 1 }
      index += 1
      continue
    }
    if (character === '}') {
      const isInitializer = initializerBraces.pop() ?? false
      if (isInitializer) current = current.trimEnd() + '}'
      else {
        flush()
        indent = Math.max(0, indent - 1)
        const elseMatch = source.slice(index + 1).match(/^\s*else\b/)
        if (elseMatch) { current = '} else'; index += elseMatch[0].length + 1; continue }
        current = '}'
        flush()
      }
      index += 1
      continue
    }
    if (character === ';') {
      current = current.trimEnd() + ';'
      if (parentheses === 0) flush()
      else current += ' '
      index += 1
      continue
    }
    if (character === '\n') { flush(); index += 1; continue }
    current += character
    index += 1
  }

  flush()
  return lines.join('\n').trimEnd() + '\n'
}

function formatCode() {
  code.value = formatCppCode(code.value)
}

function handleEditorWheel(event: WheelEvent) {
  const target = event.target
  if (!event.ctrlKey || !(target instanceof Element) || !target.closest('.code-editor')) return
  event.preventDefault()
  event.stopPropagation()
  const nextSize = fontSize.value + (event.deltaY < 0 ? 1 : -1)
  fontSize.value = Math.min(20, Math.max(12, nextSize))
}
async function runCode() {
  isDirty.value = false
  sharedCode.setCurrentCode(code.value)
  await store.run(code.value, inputText.value)
}

async function toggleFullscreen() {
  if (document.fullscreenElement) await document.exitFullscreen()
  else await workspaceRoot.value?.requestFullscreen()
}

function syncFullscreen() { isFullscreen.value = document.fullscreenElement === workspaceRoot.value }

watch(code, (value) => {
  sharedCode.setCurrentCode(value)
  isDirty.value = true
  store.pause()
  if (debounceTimer !== undefined) window.clearTimeout(debounceTimer)
  debounceTimer = window.setTimeout(() => store.validate(value), 300)
})

onMounted(() => {
  code.value = sharedCode.currentCode || DEFAULT_CODE
  document.addEventListener('fullscreenchange', syncFullscreen)
  window.addEventListener('wheel', handleEditorWheel, { capture: true, passive: false })
})

onUnmounted(() => {
  if (debounceTimer !== undefined) window.clearTimeout(debounceTimer)
  document.removeEventListener('fullscreenchange', syncFullscreen)
  window.removeEventListener('wheel', handleEditorWheel, { capture: true })
  store.pause()
})
</script>

<style scoped lang="scss">
.cpp-workbench { display: grid; align-content: start; gap: 14px; max-width: 1680px; min-height: calc(100vh - 112px); margin: 0 auto; }.cpp-workbench:fullscreen { box-sizing: border-box; grid-template-rows: minmax(0, 1fr) auto; width: 100vw; height: 100vh; min-height: 0; padding: 18px; overflow: hidden; background: var(--bg-gradient); }
.workbench-header { display: flex; gap: 18px; align-items: center; justify-content: space-between; padding: 13px 16px; background: var(--surface-glass); border: 1px solid var(--border-soft); border-radius: 16px; box-shadow: var(--shadow-soft); backdrop-filter: blur(12px); }.product-title, .header-actions { display: flex; gap: 10px; align-items: center; }.product-mark { display: grid; width: 38px; height: 38px; place-items: center; color: var(--text-inverse); background: linear-gradient(135deg, var(--color-primary), var(--color-accent)); border-radius: 12px; font-size: 21px; box-shadow: 0 10px 20px color-mix(in srgb, var(--color-primary), transparent 62%); }.product-title > div { display: grid; gap: 2px; }.product-title strong { color: var(--text-primary); }.product-title small { color: var(--text-muted); font-size: 11px; }.header-actions { flex-wrap: wrap; justify-content: flex-end; }
.workbench-main { display: grid; grid-template-columns: minmax(360px, 38fr) minmax(480px, 62fr); gap: 14px; min-height: calc(100vh - 190px); }.cpp-workbench:fullscreen .workbench-main { min-height: 0; height: 100%; }.workbench-left { display: grid; grid-template-rows: auto auto; gap: 14px; align-content: start; min-width: 0; min-height: 0; }.cpp-workbench:fullscreen .workbench-left { grid-template-rows: minmax(0, 1fr) auto; align-content: stretch; }.code-panel { display: grid; grid-template-rows: auto auto; gap: 11px; min-height: 0; overflow: hidden; padding: 15px; background: var(--surface-card); border: 1px solid var(--border-color); border-radius: 18px; box-shadow: var(--shadow-soft); }.cpp-workbench:fullscreen .code-panel { grid-template-rows: auto minmax(0, 1fr); }.code-panel > header, .console-panel header { display: flex; gap: 12px; align-items: center; justify-content: space-between; }.code-panel header span { color: var(--color-primary); font-size: 10px; font-weight: 900; letter-spacing: .1em; }.code-panel h1 { margin: 4px 0 0; color: var(--text-primary); font-size: 16px; }.code-panel header small, .console-panel small { color: var(--text-muted); font-size: 11px; }.font-size-hint { white-space: nowrap; }.dirty-hint { color: var(--color-warning) !important; white-space: nowrap; }.diagnostic { padding: 10px 12px; border-radius: 10px; font-size: 12px; line-height: 1.6; }.diagnostic b { margin-right: 8px; }.diagnostic.warning { color: #a46105; background: #fff7e8; border: 1px solid #f8d795; }.diagnostic.error { color: #b4232a; background: #fff1f1; border: 1px solid #f6c5c7; }
.console-panel { display: grid; grid-template-rows: auto auto minmax(96px, 1fr); gap: 10px; min-height: 240px; padding: 14px 16px; background: var(--surface-card); border: 1px solid var(--border-color); border-radius: 16px; box-shadow: var(--shadow-soft); }.console-panel header span { color: var(--text-primary); font-size: 14px; font-weight: 800; }.console-panel :deep(.el-textarea__inner) { min-height: 78px !important; font-family: Consolas, monospace; }.console-panel pre { min-height: 96px; max-height: 180px; margin: 0; padding: 11px; overflow: auto; color: #9ae6b4; white-space: pre-wrap; background: #0e1828; border-radius: 10px; font-family: Consolas, monospace; font-size: 12px; }.sticky-controls { position: sticky; bottom: 12px; z-index: 10; }.cpp-workbench:fullscreen .sticky-controls { position: static; }
@media (max-width: 1120px) { .workbench-main { grid-template-columns: 1fr; }.workbench-left { grid-template-rows: auto auto; }.code-panel { order: 1; } }
@media (max-width: 650px) { .workbench-header { align-items: flex-start; flex-direction: column; }.header-actions { justify-content: flex-start; }.code-panel > header { align-items: flex-start; flex-direction: column; } }
</style>
