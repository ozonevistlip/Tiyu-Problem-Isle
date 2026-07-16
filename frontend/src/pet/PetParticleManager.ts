import { Container, Graphics, Text } from 'pixi.js'
import { petMotionConfig } from './petConfig'
import type { PetDragFrame, PetParticleType } from './petTypes'

interface Particle {
  display: Graphics | Text
  velocityX: number
  velocityY: number
  life: number
  maxLife: number
  rotationSpeed: number
  motionTrail: boolean
}

const COLORS = [0xf6bd38, 0x5aa6ff, 0xf28ab2, 0x68c98f, 0xa98cf5]

export class PetParticleManager {
  readonly container = new Container()
  private readonly particles: Particle[] = []
  private lowPerformance = false
  private reducedMotion = false
  private lastMotionEmitAt = 0

  setLowPerformance(value: boolean) {
    this.lowPerformance = value
  }

  setReducedMotion(value: boolean) {
    this.reducedMotion = value
  }

  burst(type: PetParticleType, requestedCount = 8) {
    const mobileFactor = window.innerWidth <= 760 ? 0.55 : 1
    const performanceFactor = this.lowPerformance ? 0.35 : 1
    const count = Math.max(1, Math.min(18, Math.round(requestedCount * mobileFactor * performanceFactor)))

    for (let index = 0; index < count; index += 1) {
      const display = this.createDisplay(type, index)
      const angle = Math.PI * (0.15 + Math.random() * 0.7)
      const speed = 0.025 + Math.random() * 0.045
      const isStatusMark = ['question', 'exclamation', 'sleep', 'thinking'].includes(type)
      const originX = isStatusMark ? 120 + (index - count / 2) * 12 : 120 + (Math.random() - 0.5) * 95
      const originY = isStatusMark ? 28 - index * 8 : 82 + (Math.random() - 0.5) * 45
      const life = isStatusMark ? 1100 + index * 160 : 900 + Math.random() * 650

      display.position.set(originX, originY)
      this.container.addChild(display)
      this.particles.push({
        display,
        velocityX: isStatusMark ? 0.006 + index * 0.002 : Math.cos(angle) * speed * (Math.random() > 0.5 ? 1 : -1),
        velocityY: isStatusMark ? -0.018 : -Math.sin(angle) * speed,
        life,
        maxLife: life,
        rotationSpeed: (Math.random() - 0.5) * 0.004,
        motionTrail: false
      })
    }
  }

  emitMotion(frame: PetDragFrame) {
    if (this.lowPerformance || this.reducedMotion || frame.velocity.speed < petMotionConfig.particleSpeedThreshold) return
    const now = performance.now()
    const interval = window.innerWidth <= 760 ? 95 : 58
    if (now - this.lastMotionEmitAt < interval) return
    const trailCount = this.particles.filter((particle) => particle.motionTrail).length
    if (trailCount >= petMotionConfig.maxTrailCount) return
    this.lastMotionEmitAt = now

    const unitX = frame.velocity.x / Math.max(1, frame.velocity.speed)
    const unitY = frame.velocity.y / Math.max(1, frame.velocity.speed)
    const speedRatio = Math.min(1, frame.velocity.speed / petMotionConfig.maxDragSpeed)
    const length = 18 + speedRatio * 34
    const line = new Graphics()
      .moveTo(unitX * 8, unitY * 8)
      .lineTo(-unitX * length, -unitY * length)
      .stroke({ color: 0xb7cdf7, alpha: 0.58, width: 2.4 })
    line.position.set(120 - unitX * 46 + (Math.random() - 0.5) * 40, 88 - unitY * 28 + (Math.random() - 0.5) * 34)
    const life = 210
    this.container.addChild(line)
    this.particles.push({
      display: line,
      velocityX: -unitX * 0.018,
      velocityY: -unitY * 0.018 - 0.006,
      life,
      maxLife: life,
      rotationSpeed: 0,
      motionTrail: true
    })

    if (trailCount % 2 === 0) {
      const air = new Graphics().circle(0, 0, 2.2 + speedRatio * 1.8).fill({ color: 0xe9efff, alpha: 0.68 })
      air.position.set(120 - unitX * 54 + (Math.random() - 0.5) * 50, 88 + (Math.random() - 0.5) * 46)
      this.container.addChild(air)
      this.particles.push({
        display: air,
        velocityX: -unitX * 0.026,
        velocityY: -unitY * 0.02 - 0.01,
        life: 320,
        maxLife: 320,
        rotationSpeed: 0,
        motionTrail: true
      })
    }
  }

  update(deltaMs: number) {
    for (let index = this.particles.length - 1; index >= 0; index -= 1) {
      const particle = this.particles[index]
      particle.life -= deltaMs
      if (particle.life <= 0) {
        this.container.removeChild(particle.display)
        particle.display.destroy()
        this.particles.splice(index, 1)
        continue
      }

      particle.display.x += particle.velocityX * deltaMs
      particle.display.y += particle.velocityY * deltaMs
      particle.display.rotation += particle.rotationSpeed * deltaMs
      const progress = 1 - particle.life / particle.maxLife
      particle.display.alpha = particle.motionTrail
        ? Math.max(0, 1 - progress)
        : Math.min(1, particle.life / 260) * (1 - progress * 0.2)
      const scale = particle.motionTrail ? 1 - progress * 0.28 : 0.75 + Math.sin(progress * Math.PI) * 0.3
      particle.display.scale.set(scale)
    }
  }

  clear() {
    this.particles.forEach((particle) => particle.display.destroy())
    this.particles.length = 0
    this.container.removeChildren()
  }

  destroy() {
    this.clear()
    this.container.destroy({ children: true })
  }

  private createDisplay(type: PetParticleType, index: number) {
    if (type === 'confetti') {
      return new Graphics()
        .roundRect(-3, -5, 6, 10, 2)
        .fill(COLORS[index % COLORS.length])
    }
    if (type === 'thinking') {
      return new Graphics().circle(0, 0, 4 + index).fill({ color: 0x5b6b82, alpha: 0.9 })
    }

    const content = type === 'star' ? '✦' : type === 'heart' ? '♥' : type === 'question' ? '?' : type === 'exclamation' ? '!' : 'Z'
    const color = type === 'heart' ? 0xf06b92 : type === 'question' ? 0x4b84d1 : type === 'exclamation' ? 0xef8f27 : type === 'sleep' ? 0x6d75b8 : 0xf5b82e
    return new Text({
      text: content,
      style: {
        fill: color,
        fontFamily: 'Arial, Microsoft YaHei, sans-serif',
        fontSize: type === 'sleep' ? 18 + index * 3 : 24,
        fontWeight: '800',
        stroke: { color: 0xffffff, width: 3 }
      }
    })
  }
}
