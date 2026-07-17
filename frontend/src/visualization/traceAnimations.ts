import { gsap } from 'gsap'
import type { BaseTraceEvent } from '@/engine/types'

type AnimationHandler = (root: HTMLElement, event: BaseTraceEvent, playbackRate: number) => void

interface SortAnimation {
  timeline: gsap.core.Timeline
  slots: HTMLElement[]
  faces: HTMLElement[]
}

const sortAnimations = new WeakMap<HTMLElement, SortAnimation>()

function clearSortAnimation(root: HTMLElement) {
  const animation = sortAnimations.get(root)
  if (!animation) return
  animation.timeline.kill()
  gsap.set(animation.slots, { clearProps: 'transform,zIndex,transition,filter' })
  gsap.set(animation.faces, { clearProps: 'background,borderColor,color,boxShadow' })
  sortAnimations.delete(root)
}

function pulse(root: HTMLElement, selector: string, playbackRate: number, color = '#5b7cfa') {
  const target = root.querySelector(selector)
  if (!target) return
  gsap.killTweensOf(target)
  gsap.fromTo(target, { y: 6, opacity: 0.55, boxShadow: `0 0 0 0 ${color}` }, { y: 0, opacity: 1, boxShadow: `0 0 0 7px rgba(91, 124, 250, 0)`, duration: 0.42 / playbackRate, ease: 'power2.out' })
}

function pulsePointers(root: HTMLElement, playbackRate: number, selector = '[data-runtime-pointer]') {
  root.querySelectorAll<HTMLElement>(selector).forEach((target) => {
    gsap.killTweensOf(target)
    gsap.fromTo(
      target,
      { opacity: 0.35, filter: 'drop-shadow(0 0 0 rgba(245, 158, 11, 0))' },
      { opacity: 1, filter: 'drop-shadow(0 5px 7px rgba(245, 158, 11, .35))', duration: 0.42 / playbackRate, ease: 'power2.out', clearProps: 'filter' }
    )
  })
}

const byVariable: AnimationHandler = (root, event, playbackRate) => pulse(root, `[data-runtime-name="${event.data?.name ?? ''}"]`, playbackRate)
const byContainer: AnimationHandler = (root, event, playbackRate) => {
  const selector = `[data-runtime-name="${event.data?.name ?? ''}"]`
  pulse(root, selector, playbackRate, '#f59e0b')
  pulsePointers(root, playbackRate, `${selector} [data-runtime-pointer]`)
}

function animateSort(root: HTMLElement, event: BaseTraceEvent, playbackRate: number) {
  const data = event.data ?? {}
  const name = String(data.name ?? '')
  const container = root.querySelector<HTMLElement>(`[data-runtime-name="${name}"]`)
  if (!container) return

  const allSlots = [...container.querySelectorAll<HTMLElement>('[data-runtime-array-slot]')]
  const previousValues = Array.isArray(data.previousValues) ? data.previousValues.map(String) : []
  const sortedValues = Array.isArray(data.sortedValues) ? data.sortedValues.map(String) : []
  const rangeStart = Number(data.rangeStart ?? 0)
  const rangeEnd = Number(data.rangeEnd ?? sortedValues.length)
  if (!allSlots.length || previousValues.length !== allSlots.length || sortedValues.length !== allSlots.length || rangeEnd <= rangeStart) {
    byContainer(root, event, playbackRate)
    return
  }

  const oldIndices = new Map<string, number[]>()
  for (let index = rangeStart; index < rangeEnd; index += 1) {
    const value = previousValues[index]
    const indices = oldIndices.get(value) ?? []
    indices.push(index)
    oldIndices.set(value, indices)
  }

  const slots: HTMLElement[] = []
  const faces: HTMLElement[] = []
  const offsets: number[] = []
  for (let index = rangeStart; index < rangeEnd; index += 1) {
    const sourceIndex = oldIndices.get(sortedValues[index])?.shift()
    const slot = allSlots[index]
    const sourceSlot = sourceIndex === undefined ? undefined : allSlots[sourceIndex]
    const face = slot?.querySelector<HTMLElement>('b')
    if (!slot || !sourceSlot || !face) continue
    slots.push(slot)
    faces.push(face)
    offsets.push(sourceSlot.offsetLeft - slot.offsetLeft)
  }
  if (!slots.length) return

  gsap.set(slots, {
    x: (index: number) => offsets[index],
    y: 0,
    scale: 1,
    zIndex: 3,
    transition: 'none'
  })
  gsap.set(faces, {
    color: '#fff',
    borderColor: 'transparent',
    background: 'linear-gradient(135deg, #5b7cfa, #4a90e2)',
    boxShadow: '0 6px 14px rgba(74, 144, 226, .24)'
  })

  const timeline = gsap.timeline({ defaults: { ease: 'power2.inOut' } })
  timeline
    .to(faces, { boxShadow: '0 8px 18px rgba(74, 144, 226, .36)', duration: 0.8 })
    .to(slots, { y: -18, scale: 0.95, duration: 0.8 }, 'lift')
    .to(faces, { background: 'linear-gradient(135deg, #f6ad2f, #f39c12)', boxShadow: '0 9px 20px rgba(243, 156, 18, .34)', duration: 0.8 }, 'lift')
    .to(slots, { x: 0, duration: 1.2, ease: 'power3.inOut' })
    .to(slots, { y: 0, scale: 1, duration: 0.4 }, 'done')
    .to(faces, { background: 'linear-gradient(135deg, #3bd47c, #2ecc71)', boxShadow: '0 7px 16px rgba(46, 204, 113, .3)', duration: 0.4 }, 'done')
    .set(slots, { clearProps: 'transform,zIndex,transition' })
  timeline.timeScale(playbackRate)
  sortAnimations.set(root, { timeline, slots, faces })
}

export const animationHandlers: Partial<Record<BaseTraceEvent['type'], AnimationHandler>> = {
  variable_declare: byVariable,
  variable_assign: byVariable,
  variable_read: byVariable,
  array_declare: byContainer,
  array_read: byContainer,
  array_write: (root, event, playbackRate) => event.data?.animationKind === 'sort' ? undefined : byContainer(root, event, playbackRate),
  array_2d_declare: byContainer,
  array_2d_read: byContainer,
  array_2d_write: byContainer,
  string_declare: byContainer,
  string_read: byContainer,
  string_write: byContainer,
  branch: (root, _event, playbackRate) => pulse(root, '[data-runtime-role="branch"]', playbackRate, '#22c55e'),
  loop_condition: (root, _event, playbackRate) => {
    pulse(root, '[data-runtime-role="loop"]', playbackRate, '#f59e0b')
    pulsePointers(root, playbackRate)
  },
  console_input: byContainer,
  console_output: (root, _event, playbackRate) => pulse(root, '[data-runtime-role="console"]', playbackRate, '#22c55e')
}

export function animateTraceEvent(root: HTMLElement | null, event: BaseTraceEvent | null, speed = 1) {
  if (!root || !event) return
  const playbackRate = Math.max(0.25, speed / 2)
  clearSortAnimation(root)
  if (event.type === 'array_write' && event.data?.animationKind === 'sort') {
    animateSort(root, event, playbackRate)
    return
  }
  animationHandlers[event.type]?.(root, event, playbackRate)
}
