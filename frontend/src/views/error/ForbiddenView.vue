<template>
  <main class="error-page">
    <el-result icon="warning" title="403" sub-title="当前账号没有访问这个页面的权限">
      <template #extra>
        <el-button type="primary" @click="$router.back()">返回</el-button>
      </template>
    </el-result>
  </main>
</template>

<style scoped>
.error-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
}
</style>
