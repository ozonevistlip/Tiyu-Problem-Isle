<template>
  <main class="auth-layout">
    <section class="intro">
      <div class="brand-mark">C++</div>
      <h1>C++ 课堂 OJ</h1>
      <p>为老师的小比赛和学生的练习流程准备的轻量教学平台。</p>
      <img src="/assets/cppkid-console.svg" alt="CppKid console" />
    </section>
    <router-view />
  </main>
</template>

<style scoped lang="scss">
.auth-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  min-height: 100vh;
  background: #f5f7fa;
}

.intro {
  display: grid;
  align-content: center;
  padding: 56px;
  color: #fff;
  background: #17324d;
}

.brand-mark {
  display: grid;
  width: 64px;
  height: 64px;
  place-items: center;
  background: #2878d8;
  border: 4px solid #f59e0b;
  border-radius: 8px;
  font-weight: 800;
}

h1 {
  margin: 20px 0 10px;
  font-size: 42px;
}

p {
  max-width: 520px;
  color: #d9e8f6;
  line-height: 1.7;
}

img {
  width: min(320px, 100%);
  margin-top: 32px;
}

@media (max-width: 860px) {
  .auth-layout {
    grid-template-columns: 1fr;
  }

  .intro {
    min-height: 320px;
    padding: 32px;
  }
}
</style>
