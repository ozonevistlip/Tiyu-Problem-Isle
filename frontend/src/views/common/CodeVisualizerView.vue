<template>
  <main ref="workspaceRoot" class="cpp-workbench">
    <header class="workbench-header">
      <div class="product-title"><span class="product-mark">⌘</span><div><strong>C++ 执行可视化</strong><small>把每一次计算变成清晰的动态过程</small></div></div>
      <div class="header-actions"><el-button :loading="store.parsing" type="primary" :icon="VideoPlay" @click="runCode">运行</el-button><el-button :icon="RefreshRight" @click="resetCode">重置</el-button><el-button circle :icon="FullScreen" title="全屏" @click="toggleFullscreen" /></div>
    </header>

    <section class="workbench-main">
      <article class="code-panel"><header><div><span>CPP SOURCE</span><h1>代码编辑器</h1></div><small>当前执行行会自动高亮</small></header><CodeEditor v-model="code" :highlight-line="store.currentSnapshot?.currentLine" :height="editorHeight" :font-size="fontSize" /><div v-if="diagnostic" class="diagnostic" :class="diagnostic.type"><b>第 {{ diagnostic.line }} 行</b>{{ diagnostic.message }}</div><footer><el-input v-model.number="fontSize" type="number" :min="12" :max="20" aria-label="代码字体大小"><template #append>px</template></el-input><span v-if="isDirty">代码已修改，请重新运行</span><span v-else>支持 C++ 教学子集</span></footer></article>
      <VisualizationStage :event="store.currentEvent" :snapshot="store.currentSnapshot" />
    </section>

    <section class="runtime-strip">
      <article data-runtime-role="console" class="console-panel"><header><span>输入 / 输出</span><small>cin 输入用空格分隔</small></header><el-input v-model="inputText" placeholder="例如：5 10 hello" /><pre>{{ consoleText }}</pre></article>
      <article class="runtime-panel"><header><span>运行状态</span><el-tag :type="statusType" effect="plain">{{ statusText }}</el-tag></header><div class="runtime-grid"><div><small>当前步骤</small><b>{{ currentStep }} / {{ store.totalSteps }}</b></div><div><small>调用栈</small><b>{{ stackText }}</b></div><div><small>当前事件</small><b>{{ eventLabel }}</b></div></div><p>{{ store.currentEvent?.description || '运行后会显示当前步骤的易懂说明。' }}</p></article>
    </section>

    <ExecutionControls class="sticky-controls" :index="store.currentIndex" :total="store.totalSteps" :playing="store.playing" :speed="store.speed" :at-start="store.atStart" :at-end="store.atEnd" @reset="store.reset" @previous="store.previous" @play="store.play" @pause="store.pause" @next="store.next" @seek="store.seek" @update:speed="store.speed = $event" />
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { FullScreen, RefreshRight, VideoPlay } from '@element-plus/icons-vue'
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

const editorHeight = computed(() => isFullscreen.value ? 'calc(100vh - 330px)' : '515px')
const diagnostic = computed(() => store.runtimeError ? { type: 'error', ...store.runtimeError } : store.parseErrors[0] ? { type: 'warning', ...store.parseErrors[0] } : null)
const consoleText = computed(() => store.currentSnapshot?.consoleOutput.length ? store.currentSnapshot.consoleOutput.join('') : '等待程序输出…')
const stackText = computed(() => store.currentSnapshot?.callStack.map((item) => item.name + '()').join(' → ') || '尚未调用函数')
const eventLabel = computed(() => store.currentEvent?.type.split('_').join(' ') || '等待运行')
const currentStep = computed(() => store.totalSteps ? store.currentIndex + 1 : 0)
const statusText = computed(() => store.parsing ? '正在解析' : store.runtimeError ? '运行错误' : store.parseErrors.length ? '语法提示' : store.playing ? '正在播放' : store.result ? '已就绪' : '等待运行')
const statusType = computed(() => store.runtimeError ? 'danger' : store.parseErrors.length ? 'warning' : store.playing ? 'success' : 'info')

async function runCode() {
  isDirty.value = false
  sharedCode.setCurrentCode(code.value)
  await store.run(code.value, inputText.value)
}

function resetCode() {
  code.value = DEFAULT_CODE
  inputText.value = ''
  isDirty.value = true
  store.clear()
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
})

