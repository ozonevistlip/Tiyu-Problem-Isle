<template>
  <div class="page">
    <PageHeader :title="problem?.title || '答题'" subtitle="左侧读题，右侧编写并提交 C++17 代码" back>
      <template #actions><ContestCountdown v-if="contest" :start-time="contest.startTime" :end-time="contest.endTime" /></template>
    </PageHeader>
    <div class="problem-layout">
      <section class="panel problem-content" v-loading="loading">
        <ProblemStatusTag :status="problem?.status" />
        <h2>{{ problem?.title }}</h2>
        <p>{{ problem?.description }}</p>
        <h3>输入格式</h3><div class="code-block">{{ problem?.inputFormat || '-' }}</div>
        <h3>输出格式</h3><div class="code-block">{{ problem?.outputFormat || '-' }}</div>
        <h3>样例输入</h3><div class="code-block">{{ problem?.sampleInput || '-' }}</div>
        <h3>样例输出</h3><div class="code-block">{{ problem?.sampleOutput || '-' }}</div>
        <HintPanel
          v-if="hint"
          :can-show-hint="hint.canShowHint"
          :unlock-remain-seconds="hint.unlockRemainSeconds"
          :hint-title="hint.hintTitle"
          :hint-content="hint.hintContent"
          :hint-level="hint.hintLevel"
          :problem-status="problem?.status"
          :contest-ended="contestEnded"
          @unlock="loadHint"
        />
      </section>
      <section class="panel code-side">
        <div class="toolbar">
          <el-select model-value="cpp17" disabled><el-option label="C++17" value="cpp17" /></el-select>
          <el-button type="primary" :loading="submitting" @click="submit">提交代码</el-button>
        </div>
        <CodeEditor v-model="code" height="500px" />
        <div class="custom-test-panel">
          <div class="custom-test-header">
            <strong>自测运行</strong>
            <div class="custom-test-actions">
              <el-button size="small" @click="loadSampleInput">载入样例输入</el-button>
              <el-button size="small" type="primary" :loading="runningCustomTest" @click="runCustomTest">运行自测</el-button>
            </div>
          </div>
          <div class="custom-test-grid">
            <el-form-item label="自测输入">
              <el-input
                v-model="customInput"
                type="textarea"
                :rows="7"
                resize="vertical"
                placeholder="在这里输入程序的 stdin 内容"
              />
            </el-form-item>
            <el-form-item label="运行输出">
              <el-input
                v-model="customOutput"
                type="textarea"
                :rows="7"
                resize="vertical"
                readonly
                placeholder="运行结果会显示在这里"
              />
            </el-form-item>
          </div>
          <div v-if="customTestResult" class="custom-test-result">
            <el-tag :type="customStatusType">{{ customTestResult.status }}</el-tag>
            <span>耗时：{{ customTestResult.timeUsedMs }} ms</span>
            <span v-if="customTestResult.stderr" class="custom-test-error">stderr：{{ customTestResult.stderr }}</span>
            <span v-if="customTestResult.errorMessage" class="custom-test-error">{{ customTestResult.errorMessage }}</span>
          </div>
        </div>
        <div v-if="latest" class="result-card">
          <SubmissionStatusTag :status="latest.status" />
          <span>分数：{{ latest.score }}</span>
          <span>耗时：{{ latest.timeUsedMs }} ms</span>
          <span v-if="latest.errorMessage" class="error-message">{{ latest.errorMessage }}</span>
        </div>
        <el-table :data="submissions" size="small">
          <el-table-column prop="id" label="提交" width="90" />
          <el-table-column label="状态" width="130"><template #default="{ row }"><SubmissionStatusTag :status="row.status" /></template></el-table-column>
          <el-table-column prop="score" label="分数" width="80" />
          <el-table-column label="时间"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></el-table-column>
        </el-table>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import ContestCountdown from '@/components/ContestCountdown.vue'
import ProblemStatusTag from '@/components/ProblemStatusTag.vue'
import HintPanel from '@/components/HintPanel.vue'
import CodeEditor from '@/components/CodeEditor.vue'
import SubmissionStatusTag from '@/components/SubmissionStatusTag.vue'
import { useCodeVisualizerStore } from '@/stores/codeVisualizer'
import { getHintApi, getStudentContestApi, getStudentProblemApi, runCustomTestApi, studentSubmissionsApi, submitCodeApi } from '@/api/studentContest'
import { getSubmissionApi } from '@/api/submission'
import type { ContestInfo, ContestProblemInfo, CustomTestResult, HintView, SubmissionInfo } from '@/api/types'
import { contestPhase, formatDateTime } from '@/utils/time'

