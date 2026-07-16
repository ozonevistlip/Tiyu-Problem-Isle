import { petMotionConfig } from './petConfig'
import type { PetDragFrame, PetDragRelease, PetPointerStart, PetPosition, PetVelocity } from './petTypes'

interface VelocitySample {
  x: number
  y: number
  time: number
}

interface PetDragControllerHooks {
  canStart: () => boolean
  getPosition: () => PetPosition
  clampPosition: (position: PetPosition) => PetPosition
  onPointerDown: () => void
  onDragStart: (frame: PetDragFrame) => void
  onDragMove: (frame: PetDragFrame) => void
  onRelease: (release: PetDragRelease) => void
  onClick: () => void
  onLongPress: () => void
  onCandidateCancel: () => void
}

export class PetDragController {
  private active: (PetPointerStart & {
    startX: number
    startY: number
    startTime: number
    grabOffsetX: number
    grabOffsetY: number
  }) | null = null
  private samples: VelocitySample[] = []
  private dragging = false
  private longPressTimer: number | undefined
  private cooldownUntil = 0
  private previousVelocity: PetVelocity = { x: 0, y: 0, speed: 0 }
  private destroyed = false

  constructor(private readonly hooks: PetDragControllerHooks) {}

  handlePointerDown(event: PetPointerStart) {
    if (this.destroyed || this.active || performance.now() < this.cooldownUntil || !this.hooks.canStart()) return
    if (event.pointerType === 'mouse' && event.button !== 0) return

    const position = this.hooks.getPosition()
    const now = performance.now()
    this.active = {
      ...event,
      startX: event.clientX,
      startY: event.clientY,
      startTime: now,
      grabOffsetX: position.x - event.clientX,
      grabOffsetY: position.y - event.clientY
    }
    this.samples = [{ x: event.clientX, y: event.clientY, time: now }]
    this.dragging = false
    this.previousVelocity = { x: 0, y: 0, speed: 0 }
    this.capture(event.captureTarget, event.pointerId)
    this.hooks.onPointerDown()

    this.longPressTimer = window.setTimeout(() => {
      if (!this.active || this.dragging) return
      this.hooks.onLongPress()
      this.hooks.onCandidateCancel()
      this.finishPointer(false)
    }, petMotionConfig.longPressDuration)

    window.addEventListener('pointermove', this.handlePointerMove, { passive: false })
    window.addEventListener('pointerup', this.handlePointerUp)
    window.addEventListener('pointercancel', this.handlePointerCancel)
  }

  clearVelocity() {
    this.samples.length = 0
    this.previousVelocity = { x: 0, y: 0, speed: 0 }
  }

  cancel() {
    if (!this.active) return
    if (this.dragging) this.release(true, performance.now())
    else {
      this.hooks.onCandidateCancel()
      this.finishPointer(false)
    }
  }

  destroy() {
    this.destroyed = true
    this.cancel()
    this.removeListeners()
  }

  private readonly handlePointerMove = (event: PointerEvent) => {
    const active = this.active
    if (!active || event.pointerId !== active.pointerId) return
    const now = performance.now()
    const totalX = event.clientX - active.startX
    const totalY = event.clientY - active.startY
    const threshold = active.pointerType === 'touch'
      ? petMotionConfig.mobileDragThreshold
      : petMotionConfig.dragThreshold

    if (!this.dragging && Math.hypot(totalX, totalY) >= threshold) {
      this.dragging = true
      window.clearTimeout(this.longPressTimer)
      this.pushSample(event.clientX, event.clientY, now)
      this.hooks.onDragStart(this.createFrame(event.clientX, event.clientY, now))
    }
    if (!this.dragging) return

    event.preventDefault()
    this.pushSample(event.clientX, event.clientY, now)
    this.hooks.onDragMove(this.createFrame(event.clientX, event.clientY, now))
  }

  private readonly handlePointerUp = (event: PointerEvent) => {
    if (!this.active || event.pointerId !== this.active.pointerId) return
    const now = performance.now()
    if (this.dragging) this.release(false, now)
    else {
      const distance = Math.hypot(event.clientX - this.active.startX, event.clientY - this.active.startY)
      const duration = now - this.active.startTime
      if (distance <= petMotionConfig.clickMaxDistance && duration <= petMotionConfig.clickMaxDuration) this.hooks.onClick()
      else this.hooks.onCandidateCancel()
      this.finishPointer(true)
    }
  }

