<template>
  <div ref="container" class="code-editor" :style="{ height }" />
</template>

<script setup lang="ts">
import * as monaco from 'monaco-editor'
import { onMounted, onUnmounted, ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    modelValue: string
    language?: string
    height?: string
    readonly?: boolean
    highlightLine?: number | null
    fontSize?: number
  }>(),
  {
    language: 'cpp',
    height: '500px',
    readonly: false,
    highlightLine: null,
    fontSize: 14
  }
)

const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
const container = ref<HTMLElement | null>(null)
let editor: monaco.editor.IStandaloneCodeEditor | null = null
let highlightDecorations: string[] = []

function updateExecutionHighlight(line: number | null | undefined) {
  if (!editor) return
  const model = editor.getModel()
  const validLine = line && model && line >= 1 && line <= model.getLineCount() ? line : null
  highlightDecorations = editor.deltaDecorations(
    highlightDecorations,
    validLine
      ? [{
          range: new monaco.Range(validLine, 1, validLine, 1),
          options: {
            isWholeLine: true,
            className: 'execution-line-highlight',
            glyphMarginClassName: 'execution-line-glyph',
            lineNumberClassName: 'execution-line-number'
          }
        }]
      : []
  )
  if (validLine) editor.revealLineInCenterIfOutsideViewport(validLine, monaco.editor.ScrollType.Smooth)
}

onMounted(() => {
  if (!container.value) return
  editor = monaco.editor.create(container.value, {
    value: props.modelValue,
    language: props.language,
    readOnly: props.readonly,
    theme: 'vs-dark',
    automaticLayout: true,
    minimap: { enabled: false },
    fontSize: props.fontSize,
    glyphMargin: true,
    scrollBeyondLastLine: false
  })
  editor.onDidChangeModelContent(() => emit('update:modelValue', editor?.getValue() || ''))
  updateExecutionHighlight(props.highlightLine)
})

watch(
  () => props.modelValue,
  (value) => {
    if (editor && editor.getValue() !== value) {
      editor.setValue(value)
      updateExecutionHighlight(props.highlightLine)
    }
  }
)

watch(
  () => props.readonly,
  (value) => editor?.updateOptions({ readOnly: value })
)

watch(
  () => props.fontSize,
  (value) => editor?.updateOptions({ fontSize: value })
)

watch(
  () => props.highlightLine,
  (line) => updateExecutionHighlight(line)
)

onUnmounted(() => editor?.dispose())
</script>

<style scoped>
.code-editor {
  width: 100%;
  overflow: hidden;
  border: 1px solid #1f2a44;
  border-radius: 8px;
}
</style>

<style>
.monaco-editor .execution-line-highlight {
  background: linear-gradient(90deg, rgba(251, 191, 36, .2), rgba(251, 191, 36, .05)) !important;
  border-left: 3px solid #fbbf24;
  box-shadow: inset 0 0 24px rgba(251, 191, 36, .08);
}

.monaco-editor .execution-line-number {
  color: #fbbf24 !important;
  font-weight: 800 !important;
}

.monaco-editor .execution-line-glyph::before {
  content: '▶';
  display: grid;
  height: 100%;
  place-items: center;
  color: #fbbf24;
  font-size: 10px;
  filter: drop-shadow(0 0 5px rgba(251, 191, 36, .65));
}
</style>
