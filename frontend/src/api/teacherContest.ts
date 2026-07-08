import request from '@/utils/request'
import type { ContestInfo, ContestProblemInfo, RankInfo, SubmissionInfo } from './types'

export interface ContestPayload {
  classId: number
  title: string
  description?: string
  startTime: string
  endTime: string
  showRank?: boolean
  allowSubmitAfterEnd?: boolean
}

export interface AddContestProblemPayload {
  problemId: number
  displayOrder?: number
  score?: number
  hintUnlockMinutes?: number
  showHintAfterAc?: boolean
  showHintAfterContest?: boolean
}

export function createContestApi(data: ContestPayload) {
  return request.post<ContestInfo, ContestInfo>('/teacher/contests', data)
}

export function listTeacherContestsApi() {
  return request.get<ContestInfo[], ContestInfo[]>('/teacher/contests')
}

export function getTeacherContestApi(contestId: number) {
  return request.get<ContestInfo, ContestInfo>(`/teacher/contests/${contestId}`)
}

export function updateContestApi(contestId: number, data: ContestPayload) {
  return request.put<ContestInfo, ContestInfo>(`/teacher/contests/${contestId}`, data)
}

export function addContestProblemApi(contestId: number, data: AddContestProblemPayload) {
  return request.post<ContestProblemInfo, ContestProblemInfo>(`/teacher/contests/${contestId}/problems`, data)
}

export function deleteContestProblemApi(contestId: number, problemId: number) {
  return request.delete<void, void>(`/teacher/contests/${contestId}/problems/${problemId}`)
}

export function publishContestApi(contestId: number) {
  return request.post<void, void>(`/teacher/contests/${contestId}/publish`)
}

export function teacherRankApi(contestId: number) {
  return request.get<RankInfo[], RankInfo[]>(`/teacher/contests/${contestId}/rank`)
}

export function teacherSubmissionsApi(contestId: number) {
  return request.get<SubmissionInfo[], SubmissionInfo[]>(`/teacher/contests/${contestId}/submissions`)
}
