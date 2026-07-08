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
  }>(),
  {
    language: 'cpp',
    height: '500px',
    readonly: false
  }
)

const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
const container = ref<HTMLElement | null>(null)
let editor: monaco.editor.IStandaloneCodeEditor | null = null

onMounted(() => {
  if (!container.value) return
  editor = monaco.editor.create(container.value, {
    value: props.modelValue,
    language: props.language,
    readOnly: props.readonly,
    theme: 'vs-dark',
    automaticLayout: true,
    minimap: { enabled: false },
    fontSize: 14,
    scrollBeyondLastLine: false
  })
  editor.onDidChangeModelContent(() => emit('update:modelValue', editor?.getValue() || ''))
})

watch(
  () => props.modelValue,
  (value) => {
    if (editor && editor.getValue() !== value) editor.setValue(value)
  }
)

watch(
  () => props.readonly,
  (value) => editor?.updateOptions({ readOnly: value })
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
