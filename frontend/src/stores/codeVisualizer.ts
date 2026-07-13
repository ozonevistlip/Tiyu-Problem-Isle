import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useCodeVisualizerStore = defineStore('codeVisualizer', () => {
  const currentCode = ref('')

  function setCurrentCode(value: string) {
    currentCode.value = value
  }

  return { currentCode, setCurrentCode }
})
