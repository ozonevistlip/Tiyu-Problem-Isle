<template>
  <main class="auth-layout">
    <div class="auth-theme">
      <ThemeSwitcher />
    </div>
    <section class="intro">
      <div class="star-field" />
      <div class="intro-content">
        <div class="brand-mark">C++</div>
        <p class="eyebrow">CppKid Learning Planet</p>
        <h1>进入你的 C++ 学习星球</h1>
        <p class="intro-copy">把课堂练习、闯关比赛和成长记录放在同一个清晰友好的空间里。</p>
        <div class="feature-row" aria-label="平台亮点">
          <span>即时反馈</span>
          <span>班级比赛</span>
          <span>成长进度</span>
        </div>
        <img src="/assets/cppkid-console.svg" alt="CppKid console" />
      </div>
    </section>
    <section class="auth-panel">
      <router-view />
    </section>
  </main>
</template>

<script setup lang="ts">
import ThemeSwitcher from '@/components/ThemeSwitcher.vue'
</script>

<style scoped lang="scss">
.auth-layout {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(380px, 460px);
  min-height: 100vh;
  overflow: hidden;
  background: var(--bg-gradient);
}

.auth-theme {
  position: absolute;
  top: 24px;
  right: 28px;
  z-index: 5;
}

.intro {
  position: relative;
  display: grid;
  align-content: center;
  min-height: 100vh;
  padding: 64px;
  overflow: hidden;
  color: var(--text-inverse);
  background:
    radial-gradient(circle at 22% 16%, color-mix(in srgb, var(--color-primary), transparent 36%), transparent 26%),
    radial-gradient(circle at 78% 18%, color-mix(in srgb, var(--color-accent), transparent 42%), transparent 22%),
    linear-gradient(135deg, #07152a 0%, color-mix(in srgb, var(--color-primary-strong), #111827 54%) 56%, #121327 100%);
}

.star-field,
.star-field::before,
.star-field::after {
  position: absolute;
  inset: 0;
  content: "";
  background-image:
    radial-gradient(circle, rgba(255, 255, 255, 0.86) 0 1px, transparent 1.5px),
    radial-gradient(circle, rgba(255, 255, 255, 0.45) 0 1px, transparent 1.5px);
  background-position: 0 0, 36px 52px;
  background-size: 92px 92px, 132px 132px;
  opacity: 0.38;
  animation: star-drift 24s linear infinite;
}

.star-field::before {
  filter: blur(1px);
  opacity: 0.28;
  transform: scale(1.08);
  animation-duration: 36s;
}

.star-field::after {
  background-image: radial-gradient(circle at 30% 35%, rgba(255, 255, 255, 0.2), transparent 22%),
    radial-gradient(circle at 70% 55%, color-mix(in srgb, var(--color-accent), transparent 70%), transparent 22%);
  opacity: 0.7;
  animation: nebula-breathe 8s ease-in-out infinite alternate;
}

.intro-content {
  position: relative;
  z-index: 1;
  max-width: 620px;
}

.brand-mark {
  display: grid;
  width: 72px;
  height: 72px;
  place-items: center;
  color: var(--text-inverse);
  background: linear-gradient(135deg, var(--color-primary), var(--color-accent));
  border: 1px solid rgba(255, 255, 255, 0.34);
  border-radius: 22px;
  box-shadow: 0 18px 44px color-mix(in srgb, var(--color-primary), transparent 58%);
  font-weight: 800;
}

.eyebrow {
  margin: 24px 0 8px;
  color: color-mix(in srgb, var(--color-accent), white 18%);
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

h1 {
  max-width: 560px;
  margin: 0;
  font-size: clamp(38px, 5vw, 68px);
  line-height: 1.06;
}

.intro-copy {
  max-width: 520px;
  margin: 18px 0 0;
  color: rgba(238, 246, 255, 0.8);
  font-size: 17px;
  line-height: 1.7;
}

.feature-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 24px;
}

.feature-row span {
  padding: 9px 13px;
  color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 999px;
  backdrop-filter: blur(10px);
  font-weight: 700;
}

img {
  width: min(360px, 86%);
  margin-top: 44px;
  filter: drop-shadow(0 24px 36px rgba(0, 0, 0, 0.26));
  animation: float-console 5s ease-in-out infinite;
}

.auth-panel {
  position: relative;
  z-index: 2;
  display: grid;
  align-items: center;
  padding: 96px 34px 48px;
  background:
    linear-gradient(180deg, rgba(var(--bg-page-rgb), 0.62), rgba(var(--bg-page-rgb), 0.9)),
    radial-gradient(circle at 50% 8%, color-mix(in srgb, var(--color-primary), transparent 84%), transparent 34%);
  backdrop-filter: blur(18px);
}

@keyframes star-drift {
  from {
    transform: translate3d(0, 0, 0);
  }

  to {
    transform: translate3d(-92px, 92px, 0);
  }
}

@keyframes nebula-breathe {
  from {
    transform: scale(1);
  }

  to {
    transform: scale(1.05);
  }
}

@keyframes float-console {
  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-10px);
  }
}

@media (max-width: 860px) {
  .auth-layout {
    grid-template-columns: 1fr;
  }

  .intro {
    min-height: 440px;
    padding: 92px 28px 36px;
  }

  .auth-theme {
    right: 18px;
  }

  .auth-panel {
    padding: 28px 18px 42px;
  }
}
</style>
