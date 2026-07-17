import type { Container, Graphics } from 'pixi.js'
import { PET_STATE_DEFINITIONS, petMotionConfig } from './petConfig'
import { PetParticleManager } from './PetParticleManager'
import type { PetDragFrame, PetState } from './petTypes'

interface AnimatorHooks {
  onComplete: (state: PetState) => void
}

export class PetAnimator {
  private state: PetState = 'idle'
  private elapsed = 0
  private completed = false
  private lowPerformance = false
  private reducedMotion = false
  private lastLoopParticleAt = 0
  private motion: PetDragFrame | null = null
  private currentRotation = 0

  constructor(
    private readonly bodyMotion: Container,
    private readonly shadow: Graphics,
    private readonly particles: PetParticleManager,
    private readonly hooks: AnimatorHooks
  ) {}

  setState(next: PetState) {
    const preserveMomentum = ['released', 'gliding', 'flyingToEdge', 'landing'].includes(next)
    const previousRotation = this.currentRotation
    this.state = next
    this.elapsed = 0
    this.completed = false
    this.lastLoopParticleAt = 0
    this.resetBody()
    if (preserveMomentum) this.currentRotation = previousRotation

    if (next === 'happy') this.particles.burst('star', 8)
    if (next === 'success') {
      this.particles.burst('star', 11)
      this.particles.burst('confetti', 12)
    }
    if (next === 'error') this.particles.burst('question', 1)
    if (next === 'surprised' || next === 'pointerDown') this.particles.burst('exclamation', 1)
    if (next === 'thinking') this.particles.burst('thinking', 3)
  }

  setLowPerformance(value: boolean) {
    this.lowPerformance = value
    this.particles.setLowPerformance(value)
  }

  setReducedMotion(value: boolean) {
    this.reducedMotion = value
    this.particles.setReducedMotion(value)
  }

  setMotion(frame: PetDragFrame) {
    this.motion = frame
  }

