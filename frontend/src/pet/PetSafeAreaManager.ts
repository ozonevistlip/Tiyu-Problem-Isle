import { petMotionConfig } from './petConfig'
import type { PetBounds, PetPosition } from './petTypes'

interface PetSize {
  width: number
  height: number
  bottomInset: number
}

export class PetSafeAreaManager {
  constructor(private readonly getSize: () => PetSize) {}

  getBounds(): PetBounds {
    const size = this.getSize()
    const padding = petMotionConfig.edgeSnapPadding
    const minY = window.innerWidth <= 760 ? padding : 82
    return {
      minX: padding,
      maxX: Math.max(padding, window.innerWidth - size.width - padding),
      minY,
      maxY: Math.max(minY, window.innerHeight - size.height - size.bottomInset)
    }
  }

  clamp(position: PetPosition): PetPosition {
    const bounds = this.getBounds()
    return {
      x: Math.max(bounds.minX, Math.min(bounds.maxX, position.x)),
      y: Math.max(bounds.minY, Math.min(bounds.maxY, position.y))
    }
  }

  findSafePosition(position: PetPosition): PetPosition {
    let current = this.clamp(position)
    for (let attempt = 0; attempt < 8; attempt += 1) {
      const collision = this.getExclusionRects().find((rect) => this.intersects(current, rect))
      if (!collision) return current
      const size = this.getSize()
      const gap = petMotionConfig.edgeSnapPadding
      const options = [
        { x: collision.left - size.width - gap, y: current.y },
        { x: collision.right + gap, y: current.y },
        { x: current.x, y: collision.top - size.height - gap },
        { x: current.x, y: collision.bottom + gap }
      ].map((item) => this.clamp(item))
      options.sort((a, b) => this.distance(a, current) - this.distance(b, current))
      current = options.find((item) => !this.hasCollision(item)) ?? options[0]
    }
    return current
  }

  findSafeEdgeTarget(position: PetPosition): PetPosition {
    const bounds = this.getBounds()
    const spread = Math.max(70, this.getSize().height * 0.72)
    const candidates: PetPosition[] = []
    const offsets = [0, -spread, spread, -spread * 2, spread * 2]
    for (const offset of offsets) {
      candidates.push(
        { x: bounds.minX, y: position.y + offset },
        { x: bounds.maxX, y: position.y + offset },
        { x: position.x + offset, y: bounds.minY },
        { x: position.x + offset, y: bounds.maxY }
      )
    }

    const unique = new Map<string, PetPosition>()
    candidates.map((item) => this.clamp(item)).forEach((item) => unique.set(`${Math.round(item.x)}:${Math.round(item.y)}`, item))
    const sorted = [...unique.values()].sort((a, b) => this.distance(a, position) - this.distance(b, position))
    return sorted.find((item) => !this.hasCollision(item)) ?? this.findSafePosition(position)
  }

  hasCollision(position: PetPosition) {
    return this.getExclusionRects().some((rect) => this.intersects(position, rect))
  }

  private getExclusionRects() {
    return Array.from(document.querySelectorAll<HTMLElement>('[data-pet-exclusion], .el-overlay'))
      .filter((element) => element.offsetParent !== null && !element.closest('.pet-widget'))
      .map((element) => element.getBoundingClientRect())
      .filter((rect) => rect.width > 0 && rect.height > 0)
  }

  private intersects(position: PetPosition, rect: DOMRect) {
    const size = this.getSize()
    const padding = 6
    return position.x < rect.right + padding && position.x + size.width > rect.left - padding &&
      position.y < rect.bottom + padding && position.y + size.height > rect.top - padding
  }

  private distance(a: PetPosition, b: PetPosition) {
    return Math.hypot(a.x - b.x, a.y - b.y)
  }
}