const route = useRoute()
const codeVisualizer = useCodeVisualizerStore()
const contestId = computed(() => Number(route.params.contestId))
const problemId = computed(() => Number(route.params.problemId))
const loading = ref(false)
const submitting = ref(false)
const contest = ref<ContestInfo | null>(null)
const problem = ref<ContestProblemInfo | null>(null)
const hint = ref<HintView | null>(null)
const submissions = ref<SubmissionInfo[]>([])
const latest = ref<SubmissionInfo | null>(null)
const code = ref('#include <iostream>\nusing namespace std;\n\nint main() {\n    \n    return 0;\n}\n')
const customInput = ref('')
const customOutput = ref('')
const customTestResult = ref<CustomTestResult | null>(null)
const runningCustomTest = ref(false)
let pollTimer = 0

watch(code, (value) => codeVisualizer.setCurrentCode(value), { immediate: true })

const contestEnded = computed(() => (contest.value ? contestPhase(contest.value.startTime, contest.value.endTime) === 'ended' : false))
const customStatusType = computed(() => {
  if (!customTestResult.value) return 'info'
  if (customTestResult.value.status === 'FINISHED') return 'success'
  if (customTestResult.value.status === 'COMPILE_ERROR' || customTestResult.value.status === 'RUNTIME_ERROR') return 'danger'
  return 'warning'
})

async function loadHint() {
  hint.value = await getHintApi(contestId.value, problemId.value)
}
async function load() {
  loading.value = true
  try {
    const [contestInfo, problemInfo, submitList] = await Promise.all([
      getStudentContestApi(contestId.value),
      getStudentProblemApi(contestId.value, problemId.value),
      studentSubmissionsApi(contestId.value)
    ])
    contest.value = contestInfo
    problem.value = problemInfo
    submissions.value = submitList.filter((item) => item.problemId === problemId.value)
    latest.value = submissions.value[0] || null
    if (!customInput.value && problemInfo.sampleInput) customInput.value = problemInfo.sampleInput
    await loadHint()
  } finally {
    loading.value = false
  }
}

function loadSampleInput() {
  customInput.value = problem.value?.sampleInput || ''
}

async function runCustomTest() {
  runningCustomTest.value = true
  customTestResult.value = null
  customOutput.value = ''
  try {
    const result = await runCustomTestApi(contestId.value, problemId.value, code.value, customInput.value)
    customTestResult.value = result
    customOutput.value = result.stdout || ''
    if (result.status === 'FINISHED') {
      ElMessage.success('自测运行完成')
    }
  } catch {
    // The request interceptor has already shown the server message.
  } finally {
    runningCustomTest.value = false
  }
}
async function submit() {
  submitting.value = true
  try {
    latest.value = await submitCodeApi(contestId.value, problemId.value, code.value)
    ElMessage.success('提交成功，正在判题')
    startPolling(latest.value.id)
  } catch {
    // The request interceptor has already shown the server message.
  } finally {
    submitting.value = false
  }
}
function startPolling(submissionId: number) {
  window.clearInterval(pollTimer)
  pollTimer = window.setInterval(async () => {
    try {
      const result = await getSubmissionApi(submissionId)
      latest.value = result
      if (!['PENDING', 'JUDGING'].includes(result.status)) {
        window.clearInterval(pollTimer)
        await load()
        if (result.status === 'ACCEPTED') await loadHint()
      }
    } catch {
      window.clearInterval(pollTimer)
    }
  }, 1000)
}
onMounted(load)
onUnmounted(() => window.clearInterval(pollTimer))
</script>

<style scoped lang="scss">
.problem-layout {
  display: grid;
  grid-template-columns: minmax(320px, 45%) minmax(420px, 55%);
  gap: 16px;
}

.problem-content {
  display: grid;
  gap: 12px;
  align-content: start;
}

.code-side {
  display: grid;
  gap: 14px;
  min-width: 0;
}

.result-card {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  padding: 12px;
  background: #f7f8fa;
  border: 1px solid #d8e2ec;
  border-radius: 8px;
}

.custom-test-panel {
  display: grid;
  gap: 12px;
  padding: 12px;
  border: 1px solid #d8e2ec;
  border-radius: 8px;
  background: #fbfcfe;
}

.custom-test-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.custom-test-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.custom-test-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.custom-test-result {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: flex-start;
  line-height: 1.5;
}

.custom-test-error {
  flex-basis: 100%;
  color: #c45656;
  white-space: pre-wrap;
}

.error-message {
  flex-basis: 100%;
  color: #c45656;
  line-height: 1.5;
  white-space: pre-wrap;
}

@media (max-width: 1020px) {
  .problem-layout {
    grid-template-columns: 1fr;
  }

  .custom-test-grid {
    grid-template-columns: 1fr;
  }
}
</style>