  update(deltaMs: number) {
    if (this.state === 'hidden') return
    const safeDelta = Math.min(deltaMs, 50)
    this.elapsed += safeDelta
    this.particles.update(safeDelta)

    const definition = PET_STATE_DEFINITIONS[this.state]
    const duration = definition.duration
    const progress = duration ? Math.min(1, this.elapsed / duration) : 0
    const motionFactor = this.reducedMotion ? 0.25 : this.lowPerformance ? 0.55 : 1
    const frame = this.motion
    const speedRatio = Math.min(1, (frame?.velocity.speed ?? 0) / petMotionConfig.maxDragSpeed)

    switch (this.state) {
      case 'idle':
        this.bodyMotion.y = Math.sin(this.elapsed / 680) * 3.2 * motionFactor
        this.bodyMotion.rotation = Math.sin(this.elapsed / 1300) * 0.018 * motionFactor
        this.bodyMotion.scale.set(1 + Math.sin(this.elapsed / 900) * 0.018 * motionFactor, 1 - Math.sin(this.elapsed / 900) * 0.01 * motionFactor)
        this.setShadow(1, 0.16, 0)
        break
      case 'happy': {
        const bounce = Math.abs(Math.sin(progress * Math.PI * 2))
        this.bodyMotion.y = -bounce * 25 * motionFactor
        const landing = Math.max(0, Math.sin(progress * Math.PI * 4 - 0.8))
        this.bodyMotion.scale.set(1 + landing * 0.035 * motionFactor, 1 - landing * 0.055 * motionFactor)
        this.bodyMotion.rotation = Math.sin(progress * Math.PI * 4) * 0.035 * motionFactor
        this.setShadow(1 - bounce * 0.22, 0.16 - bounce * 0.05, bounce * 4)
        break
      }
      case 'error':
        this.bodyMotion.rotation = (-0.055 + Math.sin(this.elapsed / 92) * 0.07) * motionFactor
        this.bodyMotion.y = 2
        break
      case 'success':
        this.bodyMotion.y = -Math.sin(progress * Math.PI) * 38 * motionFactor
        this.bodyMotion.rotation = Math.sin(progress * Math.PI * 6) * 0.12 * motionFactor
        this.bodyMotion.scale.set(1 + Math.sin(progress * Math.PI) * 0.08 * motionFactor)
        break
      case 'sleeping':
        this.bodyMotion.y = 6 * motionFactor + Math.sin(this.elapsed / 1000) * 1.5
        this.bodyMotion.scale.set(0.94, 0.92 + Math.sin(this.elapsed / 850) * 0.012)
        if (this.elapsed - this.lastLoopParticleAt > 850) {
          this.particles.burst('sleep', 1)
          this.lastLoopParticleAt = this.elapsed
        }
        break
      case 'surprised': {
        const pulse = Math.sin(Math.min(1, progress * 1.7) * Math.PI)
        this.bodyMotion.y = -pulse * 18 * motionFactor
        this.bodyMotion.scale.set(1 + pulse * 0.14 * motionFactor)
        break
      }
      case 'thinking':
        this.bodyMotion.y = Math.sin(this.elapsed / 520) * 2.5 * motionFactor
        this.bodyMotion.rotation = -0.065 * motionFactor + Math.sin(this.elapsed / 800) * 0.015
        if (this.elapsed - this.lastLoopParticleAt > 1250) {
          this.particles.burst('thinking', 3)
          this.lastLoopParticleAt = this.elapsed
        }
        break
      case 'speaking':
        this.bodyMotion.y = Math.sin(this.elapsed / 240) * 1.8 * motionFactor
        this.bodyMotion.scale.set(1 + Math.sin(this.elapsed / 180) * 0.012 * motionFactor)
        break
      case 'pointerDown': {
        const pulse = Math.min(1, this.elapsed / 180)
        this.bodyMotion.y = -Math.sin(pulse * Math.PI) * 7 * motionFactor
        this.bodyMotion.scale.set(1 - Math.sin(pulse * Math.PI) * 0.055 * motionFactor)
        this.setShadow(0.9, 0.11, 5)
        break
      }
      case 'dragging': {
        const targetRotation = Math.max(-petMotionConfig.maxTiltAngle, Math.min(petMotionConfig.maxTiltAngle,
          ((frame?.velocity.x ?? 0) / petMotionConfig.maxDragSpeed) * petMotionConfig.maxTiltAngle +
          ((frame?.velocity.y ?? 0) / petMotionConfig.maxDragSpeed) * 0.045
        ))
        const smoothing = frame?.sharpTurn ? 0.1 : petMotionConfig.dragSmoothing
        this.currentRotation += (targetRotation - this.currentRotation) * smoothing
        this.bodyMotion.rotation = this.currentRotation
        const hover = Math.sin(this.elapsed * (0.009 + speedRatio * 0.012)) * (1.8 + speedRatio * 1.2) * motionFactor
        this.bodyMotion.y = -6 + hover
        this.bodyMotion.scale.set(0.965 + speedRatio * 0.025, 0.95 - speedRatio * 0.015)
        this.setShadow(0.82 + speedRatio * 0.15, 0.1, 7)
        if (frame) this.particles.emitMotion(frame)
        break
      }
      case 'released':
        this.bodyMotion.rotation = -this.currentRotation * 0.55
        this.bodyMotion.y = -8
        this.setShadow(0.8, 0.09, 7)
        break
      case 'gliding':
        this.bodyMotion.rotation += ((this.currentRotation * 0.65) - this.bodyMotion.rotation) * 0.12
        this.bodyMotion.y = -9 + Math.sin(this.elapsed / 170) * 1.2 * motionFactor
        this.bodyMotion.scale.set(1.02, 0.97)
        this.setShadow(0.76, 0.08, 8)
        break
      case 'flyingToEdge':
        this.bodyMotion.rotation += ((this.currentRotation * 0.4) - this.bodyMotion.rotation) * 0.16
        this.bodyMotion.y = -7 + Math.sin(this.elapsed / 95) * 2.2 * motionFactor
        this.bodyMotion.scale.set(1.01, 0.975)
        this.setShadow(0.82, 0.1, 6)
        break
      case 'landing': {
        const landingProgress = Math.min(1, this.elapsed / petMotionConfig.landingDuration)
        const squash = Math.sin(Math.min(1, landingProgress * 1.45) * Math.PI)
        const rebound = Math.sin(landingProgress * Math.PI * 2) * (1 - landingProgress)
        this.bodyMotion.y = -Math.sin(landingProgress * Math.PI) * 5
        this.bodyMotion.scale.set(1 + squash * 0.055, 1 - squash * 0.08 + rebound * 0.025)
        this.bodyMotion.rotation = this.currentRotation * (1 - landingProgress)
        this.setShadow(0.82 + squash * 0.26, 0.11 + squash * 0.06, 4 * (1 - landingProgress))
        break
      }
    }

    if (duration > 0 && this.elapsed >= duration && !this.completed) {
      this.completed = true
      this.resetBody()
      this.hooks.onComplete(this.state)
    }
  }

  destroy() {
    this.resetBody()
  }

  private setShadow(scaleX: number, alpha: number, yOffset: number) {
    this.shadow.scale.set(scaleX, Math.max(0.65, 1 - yOffset * 0.025))
    this.shadow.alpha = alpha / 0.16
    this.shadow.y = yOffset
  }

  private resetBody() {
    this.bodyMotion.position.set(0)
    this.bodyMotion.scale.set(1)
    this.bodyMotion.rotation = 0
    this.bodyMotion.alpha = 1
    this.shadow.position.set(0)
    this.shadow.scale.set(1)
    this.shadow.alpha = 1
    this.currentRotation = 0
  }
}
