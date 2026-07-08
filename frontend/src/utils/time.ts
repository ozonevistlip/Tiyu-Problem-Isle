export function formatDateTime(value?: string): string {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

export function secondsUntil(value?: string): number {
  if (!value) return 0
  return Math.max(0, Math.ceil((new Date(value).getTime() - Date.now()) / 1000))
}

export function formatDuration(totalSeconds: number): string {
  const seconds = Math.max(0, Math.floor(totalSeconds))
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  if (h > 0) return `${h}时${m}分${s}秒`
  if (m > 0) return `${m}分${s}秒`
  return `${s}秒`
}

export function contestPhase(startTime: string, endTime: string): 'upcoming' | 'running' | 'ended' {
  const now = Date.now()
  if (now < new Date(startTime).getTime()) return 'upcoming'
  if (now > new Date(endTime).getTime()) return 'ended'
  return 'running'
}