  private readonly handlePointerCancel = (event: PointerEvent) => {
    if (!this.active || event.pointerId !== this.active.pointerId) return
    if (this.dragging) this.release(true, performance.now())
    else {
      this.hooks.onCandidateCancel()
      this.finishPointer(true)
    }
  }

  private release(cancelled: boolean, now: number) {
    const active = this.active
    if (!active) return
    const last = this.samples[this.samples.length - 1] ?? { x: active.startX, y: active.startY, time: now }
    const frame = this.createFrame(last.x, last.y, now)
    this.hooks.onRelease({
      ...frame,
      velocity: cancelled ? { x: 0, y: 0, speed: 0 } : frame.velocity,
      cancelled
    })
    this.finishPointer(true)
  }

  private createFrame(clientX: number, clientY: number, now: number): PetDragFrame {
    const active = this.active!
    const velocity = this.calculateVelocity()
    const previous = this.previousVelocity
    const dot = velocity.x * previous.x + velocity.y * previous.y
    const denominator = Math.max(1, velocity.speed * previous.speed)
    const sharpTurn = velocity.speed > 380 && previous.speed > 380 && dot / denominator < -0.25
    const deltaSeconds = Math.max(0.008, (now - (this.samples[this.samples.length - 2]?.time ?? active.startTime)) / 1000)
    const acceleration = Math.min(5000, Math.abs(velocity.speed - previous.speed) / deltaSeconds)
    this.previousVelocity = velocity
    return {
      position: this.hooks.clampPosition({
        x: clientX + active.grabOffsetX,
        y: clientY + active.grabOffsetY
      }),
      velocity,
      acceleration,
      sharpTurn,
      pointerType: active.pointerType,
      timestamp: now
    }
  }

  private pushSample(x: number, y: number, time: number) {
    const last = this.samples[this.samples.length - 1]
    if (last && time - last.time < 4) return
    this.samples.push({ x, y, time })
    while (this.samples.length > petMotionConfig.velocitySampleCount) this.samples.shift()
  }

  private calculateVelocity(): PetVelocity {
    if (this.samples.length < 2) return { x: 0, y: 0, speed: 0 }
    let weightedX = 0
    let weightedY = 0
    let totalWeight = 0
    for (let index = 1; index < this.samples.length; index += 1) {
      const previous = this.samples[index - 1]
      const current = this.samples[index]
      const deltaSeconds = Math.max(0.004, (current.time - previous.time) / 1000)
      const weight = index
      weightedX += ((current.x - previous.x) / deltaSeconds) * weight
      weightedY += ((current.y - previous.y) / deltaSeconds) * weight
      totalWeight += weight
    }
    const rawX = weightedX / Math.max(1, totalWeight)
    const rawY = weightedY / Math.max(1, totalWeight)
    const rawSpeed = Math.hypot(rawX, rawY)
    const limiter = rawSpeed > petMotionConfig.maxDragSpeed ? petMotionConfig.maxDragSpeed / rawSpeed : 1
    const x = rawX * limiter
    const y = rawY * limiter
    return { x, y, speed: Math.hypot(x, y) }
  }

  private finishPointer(withCooldown: boolean) {
    const active = this.active
    if (active) this.releaseCapture(active.captureTarget, active.pointerId)
    window.clearTimeout(this.longPressTimer)
    this.active = null
    this.dragging = false
    this.samples.length = 0
    if (withCooldown) this.cooldownUntil = performance.now() + petMotionConfig.dragCooldown
    this.removeListeners()
  }

  private removeListeners() {
    window.removeEventListener('pointermove', this.handlePointerMove)
    window.removeEventListener('pointerup', this.handlePointerUp)
    window.removeEventListener('pointercancel', this.handlePointerCancel)
  }

  private capture(target: HTMLElement | null, pointerId: number) {
    try {
      target?.setPointerCapture(pointerId)
    } catch {
      // 某些浏览器会由 Pixi 代理事件，窗口监听仍可保证拖拽完整结束。
    }
  }

  private releaseCapture(target: HTMLElement | null, pointerId: number) {
    try {
      if (target?.hasPointerCapture(pointerId)) target.releasePointerCapture(pointerId)
    } catch {
      // 元素卸载或指针已取消时无需再次释放。
    }
  }
}