onUnmounted(() => {
  if (debounceTimer !== undefined) window.clearTimeout(debounceTimer)
  document.removeEventListener('fullscreenchange', syncFullscreen)
  store.pause()
})
</script>

<style scoped lang="scss">
.cpp-workbench { display: grid; gap: 14px; max-width: 1680px; min-height: calc(100vh - 112px); margin: 0 auto; }.cpp-workbench:fullscreen { min-height: 100vh; padding: 18px; overflow: auto; background: var(--bg-gradient); }
.workbench-header { display: flex; gap: 18px; align-items: center; justify-content: space-between; padding: 13px 16px; background: var(--surface-glass); border: 1px solid var(--border-soft); border-radius: 16px; box-shadow: var(--shadow-soft); backdrop-filter: blur(12px); }.product-title, .header-actions { display: flex; gap: 10px; align-items: center; }.product-mark { display: grid; width: 38px; height: 38px; place-items: center; color: var(--text-inverse); background: linear-gradient(135deg, var(--color-primary), var(--color-accent)); border-radius: 12px; font-size: 21px; box-shadow: 0 10px 20px color-mix(in srgb, var(--color-primary), transparent 62%); }.product-title > div { display: grid; gap: 2px; }.product-title strong { color: var(--text-primary); }.product-title small { color: var(--text-muted); font-size: 11px; }.header-actions { flex-wrap: wrap; justify-content: flex-end; }
.workbench-main { display: grid; grid-template-columns: minmax(360px, 38fr) minmax(480px, 62fr); gap: 14px; min-height: 0; }.code-panel { display: grid; gap: 11px; padding: 15px; background: var(--surface-card); border: 1px solid var(--border-color); border-radius: 18px; box-shadow: var(--shadow-soft); }.code-panel > header, .console-panel header, .runtime-panel header { display: flex; gap: 12px; align-items: center; justify-content: space-between; }.code-panel header span { color: var(--color-primary); font-size: 10px; font-weight: 900; letter-spacing: .1em; }.code-panel h1 { margin: 4px 0 0; color: var(--text-primary); font-size: 16px; }.code-panel header small, .console-panel small { color: var(--text-muted); font-size: 11px; }.code-panel footer { display: flex; gap: 10px; align-items: center; justify-content: space-between; color: var(--text-muted); font-size: 12px; font-weight: 700; }.code-panel footer .el-input { width: 90px; }.diagnostic { padding: 10px 12px; border-radius: 10px; font-size: 12px; line-height: 1.6; }.diagnostic b { margin-right: 8px; }.diagnostic.warning { color: #a46105; background: #fff7e8; border: 1px solid #f8d795; }.diagnostic.error { color: #b4232a; background: #fff1f1; border: 1px solid #f6c5c7; }
.runtime-strip { display: grid; grid-template-columns: minmax(360px, 38fr) minmax(480px, 62fr); gap: 14px; }.console-panel, .runtime-panel { display: grid; gap: 10px; padding: 14px 16px; background: var(--surface-card); border: 1px solid var(--border-color); border-radius: 16px; box-shadow: var(--shadow-soft); }.console-panel header span, .runtime-panel header span { color: var(--text-primary); font-size: 14px; font-weight: 800; }.console-panel pre { min-height: 64px; max-height: 130px; margin: 0; padding: 11px; overflow: auto; color: #9ae6b4; white-space: pre-wrap; background: #0e1828; border-radius: 10px; font-family: Consolas, monospace; font-size: 12px; }.runtime-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 9px; }.runtime-grid div { display: grid; gap: 3px; padding: 9px; background: var(--surface-soft); border-radius: 10px; }.runtime-grid small { color: var(--text-muted); font-size: 10px; }.runtime-grid b { overflow: hidden; color: var(--color-primary); text-overflow: ellipsis; white-space: nowrap; font-size: 12px; }.runtime-panel p { margin: 0; color: var(--text-secondary); font-size: 12px; line-height: 1.6; }.sticky-controls { position: sticky; bottom: 12px; z-index: 10; }
@media (max-width: 1120px) { .workbench-main, .runtime-strip { grid-template-columns: 1fr; }.code-panel { order: 1; } }
@media (max-width: 650px) { .workbench-header { align-items: flex-start; flex-direction: column; }.header-actions { justify-content: flex-start; }.runtime-grid { grid-template-columns: 1fr; }.code-panel footer { align-items: flex-start; flex-direction: column; } }
</style>
