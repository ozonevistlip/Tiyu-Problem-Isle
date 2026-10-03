import { Application, Assets, Container, Graphics, Rectangle, Sprite, type FederatedPointerEvent, type Texture, type Ticker } from 'pixi.js'
import { PetAnimator } from './PetAnimator'
import { petMotionConfig } from './petConfig'
import { PetParticleManager } from './PetParticleManager'
import { PetSpriteAnimator } from './PetSpriteAnimator'
import { PetWingAnimator } from './PetWingAnimator'
import type { PetDragFrame, PetPointerStart, PetState } from './petTypes'

interface PetRendererHooks {
  onAnimationComplete: (state: PetState) => void
  onPointerDown: (event: PetPointerStart) => void
}

const STAGE_WIDTH = 240
const STAGE_HEIGHT = 180
const BODY_X = STAGE_WIDTH / 2
const BODY_Y = 88

export class PetRenderer {
  private app: Application | null = null
  private animator: PetAnimator | null = null
  private wings: PetWingAnimator | null = null
  private particles: PetParticleManager | null = null
  private spriteAnimator: PetSpriteAnimator | null = null
  private directionContainer: Container | null = null
  private destroyed = false
  private facing: -1 | 1 = 1
  private lastFacingChange = 0
  private tickHandler: ((deltaMs: number) => void) | null = null

  constructor(private readonly hooks: PetRendererHooks) {}

  async init(host: HTMLElement, assetUrl: string) {
    const app = new Application()
    await app.init({
      width: STAGE_WIDTH,
      height: STAGE_HEIGHT,
      backgroundAlpha: 0,
      antialias: true,
      autoDensity: true,
      resolution: Math.min(window.devicePixelRatio || 1, 2),
      preference: 'webgl'
    })
    if (this.destroyed) {
      app.destroy({ removeView: true })
      return
    }

    this.app = app
    app.canvas.className = 'pet-pixi-canvas'
    app.canvas.setAttribute('aria-hidden', 'true')
    Object.assign(app.canvas.style, {
      display: 'block', width: '100%', height: '100%', cursor: 'grab', imageRendering: 'pixelated', touchAction: 'none'
    })
    host.appendChild(app.canvas)

    // Authenticated pet atlases use blob: URLs, which have no file extension for Pixi to detect.
    const texture = await Assets.load<Texture>({ src: assetUrl, loadParser: 'loadTextures' })
    if (this.destroyed) return
    texture.source.scaleMode = 'nearest'

    const shadow = new Graphics().ellipse(BODY_X, 151, 58, 10).fill({ color: 0x1f3855, alpha: 0.16 })
    const effectLayer = new Container({ x: BODY_X, y: BODY_Y })
    const directionContainer = new Container({ x: BODY_X, y: BODY_Y })
    const bodyMotion = new Container()
    const wingMotion = new Container()
    const sprite = new Sprite()
    sprite.anchor.set(0.5)
    sprite.scale.set(0.92)
    const spriteAnimator = new PetSpriteAnimator(sprite, texture)
    wingMotion.addChild(sprite)
    bodyMotion.addChild(wingMotion)
    directionContainer.addChild(bodyMotion)

    const particles = new PetParticleManager()
    app.stage.addChild(shadow, effectLayer, directionContainer, particles.container)
    app.stage.eventMode = 'static'
    app.stage.cursor = 'grab'
    app.stage.hitArea = new Rectangle(0, 0, STAGE_WIDTH, STAGE_HEIGHT)
    app.stage.on('pointerdown', this.handlePointerDown)

    this.directionContainer = directionContainer
    this.particles = particles
    this.spriteAnimator = spriteAnimator
    this.animator = new PetAnimator(bodyMotion, shadow, particles, { onComplete: this.hooks.onAnimationComplete })
    this.wings = new PetWingAnimator(wingMotion, effectLayer)
    app.ticker.add(this.tick)
  }

  play(state: PetState) {
    this.animator?.setState(state)
    this.wings?.setState(state)
    this.spriteAnimator?.setState(state)
  }

  setLowPerformance(value: boolean) {
    this.animator?.setLowPerformance(value)
    this.wings?.setLowPerformance(value)
    this.spriteAnimator?.setLowPerformance(value)
  }

  setReducedMotion(value: boolean) {
    this.animator?.setReducedMotion(value)
    this.wings?.setReducedMotion(value)
    this.spriteAnimator?.setReducedMotion(value)
  }

  setMotionFrame(frame: PetDragFrame) {
    this.animator?.setMotion(frame)
    this.wings?.setMotion(frame)
    this.spriteAnimator?.setMotion(frame)
    const now = performance.now()
    if (Math.abs(frame.velocity.x) >= petMotionConfig.directionFlipThreshold && now - this.lastFacingChange >= petMotionConfig.directionFlipCooldown) {
      const nextFacing: -1 | 1 = frame.velocity.x < 0 ? -1 : 1
      if (nextFacing !== this.facing) {
        this.facing = nextFacing
        this.lastFacingChange = now
        this.spriteAnimator?.setFacing(nextFacing)
      }
    }
  }

  getFacing() {
    return this.facing
  }

  setTickHandler(handler: ((deltaMs: number) => void) | null) {
    this.tickHandler = handler
  }

  pause() {
    this.app?.ticker.stop()
  }

  resume() {
    this.app?.ticker.start()
  }

  destroy() {
    this.destroyed = true
    if (!this.app) return
    this.app.stage.off('pointerdown', this.handlePointerDown)
    this.app.ticker.remove(this.tick)
    this.animator?.destroy()
    this.wings?.destroy()
    this.spriteAnimator?.destroy()
    this.particles?.destroy()
    this.app.destroy({ removeView: true }, { children: true, texture: false, textureSource: false })
    this.app = null
    this.animator = null
    this.wings = null
    this.particles = null
    this.spriteAnimator = null
    this.directionContainer = null
    this.tickHandler = null
  }

  private readonly tick = (ticker: Ticker) => {
    this.animator?.update(ticker.deltaMS)
    this.wings?.update(ticker.deltaMS)
    this.spriteAnimator?.update(ticker.deltaMS)
    this.tickHandler?.(ticker.deltaMS)
  }

  private readonly handlePointerDown = (event: FederatedPointerEvent) => {
    this.hooks.onPointerDown({
      clientX: event.clientX,
      clientY: event.clientY,
      pointerId: event.pointerId,
      pointerType: event.pointerType,
      button: event.button,
      captureTarget: this.app?.canvas ?? null
    })
  }
}
