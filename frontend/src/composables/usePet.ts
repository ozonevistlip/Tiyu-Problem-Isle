import { emitPetEvent } from '@/pet/PetEventBus'
import type { PetBubbleType, PetEventPayload } from '@/pet/petTypes'

/** 页面只表达业务含义，不需要感知 PixiJS、动画或宠物当前状态。 */
export function usePet() {
  return {
    welcome: (message?: string) => emitPetEvent('PET_WELCOME', { message }),
    codeStart: () => emitPetEvent('PET_CODE_START'),
    codeError: (message?: string) => emitPetEvent('PET_CODE_ERROR', { message }),
    codeSuccess: (message?: string) => emitPetEvent('PET_CODE_SUCCESS', { message }),
    exerciseComplete: (message?: string) => emitPetEvent('PET_EXERCISE_COMPLETE', { message }),
    thinking: () => emitPetEvent('PET_THINKING'),
    idle: () => emitPetEvent('PET_IDLE'),
    sleep: () => emitPetEvent('PET_SLEEP'),
    wakeUp: () => emitPetEvent('PET_WAKE_UP'),
    showMessage: (message: string, options: Omit<PetEventPayload, 'message'> = {}) => emitPetEvent('PET_SHOW_MESSAGE', { message, ...options }),
    hide: () => emitPetEvent('PET_HIDE'),
    show: () => emitPetEvent('PET_SHOW'),
    emit: emitPetEvent,
    showTip: (message: string, bubbleType: PetBubbleType = 'tip') => emitPetEvent('PET_SHOW_MESSAGE', { message, bubbleType })
  }
}
