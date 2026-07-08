import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { ContestInfo, ContestProblemInfo } from '@/api/types'
import { getStudentContestApi, getStudentProblemApi } from '@/api/studentContest'
import { contestPhase } from '@/utils/time'

export const useContestStore = defineStore('contest', () => {
  const currentContest = ref<ContestInfo | null>(null)
  const currentProblem = ref<ContestProblemInfo | null>(null)
  const contestStatus = computed(() => {
    if (!currentContest.value) return 'unknown'
    return contestPhase(currentContest.value.startTime, currentContest.value.endTime)
  })

  async function refreshContest(contestId: number) {
    currentContest.value = await getStudentContestApi(contestId)
    return currentContest.value
  }

  async function refreshProblem(contestId: number, problemId: number) {
    currentProblem.value = await getStudentProblemApi(contestId, problemId)
    return currentProblem.value
  }

  return { currentContest, currentProblem, contestStatus, refreshContest, refreshProblem }
})
