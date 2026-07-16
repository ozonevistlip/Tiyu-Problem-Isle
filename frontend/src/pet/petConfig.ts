import type { PetBubbleType, PetState, PetStateDefinition } from './petTypes'

const noop = () => undefined
const noopUpdate = (_deltaMs: number) => undefined

export const PET_STATE_DEFINITIONS: Record<PetState, PetStateDefinition> = {
  idle: { name: 'idle', animation: 'idle', duration: 0, interruptible: true, nextState: 'idle', showBubble: false, priority: 10, onEnter: noop, onUpdate: noopUpdate, onExit: noop },
  sleeping: { name: 'sleeping', animation: 'sleeping', duration: 0, interruptible: true, nextState: 'idle', showBubble: false, priority: 20, onEnter: noop, onUpdate: noopUpdate, onExit: noop },
  happy: { name: 'happy', animation: 'happy', duration: 1600, interruptible: true, nextState: 'idle', showBubble: true, priority: 30, onEnter: noop, onUpdate: noopUpdate, onExit: noop },
  thinking: { name: 'thinking', animation: 'thinking', duration: 5200, interruptible: true, nextState: 'idle', showBubble: false, priority: 40, onEnter: noop, onUpdate: noopUpdate, onExit: noop },
  speaking: { name: 'speaking', animation: 'speaking', duration: 2800, interruptible: true, nextState: 'idle', showBubble: true, priority: 50, onEnter: noop, onUpdate: noopUpdate, onExit: noop },
  surprised: { name: 'surprised', animation: 'surprised', duration: 900, interruptible: true, nextState: 'idle', showBubble: false, priority: 60, onEnter: noop, onUpdate: noopUpdate, onExit: noop },
  landing: { name: 'landing', animation: 'landing', duration: 0, interruptible: true, nextState: 'idle', showBubble: false, priority: 65, onEnter: noop, onUpdate: noopUpdate, onExit: noop },
  flyingToEdge: { name: 'flyingToEdge', animation: 'flyingToEdge', duration: 0, interruptible: true, nextState: 'landing', showBubble: false, priority: 68, onEnter: noop, onUpdate: noopUpdate, onExit: noop },
  gliding: { name: 'gliding', animation: 'gliding', duration: 0, interruptible: true, nextState: 'flyingToEdge', showBubble: false, priority: 70, onEnter: noop, onUpdate: noopUpdate, onExit: noop },
  released: { name: 'released', animation: 'released', duration: 0, interruptible: true, nextState: 'flyingToEdge', showBubble: false, priority: 72, onEnter: noop, onUpdate: noopUpdate, onExit: noop },
  pointerDown: { name: 'pointerDown', animation: 'pointerDown', duration: 0, interruptible: true, nextState: 'idle', showBubble: false, priority: 74, onEnter: noop, onUpdate: noopUpdate, onExit: noop },
  dragging: { name: 'dragging', animation: 'dragging', duration: 0, interruptible: true, nextState: 'released', showBubble: false, priority: 80, onEnter: noop, onUpdate: noopUpdate, onExit: noop },
  error: { name: 'error', animation: 'error', duration: 2200, interruptible: false, nextState: 'idle', showBubble: true, priority: 90, onEnter: noop, onUpdate: noopUpdate, onExit: noop },
  success: { name: 'success', animation: 'success', duration: 2800, interruptible: false, nextState: 'idle', showBubble: true, priority: 100, onEnter: noop, onUpdate: noopUpdate, onExit: noop },
  hidden: { name: 'hidden', animation: null, duration: 0, interruptible: false, nextState: 'idle', showBubble: false, priority: 110, onEnter: noop, onUpdate: noopUpdate, onExit: noop }
}

export const PET_DEFAULT_BUBBLE_DURATION: Record<PetBubbleType, number> = {
  welcome: 5000,
  tip: 5000,
  success: 4200,
  error: 5200,
  encourage: 3600,
  idle: 3000
}

export const PET_STORAGE_KEY = 'cppkid:web-pet:v1'
export const petMotionConfig = {
  dragThreshold: 7, // 桌面端进入拖拽所需的最小位移（像素）。
  mobileDragThreshold: 12, // 触摸端提高阈值，降低滚动页面时的误触。
  clickMaxDuration: 460, // 超过此时长且未触发长按时不再视作普通点击。
  clickMaxDistance: 7, // 普通点击允许的最大位移。
  longPressDuration: 520, // 长按打开宠物菜单的等待时间。
  dragCooldown: 110, // 一次拖拽结束后阻止重复指针事件的短暂冷却。
  maxDragSpeed: 2400, // 速度上限（像素/秒），防止甩动导致参数失控。
  velocitySampleCount: 6, // 固定长度速度采样窗口。
  dragSmoothing: 0.32, // 姿态追随平滑系数；位置本身仍直接跟手。
  maxTiltAngle: 0.16, // 身体最大倾角（弧度）。
  directionFlipThreshold: 120, // 横向速度超过该值才改变朝向。
  directionFlipCooldown: 140, // 朝向切换冷却，避免镜像闪烁。
  wingIdleSpeed: 0.004, // 待机时单图兼容扑翼相位速度。
  wingHoverSpeed: 0.011, // 抓住但接近静止时的悬停扑翼速度。
  wingDragMinSpeed: 0.013, // 低速拖动的扑翼速度。
  wingDragMaxSpeed: 0.03, // 高速拖动的扑翼速度上限。
  wingMaxAngle: 0.045, // 单图兼容模式下的最大周期旋转。
  glideDuration: 360, // 高速松手后的最大滑翔时长。
  glideDistance: 150, // 惯性滑翔的最大距离。
  glideSpeedThreshold: 560, // 超过该释放速度才播放滑翔。
  edgeSnapPadding: 12, // 宠物与视口、安全区域之间的留白。
  edgeFlightMinDuration: 260,
  edgeFlightMaxDuration: 620,
  landingDuration: 420, // 靠边后的减速、压缩和回弹时长。
  particleSpeedThreshold: 720, // 超过该速度才生成速度线和空气粒子。
  maxTrailCount: 10, // 同时存在的飞行轨迹上限。
  reducedMotionEnabled: false // 可由系统偏好在运行时覆盖。
} as const

export const PET_DRAG_THRESHOLD = petMotionConfig.dragThreshold
export const PET_LONG_PRESS_DURATION = petMotionConfig.longPressDuration
export const PET_SLEEP_AFTER = 90_000
export const PET_AUTO_BEHAVIOR_MIN_INTERVAL = 22_000
export const PET_AUTO_BEHAVIOR_MAX_INTERVAL = 38_000

export function getPetDimensions() {
  if (window.innerWidth <= 620) return { width: 122, height: 98, bottomInset: 72 }
  if (window.innerWidth <= 900) return { width: 156, height: 124, bottomInset: 20 }
  return { width: 190, height: 150, bottomInset: 20 }
}
