<template>
  <el-dropdown trigger="click" @command="setTheme">
    <button class="theme-switcher" type="button" :aria-label="`当前主题：${activeTheme.label}`">
      <span class="theme-dot" />
      <span class="theme-copy">
        <strong>{{ activeTheme.label }}主题</strong>
        <small>{{ activeTheme.tone }}</small>
      </span>
    </button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item v-for="theme in themeOptions" :key="theme.name" :command="theme.name">
          <span class="theme-menu-item" :data-theme-preview="theme.name">
            <span class="preview-dot" />
            <span>
              <strong>{{ theme.label }}主题</strong>
              <small>{{ theme.tone }}</small>
            </span>
          </span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { useTheme } from '@/utils/theme'

const { activeTheme, themeOptions, setTheme } = useTheme()
</script>

<style scoped lang="scss">
.theme-switcher {
  display: inline-flex;
  gap: 10px;
  align-items: center;
  min-width: 132px;
  padding: 8px 12px;
  color: var(--text-primary);
  cursor: pointer;
  background: var(--surface-glass);
  border: 1px solid var(--border-soft);
  border-radius: 999px;
  box-shadow: var(--shadow-soft);
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.theme-switcher:hover {
  background: var(--surface-raised);
  border-color: var(--color-primary);
  box-shadow: var(--shadow-hover);
  transform: translateY(-1px);
}

.theme-switcher:active {
  transform: translateY(0) scale(0.98);
}

.theme-dot,
.preview-dot {
  width: 16px;
  height: 16px;
  flex: 0 0 auto;
  background: linear-gradient(135deg, var(--color-primary), var(--color-accent));
  border: 2px solid color-mix(in srgb, var(--surface-raised), transparent 15%);
  border-radius: 999px;
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--color-primary), transparent 86%);
}

.theme-copy,
.theme-menu-item,
.theme-menu-item > span:last-child {
  display: grid;
  gap: 2px;
}

.theme-copy {
  text-align: left;
}

strong {
  font-size: 13px;
  line-height: 1.1;
}

small {
  color: var(--text-muted);
  font-size: 11px;
  line-height: 1.1;
}

.theme-menu-item {
  display: flex;
  gap: 10px;
  align-items: center;
  min-width: 148px;
}

.theme-menu-item[data-theme-preview='red'] .preview-dot {
  background: linear-gradient(135deg, #ef4444, #f97316);
}

.theme-menu-item[data-theme-preview='orange'] .preview-dot {
  background: linear-gradient(135deg, #f97316, #facc15);
}

.theme-menu-item[data-theme-preview='pink'] .preview-dot {
  background: linear-gradient(135deg, #ec4899, #a855f7);
}

.theme-menu-item[data-theme-preview='dark'] .preview-dot {
  background: linear-gradient(135deg, #38bdf8, #8b5cf6);
}
</style>
