import type { PetBusinessEvent, PetEventPayload, PetEventType } from './petTypes'

type PetEventHandler = (event: PetBusinessEvent) => void

const handlers = new Set<PetEventHandler>()

export function emitPetEvent(type: PetEventType, payload?: PetEventPayload) {
  const event: PetBusinessEvent = { type, payload }
  handlers.forEach((handler) => handler(event))
}
export function onPetEvent(handler: PetEventHandler) {
  handlers.add(handler)
  return () => handlers.delete(handler)
}
