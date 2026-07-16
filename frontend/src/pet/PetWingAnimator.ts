import { Container, Graphics } from 'pixi.js'
import { petMotionConfig } from './petConfig'
import type { PetDragFrame, PetState } from './petTypes'

interface OptionalWingParts {
  leftWing?: Container
  rightWing?: Container
}

/**
 * 当前素材是身体与双翼合成图，因此默认使用轻微压缩、旋转和翼侧光晕模拟扑翼。
 * 未来提供独立透明翅膀时，只需传入 leftWing/rightWing 即可切换到真实双翼旋转。
 */
export class PetWingAnimator {
  private state: PetState = 'idle'
  private elapsed = 0
  private motion: PetDragFrame | null = null
  private reducedMotion = false
  private lowPerformance = false
  private readonly halo: Graphics

  constructor(
    private readonly wingMotion: Container,
    private readonly effectLayer: Container,
    private readonly parts: OptionalWingParts = {}
  ) {
    this.halo = new Graphics()
      .ellipse(-69, -9, 38, 17)
      .fill({ color: 0xc9c5ff, alpha: 0.24 })
      .ellipse(69, -9, 38, 17)
      .fill({ color: 0xc9c5ff, alpha: 0.24 })
    this.halo.blendMode = 'add'
    this.halo.alpha = 0
    this.effectLayer.addChild(this.halo)
  }

  setState(state: PetState) {
    this.state = state
    this.elapsed = 0
    if (state !== 'dragging') this.motion = null
    this.resetTransforms()
  }

  setMotion(frame: PetDragFrame) {
    this.motion = frame
  }

  setReducedMotion(value: boolean) {
    this.reducedMotion = value
  }

  setLowPerformance(value: boolean) {
    this.lowPerformance = value
  }

  update(deltaMs: number) {
    this.elapsed += Math.min(50, deltaMs)
    const speed = this.motion?.velocity.speed ?? 0
    const speedRatio = Math.min(1, speed / petMotionConfig.maxDragSpeed)
    let phaseSpeed: number = petMotionConfig.wingIdleSpeed
    let amplitude: number = 0.012
    let haloAlpha: number = 0

    switch (this.state) {
      case 'pointerDown':
        phaseSpeed = 0.026
        amplitude = 0.045
        haloAlpha = 0.22
        break
      case 'dragging':
        phaseSpeed = speed < 90
          ? petMotionConfig.wingHoverSpeed
          : petMotionConfig.wingDragMinSpeed + (petMotionConfig.wingDragMaxSpeed - petMotionConfig.wingDragMinSpeed) * speedRatio
        amplitude = 0.026 + speedRatio * 0.032
        haloAlpha = speedRatio > 0.32 ? (speedRatio - 0.32) * 0.42 : 0
        break
      case 'released':
        phaseSpeed = 0.025
        amplitude = 0.05
        haloAlpha = 0.18
        break
      case 'gliding':
        phaseSpeed = 0.0045
        amplitude = 0.008
        haloAlpha = 0.12
        break
      case 'flyingToEdge':
        phaseSpeed = 0.017
        amplitude = 0.035
        haloAlpha = 0.16
        break
      case 'landing':
        phaseSpeed = 0.012
        amplitude = Math.max(0.008, 0.038 * (1 - Math.min(1, this.elapsed / petMotionConfig.landingDuration)))
        haloAlpha = 0.1
        break
    }

    if (this.reducedMotion) {
      phaseSpeed *= 0.42
      amplitude *= 0.3
      haloAlpha = 0
    } else if (this.lowPerformance) {
      haloAlpha = 0
    }

    const pulse = Math.sin(this.elapsed * phaseSpeed)
    this.wingMotion.rotation = pulse * Math.min(petMotionConfig.wingMaxAngle, amplitude)
    this.wingMotion.scale.set(1 + Math.abs(pulse) * amplitude * 0.36, 1 - Math.abs(pulse) * amplitude)
    this.wingMotion.y = -Math.abs(pulse) * amplitude * 35
    this.halo.alpha += (haloAlpha * (0.6 + Math.abs(pulse) * 0.4) - this.halo.alpha) * 0.24
    this.halo.scale.set(1 + speedRatio * 0.08, 0.82 + Math.abs(pulse) * 0.22)

    if (this.parts.leftWing && this.parts.rightWing) {
      const angle = pulse * petMotionConfig.wingMaxAngle * (1 + speedRatio)
      this.parts.leftWing.rotation = angle
      this.parts.rightWing.rotation = -angle
    }
  }

  destroy() {
    this.resetTransforms()
    this.halo.destroy()
  }

  private resetTransforms() {
    this.wingMotion.position.set(0)
    this.wingMotion.scale.set(1)
    this.wingMotion.rotation = 0
    this.halo.alpha = 0
    if (this.parts.leftWing) this.parts.leftWing.rotation = 0
    if (this.parts.rightWing) this.parts.rightWing.rotation = 0
  }
}
