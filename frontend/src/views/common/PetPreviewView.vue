<template>
  <main class="pet-preview">
    <header class="preview-header" data-pet-exclusion="preview-header">
      <div class="preview-brand"><span>C++</span><div><strong>互动宠物实验室</strong><small>仅开发环境可见</small></div></div>
      <el-button text @click="$router.push('/login')">返回登录页</el-button>
    </header>

    <section class="preview-hero">
      <div>
        <span class="preview-kicker">PET INTERACTION LAB</span>
        <h1>让学习反馈更有温度</h1>
        <p>点击下面的情境按钮检查宠物状态、气泡和粒子；也可以拖动宠物，验证边缘吸附与安全区域避让。</p>
      </div>
      <div class="preview-actions" data-pet-exclusion="preview-actions">
        <el-button type="primary" @click="pet.codeSuccess()">代码正确</el-button>
        <el-button type="success" @click="pet.exerciseComplete()">完成练习</el-button>
        <el-button type="warning" @click="pet.codeError()">遇到错误</el-button>
        <el-button @click="pet.thinking()">正在思考</el-button>
        <el-button @click="pet.sleep()">休息一下</el-button>
        <el-button @click="showTip">学习提示</el-button>
      </div>
    </section>

    <section class="preview-grid">
      <article class="preview-card">
        <span class="preview-card__label">今日任务</span>
        <h2>数组中的最大值</h2>
        <p>读入 n 个整数，输出其中最大的一个数。</p>
        <div class="preview-progress"><span style="width: 68%" /></div>
        <small>学习进度 68%</small>
      </article>
      <article class="preview-card preview-editor" data-pet-exclusion="preview-editor">
        <div class="preview-editor__bar"><span>main.cpp</span><span>C++17</span></div>
        <pre><code><b>#include</b> &lt;iostream&gt;
<b>using namespace</b> std;

<b>int</b> main() {
  <b>int</b> n;
  cin &gt;&gt; n;
  <span>// 从这里继续完成吧</span>
}</code></pre>
      </article>
    </section>
    <PetWidget debug />
  </main>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import PetWidget from '@/components/pet/PetWidget.vue'
import { usePet } from '@/composables/usePet'

const pet = usePet()
const showTip = () => pet.showMessage('先想一想：最大值变量应该用第几个数初始化？', { bubbleType: 'tip', duration: 5000 })
onMounted(() => pet.welcome('这是互动宠物实验室，来试试不同的学习情境吧！'))
</script>

<style scoped lang="scss">
.pet-preview {
  min-height: 100vh;
  padding: 28px clamp(18px, 4vw, 64px) 56px;
  color: var(--text-primary);
  background: var(--bg-gradient);
}

.preview-header,
.preview-brand,
.preview-actions,
.preview-editor__bar {
  display: flex;
  align-items: center;
}

.preview-header {
  justify-content: space-between;
  gap: 16px;
  padding: 12px 16px;
  background: var(--surface-glass);
  border: 1px solid var(--border-color);
  border-radius: 18px;
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(16px);
}

.preview-brand { gap: 10px; }
.preview-brand > span {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  color: var(--text-inverse);
  background: linear-gradient(135deg, var(--color-primary), var(--color-accent));
  border-radius: 14px;
  font-weight: 900;
}
.preview-brand strong,
.preview-brand small { display: block; }
.preview-brand small { margin-top: 2px; color: var(--text-muted); font-size: 11px; }

.preview-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.9fr);
  gap: 40px;
  align-items: end;
  max-width: 1180px;
  padding: 70px 0 38px;
  margin: 0 auto;
}
.preview-kicker { color: var(--color-primary); font-size: 12px; font-weight: 900; letter-spacing: .14em; }
.preview-hero h1 { max-width: 620px; margin: 12px 0; font-size: clamp(34px, 5vw, 58px); line-height: 1.08; }
.preview-hero p { max-width: 660px; margin: 0; color: var(--text-muted); font-size: 16px; line-height: 1.8; }
.preview-actions { flex-wrap: wrap; gap: 10px; justify-content: flex-end; }
.preview-actions .el-button { margin-left: 0; }

.preview-grid { display: grid; grid-template-columns: minmax(260px, .65fr) minmax(420px, 1.35fr); gap: 18px; max-width: 1180px; margin: 0 auto; }
.preview-card { padding: 24px; background: var(--surface-card); border: 1px solid var(--border-color); border-radius: 22px; box-shadow: var(--shadow-soft); }
.preview-card__label { color: var(--color-primary); font-size: 12px; font-weight: 900; }
.preview-card h2 { margin: 12px 0 8px; }
.preview-card p { color: var(--text-muted); line-height: 1.7; }
.preview-card small { color: var(--text-muted); }
.preview-progress { height: 12px; margin: 28px 0 8px; overflow: hidden; background: var(--surface-soft); border-radius: 999px; }
.preview-progress span { display: block; height: 100%; background: linear-gradient(90deg, var(--color-primary), var(--color-accent)); border-radius: inherit; }
.preview-editor { padding: 0; overflow: hidden; background: #101827; border-color: #24344e; }
.preview-editor__bar { justify-content: space-between; padding: 12px 16px; color: #a9bad1; background: #172237; font-size: 12px; font-weight: 800; }
.preview-editor pre { min-height: 310px; padding: 24px; margin: 0; color: #d5e1f2; font: 14px/1.8 Consolas, monospace; }
.preview-editor b { color: #78b9ff; }
.preview-editor code span { color: #7f94ad; }

@media (max-width: 820px) {
  .preview-hero,
  .preview-grid { grid-template-columns: 1fr; }
  .preview-hero { gap: 24px; padding-top: 44px; }
  .preview-actions { justify-content: flex-start; }
}

@media (max-width: 560px) {
  .pet-preview { padding: 14px 14px 90px; }
  .preview-brand small { display: none; }
  .preview-hero h1 { font-size: 36px; }
  .preview-card { padding: 18px; }
  .preview-editor { padding: 0; }
}
</style>
