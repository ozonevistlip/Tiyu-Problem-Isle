import { Rectangle, Texture, type Sprite } from 'pixi.js'
import { petMotionConfig } from './petConfig'
import type { PetDragFrame, PetState } from './petTypes'

type AtlasRow = 'idle' | 'running-right' | 'running-left' | 'waving' | 'jumping' | 'failed' | 'waiting' | 'running' | 'review'

interface AtlasClip {
  row: number
  frames: number
  fps: number
  loop: boolean
  reverse?: boolean
}

const CELL_WIDTH = 192
const CELL_HEIGHT = 208

const ATLAS_CLIPS: Record<AtlasRow, AtlasClip> = {
  idle: { row: 0, frames: 6, fps: 5, loop: true },
  'running-right': { row: 1, frames: 8, fps: 10, loop: true },
  'running-left': { row: 2, frames: 8, fps: 10, loop: true },
  waving: { row: 3, frames: 4, fps: 7, loop: true },
  jumping: { row: 4, frames: 5, fps: 8, loop: true },
  failed: { row: 5, frames: 8, fps: 5, loop: true },
  waiting: { row: 6, frames: 6, fps: 3.5, loop: true },
  running: { row: 7, frames: 6, fps: 7, loop: true },
  review: { row: 8, frames: 6, fps: 4, loop: true }
}

const STATE_CLIPS: Record<PetState, AtlasRow> = {
  idle: 'idle',
  happy: 'jumping',
  error: 'failed',
  success: 'jumping',
  sleeping: 'waiting',
  surprised: 'jumping',
  thinking: 'review',
  speaking: 'waving',
  pointerDown: 'waving',
  dragging: 'running-right',
  released: 'waving',
  gliding: 'running-right',
  flyingToEdge: 'running-right',
  landing: 'jumping',
  hidden: 'idle'
}

/** 使用 hatch-pet 的 8×9 图集切换角色帧；粒子仍由 PetParticleManager 独立管理。 */
export class PetSpriteAnimator {
  private readonly textures = new Map<AtlasRow, Texture[]>()
  private state: PetState = 'idle'
  private row: AtlasRow = 'idle'
  private facing: -1 | 1 = 1
  private elapsed = 0
  private motionSpeed = 0
  private lowPerformance = false
  private reducedMotion = false

  constructor(private readonly sprite: Sprite, atlas: Texture) {
    atlas.source.scaleMode = 'nearest'
    for (const [rowName, clip] of Object.entries(ATLAS_CLIPS) as Array<[AtlasRow, AtlasClip]>) {
      const frames: Texture[] = []
      for (let column = 0; column < clip.frames; column += 1) {
        frames.push(new Texture({
          source: atlas.source,
          frame: new Rectangle(column * CELL_WIDTH, clip.row * CELL_HEIGHT, CELL_WIDTH, CELL_HEIGHT)
        }))
      }
      this.textures.set(rowName, frames)
    }
    this.applyFrame(0)
  }

  setState(state: PetState) {
    this.state = state
    this.elapsed = 0
    this.selectRow()
    this.applyFrame(0)
  }

  setFacing(facing: -1 | 1) {
    if (this.facing === facing) return
    this.facing = facing
    this.selectRow(false)
  }

  setMotion(frame: PetDragFrame) {
    this.motionSpeed = frame.velocity.speed
  }

  setLowPerformance(value: boolean) {
    this.lowPerformance = value
  }

  setReducedMotion(value: boolean) {
    this.reducedMotion = value
  }

  update(deltaMs: number) {
    const clip = this.clip
    const speedRatio = Math.min(1, this.motionSpeed / petMotionConfig.maxDragSpeed)
    const isMoving = ['dragging', 'gliding', 'flyingToEdge'].includes(this.state)
    let fps = isMoving ? clip.fps * (0.78 + speedRatio * 1.15) : clip.fps
    if (this.lowPerformance) fps *= 0.72
    if (this.reducedMotion) fps *= 0.45
    this.elapsed += Math.min(50, deltaMs)
    const rawIndex = Math.floor(this.elapsed / Math.max(45, 1000 / fps))
    const index = clip.loop ? rawIndex % clip.frames : Math.min(clip.frames - 1, rawIndex)
    this.applyFrame(clip.reverse ? clip.frames - 1 - index : index)
  }

  destroy() {
    this.textures.forEach((frames) => frames.forEach((texture) => texture.destroy(false)))
    this.textures.clear()
  }

  private get clip() {
    return ATLAS_CLIPS[this.row]
  }

  private selectRow(resetElapsed = true) {
    let next = STATE_CLIPS[this.state]
    if (['dragging', 'gliding', 'flyingToEdge'].includes(this.state)) {
      next = this.facing < 0 ? 'running-left' : 'running-right'
    }
    if (next === this.row) return
    this.row = next
    if (resetElapsed) this.elapsed = 0
  }

  private applyFrame(index: number) {
    const frames = this.textures.get(this.row)
    if (frames?.[index] && this.sprite.texture !== frames[index]) this.sprite.texture = frames[index]
  }
}
