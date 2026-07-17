export type PetState =
  | 'idle'
  | 'happy'
  | 'error'
  | 'success'
  | 'sleeping'
  | 'surprised'
  | 'thinking'
  | 'speaking'
  | 'pointerDown'
  | 'dragging'
  | 'released'
  | 'gliding'
  | 'flyingToEdge'
  | 'landing'
  | 'hidden'

export type PetAnimationName = Exclude<PetState, 'hidden'>
export type PetBubbleType = 'welcome' | 'tip' | 'success' | 'error' | 'encourage' | 'idle'
export type PetParticleType = 'star' | 'heart' | 'question' | 'exclamation' | 'confetti' | 'sleep' | 'thinking'

export type PetEventType =
  | 'PET_WELCOME'
  | 'PET_CLICK'
  | 'PET_CODE_START'
  | 'PET_CODE_ERROR'
  | 'PET_CODE_SUCCESS'
  | 'PET_EXERCISE_COMPLETE'
  | 'PET_THINKING'
  | 'PET_IDLE'
  | 'PET_SLEEP'
  | 'PET_WAKE_UP'
  | 'PET_SHOW_MESSAGE'
  | 'PET_HIDE'
  | 'PET_SHOW'

export interface PetPosition {
  x: number
  y: number
}

export interface PetEventPayload {
  message?: string
  priority?: number
  duration?: number
  bubbleType?: PetBubbleType
  actionLabel?: string
  actionEvent?: PetEventType
}

export interface PetBusinessEvent {
  type: PetEventType
  payload?: PetEventPayload
}

export interface PetStateDefinition {
  name: PetState
  animation: PetAnimationName | null
  duration: number
  interruptible: boolean
  nextState: PetState
  showBubble: boolean
  priority: number
  onEnter: () => void
  onUpdate: (deltaMs: number) => void
  onExit: () => void
}

export interface PetPointerStart {
  clientX: number
  clientY: number
  pointerId: number
  pointerType: string
  button: number
  captureTarget: HTMLElement | null
}

export interface PetVelocity {
  x: number
  y: number
  speed: number
}

export interface PetDragFrame {
  position: PetPosition
  velocity: PetVelocity
  acceleration: number
  sharpTurn: boolean
  pointerType: string
  timestamp: number
}

export interface PetDragRelease extends PetDragFrame {
  cancelled: boolean
}

export interface PetBounds {
  minX: number
  maxX: number
  minY: number
  maxY: number
}
