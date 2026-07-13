import { computed, shallowRef, ref } from 'vue'
import { defineStore } from 'pinia'
import { runInExecutionWorker } from '@/engine/worker/executionClient'
import type { BaseTraceEvent, ExecutionResult, ParseError, RuntimeErrorInfo } from '@/engine/types'

export const useExecutionStore = defineStore('execution', () => {
  const result = shallowRef<ExecutionResult | null>(null)
  const currentIndex = ref(0)
  const playing = ref(false)
  const speed = ref(1)
  const parsing = ref(false)
  const parseErrors = ref<ParseError[]>([])
  const runtimeError = ref<RuntimeErrorInfo | null>(null)
  let timer: number | undefined

  const totalSteps = computed(() => result.value?.events.length ?? 0)
  const currentEvent = computed<BaseTraceEvent | null>(() => result.value?.events[currentIndex.value] ?? null)
  const currentSnapshot = computed(() => result.value?.snapshots[currentIndex.value] ?? null)
  const progress = computed(() => totalSteps.value ? Math.round(((currentIndex.value + 1) / totalSteps.value) * 100) : 0)
  const atStart = computed(() => currentIndex.value === 0)
  const atEnd = computed(() => currentIndex.value >= totalSteps.value - 1)

  function clearTimer() { if (timer !== undefined) window.clearInterval(timer); timer = undefined }
  function pause() { playing.value = false; clearTimer() }
  function reset() { pause(); currentIndex.value = 0 }
  function previous() { pause(); currentIndex.value = Math.max(0, currentIndex.value - 1) }
  function next() { if (atEnd.value) { pause(); return }; currentIndex.value += 1 }
  function seek(index: number) {
    pause()
    const lastIndex = Math.max(0, totalSteps.value - 1)
    currentIndex.value = Math.min(lastIndex, Math.max(0, Math.round(index)))
  }
  function play() {
    if (!result.value || playing.value) return
    if (atEnd.value) currentIndex.value = 0
    playing.value = true
    clearTimer()
    timer = window.setInterval(next, Math.max(120, 850 / speed.value))
  }
  function begin() { reset(); play() }

  async function run(code: string, rawInput: string) {
    pause()
    parsing.value = true
    parseErrors.value = []
    runtimeError.value = null
    result.value = null
    currentIndex.value = 0
    const input = rawInput.trim() ? rawInput.trim().split(/\s+/) : []
    const response = await runInExecutionWorker({ type: 'run', code, input })
    parsing.value = false
    parseErrors.value = response.parseErrors
    runtimeError.value = response.runtimeError ?? null
    result.value = response.result ?? null
  }

  async function validate(code: string) {
    const response = await runInExecutionWorker({ type: 'validate', code })
    parseErrors.value = response.parseErrors
  }

  function clear() { pause(); result.value = null; currentIndex.value = 0; parseErrors.value = []; runtimeError.value = null }

  return { result, currentIndex, currentEvent, currentSnapshot, totalSteps, progress, playing, speed, parsing, parseErrors, runtimeError, atStart, atEnd, run, validate, clear, begin, play, pause, reset, previous, next, seek }
})
