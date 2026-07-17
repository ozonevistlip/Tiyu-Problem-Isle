import { PET_STATE_DEFINITIONS } from './petConfig'
import type { PetState } from './petTypes'

interface TransitionOptions {
  force?: boolean
  priority?: number
}

interface PetStateMachineHooks {
  onTransition: (next: PetState, previous: PetState) => void
  onUpdate?: (state: PetState, deltaMs: number) => void
}

export class PetStateMachine {
  private current: PetState
  private lastRequestedAt = 0
  private destroyed = false

  constructor(initial: PetState, private readonly hooks: PetStateMachineHooks) {
    this.current = initial
  }

  get state() {
    return this.current
  }

  request(next: PetState, options: TransitionOptions = {}) {
    if (this.destroyed) return false
    const now = Date.now()
    if (!options.force && next === this.current && now - this.lastRequestedAt < 500) return false

    const currentDefinition = PET_STATE_DEFINITIONS[this.current]
    const nextDefinition = PET_STATE_DEFINITIONS[next]
    const nextPriority = options.priority ?? nextDefinition.priority

    if (!options.force) {
      if (this.current === 'hidden' && next !== 'hidden') return false
      if (!currentDefinition.interruptible && nextPriority <= currentDefinition.priority) return false
    }

    const previous = this.current
    currentDefinition.onExit()
    this.current = next
    this.lastRequestedAt = now
    nextDefinition.onEnter()
    this.hooks.onTransition(next, previous)
    return true
  }

  complete(completedState: PetState) {
    if (this.destroyed || this.current !== completedState) return
    const definition = PET_STATE_DEFINITIONS[completedState]
    if (definition.duration > 0) this.request(definition.nextState, { force: true })
  }

  update(deltaMs: number) {
    if (this.destroyed) return
    PET_STATE_DEFINITIONS[this.current].onUpdate(deltaMs)
    this.hooks.onUpdate?.(this.current, deltaMs)
  }

  destroy() {
    PET_STATE_DEFINITIONS[this.current].onExit()
    this.destroyed = true
  }
}
