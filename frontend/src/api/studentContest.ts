import request from '@/utils/request'
import type { ContestInfo, ContestProblemInfo, CustomTestResult, HintView, RankInfo, SubmissionInfo } from './types'

export function listStudentContestsApi() {
  return request.get<ContestInfo[], ContestInfo[]>('/student/contests')
}

export function getStudentContestApi(contestId: number) {
  return request.get<ContestInfo, ContestInfo>(`/student/contests/${contestId}`)
}

export function listStudentContestProblemsApi(contestId: number) {
  return request.get<ContestProblemInfo[], ContestProblemInfo[]>(`/student/contests/${contestId}/problems`)
}

export function getStudentProblemApi(contestId: number, problemId: number) {
  return request.get<ContestProblemInfo, ContestProblemInfo>(`/student/contests/${contestId}/problems/${problemId}`)
}

export function getHintApi(contestId: number, problemId: number) {
  return request.get<HintView, HintView>(`/student/contests/${contestId}/problems/${problemId}/hint`)
}

export function submitCodeApi(contestId: number, problemId: number, code: string) {
  return request.post<SubmissionInfo, SubmissionInfo>(`/student/contests/${contestId}/problems/${problemId}/submit`, {
    language: 'cpp17',
    code
  })
}

export function runCustomTestApi(contestId: number, problemId: number, code: string, input: string) {
  return request.post<CustomTestResult, CustomTestResult>(
    `/student/contests/${contestId}/problems/${problemId}/run`,
    {
      language: 'cpp17',
      code,
      input
    },
    { timeout: 30000 }
  )
}

export function studentSubmissionsApi(contestId: number) {
  return request.get<SubmissionInfo[], SubmissionInfo[]>(`/student/contests/${contestId}/submissions`)
}

export function studentRankApi(contestId: number) {
  return request.get<RankInfo[], RankInfo[]>(`/student/contests/${contestId}/rank`)
}
