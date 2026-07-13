<template>
  <div class="page">
    <PageHeader title="代码可视化" subtitle="把 C++ 数组变成看得见的小格子" />

    <div class="visualizer-page-layout">
      <section class="panel visualizer-code-panel">
        <div class="panel-head">
          <div>
            <strong>C++ 代码</strong>
            <span>可以在这里临时修改，不会影响答题页编辑器</span>
          </div>
          <div class="panel-actions">
            <el-button :icon="RefreshRight" @click="resetExampleCode">重置示例代码</el-button>
            <el-button type="primary" :icon="DataAnalysis" @click="generateVisualization">生成可视化</el-button>
          </div>
        </div>
        <CodeEditor v-model="draftCode" height="520px" />
      </section>

      <section class="panel visualizer-result-panel">
        <div class="result-heading">
          <span class="result-kicker">数组可视化</span>
          <h3>把 C++ 数组变成看得见的小格子</h3>
        </div>

        <ArrayVisualizer
          v-if="parsedArray"
          :array-name="parsedArray.arrayName"
          :length="parsedArray.length"
          :values="parsedArray.values"
          :initialized-count="parsedArray.initializedCount"
        />
        <el-empty
          v-else
          class="visualizer-empty"
          description="暂时没有找到可以可视化的 int 数组，可以试试 int arr[5] = {1,2,3};"
        />
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { DataAnalysis, RefreshRight } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import CodeEditor from '@/components/CodeEditor.vue'
import ArrayVisualizer from '@/components/code-visualizer/ArrayVisualizer.vue'
import { useCodeVisualizerStore } from '@/stores/codeVisualizer'
import { parseFirstIntArray, type ParsedArray } from '@/utils/codeVisualizer'

const EXAMPLE_CODE = `#include <iostream>
using namespace std;

int main() {
    int arr[10] = {5, 9, 5, 3, 3, -1};
    return 0;
}
`

const codeVisualizer = useCodeVisualizerStore()
const draftCode = ref('')
const parsedArray = ref<ParsedArray | null>(null)

function generateVisualization() {
  parsedArray.value = parseFirstIntArray(draftCode.value)
}

function resetExampleCode() {
  draftCode.value = EXAMPLE_CODE
  generateVisualization()
}

onMounted(() => {
  draftCode.value = codeVisualizer.currentCode || EXAMPLE_CODE
  generateVisualization()
})
</script>

<style scoped lang="scss">
.visualizer-page-layout {
  display: grid;
  grid-template-columns: minmax(360px, 48%) minmax(360px, 52%);
  gap: 16px;
}

.visualizer-code-panel,
.visualizer-result-panel {
  display: grid;
  min-width: 0;
  align-content: start;
  gap: 14px;
}

.visualizer-result-panel {
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--color-primary), transparent 94%), transparent 42%),
    var(--surface-card);
}

.panel-head {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
}

.panel-head strong {
  display: block;
  color: var(--text-primary);
  font-size: 16px;
}

.panel-head span,
.result-heading h3 {
  color: var(--text-muted);
}

.panel-head span {
  display: block;
  margin-top: 4px;
  font-size: 12px;
}

.panel-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.result-heading {
  display: grid;
  gap: 6px;
}

.result-kicker {
  width: fit-content;
  padding: 6px 10px;
  color: var(--color-primary);
  background: color-mix(in srgb, var(--color-primary), transparent 88%);
  border: 1px solid color-mix(in srgb, var(--color-primary), transparent 58%);
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
}

.result-heading h3 {
  margin: 0;
  font-size: 15px;
  line-height: 1.5;
}

.visualizer-empty {
  min-height: 360px;
  background: var(--surface-soft);
  border: 1px dashed var(--border-color);
  border-radius: 14px;
}

@media (max-width: 1080px) {
  .visualizer-page-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .panel-head {
    align-items: stretch;
    flex-direction: column;
  }

  .panel-actions {
    justify-content: flex-start;
  }
}
</style>
