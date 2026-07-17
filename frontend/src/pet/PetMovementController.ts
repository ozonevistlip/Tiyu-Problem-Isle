import { petMotionConfig } from './petConfig'
import type { PetPosition, PetState, PetVelocity } from './petTypes'

interface PetMovementControllerHooks {
  clampPosition: (position: PetPosition) => PetPosition
  resolveTarget: (position: PetPosition) => PetPosition
  onPosition: (position: PetPosition) => void
  onPhase: (phase: Extract<PetState, 'released' | 'gliding' | 'flyingToEdge' | 'landing'>) => void
  onSettled: (position: PetPosition) => void
}

type MovementPhase = 'idle' | 'released' | 'gliding' | 'flyingToEdge' | 'landing'

export class PetMovementController {
  private phase: MovementPhase = 'idle'
  private frame = 0
  private phaseStartedAt = 0
  private lastFrameAt = 0
  private phaseStart: PetPosition = { x: 0, y: 0 }
  private position: PetPosition = { x: 0, y: 0 }
  private target: PetPosition = { x: 0, y: 0 }
  private glideEnd: PetPosition = { x: 0, y: 0 }
  private flightDuration: number = petMotionConfig.edgeFlightMinDuration
  private releaseVelocity: PetVelocity = { x: 0, y: 0, speed: 0 }
  private reducedMotion = false
  private destroyed = false

  constructor(private readonly hooks: PetMovementControllerHooks) {}

  setReducedMotion(value: boolean) {
    this.reducedMotion = value
  }

  startRelease(position: PetPosition, velocity: PetVelocity) {
    this.cancel()
    this.position = { ...position }
    this.releaseVelocity = { ...velocity }
    this.beginPhase('released')
  }

  cancel() {
    cancelAnimationFrame(this.frame)
    this.frame = 0
    this.phase = 'idle'
  }

  destroy() {
    this.destroyed = true
    this.cancel()
  }

  private beginPhase(phase: Exclude<MovementPhase, 'idle'>) {
    this.phase = phase
    this.phaseStart = { ...this.position }
    this.phaseStartedAt = performance.now()
    this.lastFrameAt = this.phaseStartedAt
    if (phase === 'flyingToEdge') {
      this.target = this.hooks.resolveTarget(this.position)
      const distance = Math.hypot(this.target.x - this.position.x, this.target.y - this.position.y)
      this.flightDuration = this.reducedMotion
        ? 180
        : Math.max(petMotionConfig.edgeFlightMinDuration, Math.min(petMotionConfig.edgeFlightMaxDuration, 220 + distance * 0.72))
    }
    this.hooks.onPhase(phase)
    this.frame = requestAnimationFrame(this.update)
  }

  private readonly update = (now: number) => {
    if (this.destroyed || this.phase === 'idle') return
    const deltaMs = Math.min(40, now - this.lastFrameAt)
    this.lastFrameAt = now
    const elapsed = now - this.phaseStartedAt

    if (this.phase === 'released') {
      const releaseDelay = this.reducedMotion ? 34 : 76
      if (elapsed >= releaseDelay) {
        if (!this.reducedMotion && this.releaseVelocity.speed >= petMotionConfig.glideSpeedThreshold) {
          const distance = Math.min(petMotionConfig.glideDistance, this.releaseVelocity.speed * 0.075)
          const unitX = this.releaseVelocity.x / Math.max(1, this.releaseVelocity.speed)
          const unitY = this.releaseVelocity.y / Math.max(1, this.releaseVelocity.speed)
          this.glideEnd = this.hooks.clampPosition({ x: this.position.x + unitX * distance, y: this.position.y + unitY * distance })
          this.beginPhase('gliding')
        } else {
          this.target = this.hooks.resolveTarget(this.position)
          this.beginPhase('flyingToEdge')
        }
        return
      }
    } else if (this.phase === 'gliding') {
      const progress = Math.min(1, elapsed / petMotionConfig.glideDuration)
      const eased = 1 - Math.pow(1 - progress, 3)
      this.position = this.hooks.clampPosition(this.lerp(this.phaseStart, this.glideEnd, eased))
      this.hooks.onPosition(this.position)
      if (progress >= 1) {
        this.beginPhase('flyingToEdge')
        return
      }
    } else if (this.phase === 'flyingToEdge') {
      const progress = Math.min(1, elapsed / this.flightDuration)
      const eased = progress < 0.5 ? 2 * progress * progress : 1 - Math.pow(-2 * progress + 2, 2) / 2
      const straight = this.lerp(this.phaseStart, this.target, eased)
      const lift = Math.sin(progress * Math.PI) * Math.min(24, Math.hypot(this.target.x - this.phaseStart.x, this.target.y - this.phaseStart.y) * 0.08)
      this.position = this.hooks.clampPosition({ x: straight.x, y: straight.y - lift })
      this.hooks.onPosition(this.position)
      if (progress >= 1) {
        this.position = { ...this.target }
        this.beginPhase('landing')
        return
      }
    } else if (this.phase === 'landing') {
      const duration = this.reducedMotion ? 170 : petMotionConfig.landingDuration
      const progress = Math.min(1, elapsed / duration)
      const lift = this.reducedMotion ? 0 : Math.sin(progress * Math.PI) * 8
      this.position = { x: this.target.x, y: this.target.y - lift }
      this.hooks.onPosition(this.position)
      if (progress >= 1) {
        const settled = { ...this.target }
        this.phase = 'idle'
        this.frame = 0
        this.hooks.onSettled(settled)
        return
      }
    }

    void deltaMs
    this.frame = requestAnimationFrame(this.update)
  }

  private lerp(a: PetPosition, b: PetPosition, amount: number): PetPosition {
    return { x: a.x + (b.x - a.x) * amount, y: a.y + (b.y - a.y) * amount }
  }
}
