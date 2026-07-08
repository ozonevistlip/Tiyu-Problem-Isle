import request from '@/utils/request'
import type { HintInfo, ProblemInfo, TestcaseInfo } from './types'

export interface ProblemPayload {
  title: string
  description: string
  inputFormat?: string
  outputFormat?: string
  sampleInput?: string
  sampleOutput?: string
  difficulty?: string
  timeLimitMs?: number
  memoryLimitMb?: number
  compareMode?: string
  visibility?: string
}

export interface TestcasePayload {
  inputData: string
  outputData: string
  score?: number
  sortOrder?: number
  sample?: boolean
}

export interface HintPayload {
  hintTitle?: string
  hintContent: string
  hintLevel?: number
}

export function createProblemApi(data: ProblemPayload) {
  return request.post<ProblemInfo, ProblemInfo>('/teacher/problems', data)
}

export function listProblemsApi() {
  return request.get<ProblemInfo[], ProblemInfo[]>('/teacher/problems')
}

export function getProblemApi(problemId: number) {
  return request.get<ProblemInfo, ProblemInfo>(`/teacher/problems/${problemId}`)
}

export function updateProblemApi(problemId: number, data: ProblemPayload) {
  return request.put<ProblemInfo, ProblemInfo>(`/teacher/problems/${problemId}`, data)
}

export function deleteProblemApi(problemId: number) {
  return request.delete<void, void>(`/teacher/problems/${problemId}`)
}

export function createTestcaseApi(problemId: number, data: TestcasePayload) {
  return request.post<TestcaseInfo, TestcaseInfo>(`/teacher/problems/${problemId}/testcases`, data)
}

export function listTestcasesApi(problemId: number) {
  return request.get<TestcaseInfo[], TestcaseInfo[]>(`/teacher/problems/${problemId}/testcases`)
}

export function updateTestcaseApi(testcaseId: number, data: TestcasePayload) {
  return request.put<TestcaseInfo, TestcaseInfo>(`/teacher/testcases/${testcaseId}`, data)
}

export function deleteTestcaseApi(testcaseId: number) {
  return request.delete<void, void>(`/teacher/testcases/${testcaseId}`)
}

export function createHintApi(problemId: number, data: HintPayload) {
  return request.post<HintInfo, HintInfo>(`/teacher/problems/${problemId}/hints`, data)
}

export function listHintsApi(problemId: number) {
  return request.get<HintInfo[], HintInfo[]>(`/teacher/problems/${problemId}/hints`)
}

export function updateHintApi(hintId: number, data: HintPayload) {
  return request.put<HintInfo, HintInfo>(`/teacher/hints/${hintId}`, data)
}

export function deleteHintApi(hintId: number) {
  return request.delete<void, void>(`/teacher/hints/${hintId}`)
}
