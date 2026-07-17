import { defineStore } from 'pinia'
import { PET_DEFAULT_BUBBLE_DURATION, PET_STORAGE_KEY } from '@/pet/petConfig'
import type { PetBubbleType, PetEventType, PetPosition, PetState } from '@/pet/petTypes'

interface PersistedPetSettings {
  visible: boolean
  muted: boolean
  autoBehaviorEnabled: boolean
  lowPerformanceMode: boolean
  position: PetPosition
}

interface PetStoreState extends PersistedPetSettings {
  currentState: PetState
  previousState: PetState
  currentAction: string
  isDragging: boolean
  bubbleVisible: boolean
  bubbleMessage: string
  bubbleType: PetBubbleType
  bubbleActionLabel: string
  bubbleActionEvent: PetEventType | null
  lastInteractionTime: number
  lastDialogue: string
  lastBubbleTime: number
  isInitialized: boolean
}

let bubbleTimer: number | undefined

export const usePetStore = defineStore('pet', {
  state: (): PetStoreState => ({
    visible: true,
    muted: false,
    autoBehaviorEnabled: true,
    lowPerformanceMode: false,
    position: { x: -1, y: -1 },
    currentState: 'idle',
    previousState: 'idle',
    currentAction: 'idle',
    isDragging: false,
    bubbleVisible: false,
    bubbleMessage: '',
    bubbleType: 'idle',
    bubbleActionLabel: '',
    bubbleActionEvent: null,
    lastInteractionTime: Date.now(),
    lastDialogue: '',
    lastBubbleTime: 0,
    isInitialized: false
  }),
  actions: {
    initialize() {
      if (this.isInitialized) return
      this.loadSettings()
      this.isInitialized = true
    },
    showPet() {
      this.visible = true
      this.saveSettings()
    },
    hidePet() {
      this.visible = false
      this.hideBubble()
      this.saveSettings()
    },
    toggleMuted() {
      this.muted = !this.muted
      this.saveSettings()
    },
    toggleAutoBehavior() {
      this.autoBehaviorEnabled = !this.autoBehaviorEnabled
      this.saveSettings()
    },
    toggleLowPerformanceMode() {
      this.lowPerformanceMode = !this.lowPerformanceMode
      this.saveSettings()
    },
    setState(next: PetState) {
      if (this.currentState === next) return
      this.previousState = this.currentState
      this.currentState = next
      this.currentAction = next
    },
    setDragging(value: boolean) {
      this.isDragging = value
      this.touch()
    },
    touch() {
      this.lastInteractionTime = Date.now()
    },
    showBubble(message: string, type: PetBubbleType = 'idle', duration?: number, actionLabel = '', actionEvent: PetEventType | null = null) {
      window.clearTimeout(bubbleTimer)
      this.bubbleMessage = message
      this.bubbleType = type
      this.bubbleVisible = true
      this.bubbleActionLabel = actionLabel
      this.bubbleActionEvent = actionEvent
      this.lastDialogue = message
      this.lastBubbleTime = Date.now()
      if (!actionLabel) {
        bubbleTimer = window.setTimeout(() => this.hideBubble(), duration ?? PET_DEFAULT_BUBBLE_DURATION[type])
      }
    },
    hideBubble() {
      window.clearTimeout(bubbleTimer)
      this.bubbleVisible = false
      this.bubbleActionLabel = ''
      this.bubbleActionEvent = null
    },
    updatePosition(position: PetPosition, persist = true) {
      this.position = { x: Math.round(position.x), y: Math.round(position.y) }
      if (persist) this.saveSettings()
    },
    resetPosition() {
      this.position = { x: -1, y: -1 }
      this.saveSettings()
    },
    saveSettings() {
      const settings: PersistedPetSettings = {
        visible: this.visible,
        muted: this.muted,
        autoBehaviorEnabled: this.autoBehaviorEnabled,
        lowPerformanceMode: this.lowPerformanceMode,
        position: this.position
      }
      try {
        localStorage.setItem(PET_STORAGE_KEY, JSON.stringify(settings))
      } catch {
        // 隐私模式或存储空间不足时，宠物仍可在当前页面正常工作。
      }
    },
    loadSettings() {
      try {
        const raw = localStorage.getItem(PET_STORAGE_KEY)
        if (!raw) return
        const settings = JSON.parse(raw) as Partial<PersistedPetSettings>
        if (typeof settings.visible === 'boolean') this.visible = settings.visible
        if (typeof settings.muted === 'boolean') this.muted = settings.muted
        if (typeof settings.autoBehaviorEnabled === 'boolean') this.autoBehaviorEnabled = settings.autoBehaviorEnabled
        if (typeof settings.lowPerformanceMode === 'boolean') this.lowPerformanceMode = settings.lowPerformanceMode
        if (Number.isFinite(settings.position?.x) && Number.isFinite(settings.position?.y)) this.position = settings.position as PetPosition
      } catch {
        localStorage.removeItem(PET_STORAGE_KEY)
      }
    },
    dispose() {
      window.clearTimeout(bubbleTimer)
    }
  }
})
