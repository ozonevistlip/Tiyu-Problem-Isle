import { gsap } from 'gsap'
import type { BaseTraceEvent } from '@/engine/types'

type AnimationHandler = (root: HTMLElement, event: BaseTraceEvent) => void

function pulse(root: HTMLElement, selector: string, color = '#5b7cfa') {
  const target = root.querySelector(selector)
  if (!target) return
  gsap.killTweensOf(target)
  gsap.fromTo(target, { y: 6, opacity: 0.55, boxShadow: `0 0 0 0 ${color}` }, { y: 0, opacity: 1, boxShadow: `0 0 0 7px rgba(91, 124, 250, 0)`, duration: 0.42, ease: 'power2.out' })
}

function pulsePointers(root: HTMLElement, selector = '[data-runtime-pointer]') {
  root.querySelectorAll<HTMLElement>(selector).forEach((target) => {
    gsap.killTweensOf(target)
    gsap.fromTo(
      target,
      { opacity: 0.35, filter: 'drop-shadow(0 0 0 rgba(245, 158, 11, 0))' },
      { opacity: 1, filter: 'drop-shadow(0 5px 7px rgba(245, 158, 11, .35))', duration: 0.42, ease: 'power2.out', clearProps: 'filter' }
    )
  })
}

const byVariable: AnimationHandler = (root, event) => pulse(root, `[data-runtime-name="${event.data?.name ?? ''}"]`)
const byContainer: AnimationHandler = (root, event) => {
  const selector = `[data-runtime-name="${event.data?.name ?? ''}"]`
  pulse(root, selector, '#f59e0b')
  pulsePointers(root, `${selector} [data-runtime-pointer]`)
}

export const animationHandlers: Partial<Record<BaseTraceEvent['type'], AnimationHandler>> = {
  variable_declare: byVariable,
  variable_assign: byVariable,
  variable_read: byVariable,
  array_declare: byContainer,
  array_read: byContainer,
  array_write: byContainer,
  array_2d_declare: byContainer,
  array_2d_read: byContainer,
  array_2d_write: byContainer,
  string_declare: byContainer,
  string_read: byContainer,
  string_write: byContainer,
  branch: (root) => pulse(root, '[data-runtime-role="branch"]', '#22c55e'),
  loop_condition: (root) => {
    pulse(root, '[data-runtime-role="loop"]', '#f59e0b')
    pulsePointers(root)
  },
  console_input: byContainer,
  console_output: (root) => pulse(root, '[data-runtime-role="console"]', '#22c55e')
}

export function animateTraceEvent(root: HTMLElement | null, event: BaseTraceEvent | null) {
  if (!root || !event) return
  animationHandlers[event.type]?.(root, event)
}
