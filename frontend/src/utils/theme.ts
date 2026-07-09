import { computed, ref } from 'vue'

export type ThemeName = 'blue' | 'red' | 'orange' | 'pink' | 'dark'

export interface ThemeOption {
  name: ThemeName
  label: string
  tone: string
}

const STORAGE_KEY = 'cppkid-theme'

export const themeOptions: ThemeOption[] = [
  { name: 'blue', label: '蓝色', tone: '科技学习' },
  { name: 'red', label: '红色', tone: '挑战竞赛' },
  { name: 'orange', label: '橙色', tone: '活力成长' },
  { name: 'pink', label: '粉色', tone: '轻松可爱' },
  { name: 'dark', label: '暗夜', tone: '沉浸护眼' }
]

const names = themeOptions.map((item) => item.name)
const currentTheme = ref<ThemeName>(resolveStoredTheme())

function isThemeName(value: string | null): value is ThemeName {
  return Boolean(value && names.includes(value as ThemeName))
}

function resolveStoredTheme(): ThemeName {
  if (typeof window === 'undefined') return 'blue'
  return isThemeName(window.localStorage.getItem(STORAGE_KEY)) ? (window.localStorage.getItem(STORAGE_KEY) as ThemeName) : 'blue'
}

export function applyTheme(theme: ThemeName) {
  currentTheme.value = theme
  if (typeof document !== 'undefined') {
    document.documentElement.dataset.theme = theme
  }
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(STORAGE_KEY, theme)
  }
}

export function initTheme() {
  applyTheme(currentTheme.value)
}

export function useTheme() {
  const activeTheme = computed(() => themeOptions.find((item) => item.name === currentTheme.value) || themeOptions[0])
  return {
    activeTheme,
    currentTheme,
    themeOptions,
    setTheme: applyTheme
  }
}
