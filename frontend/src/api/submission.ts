import request from '@/utils/request'
import type { SubmissionCaseInfo, SubmissionInfo } from './types'

export function getSubmissionApi(submissionId: number) {
  return request.get<SubmissionInfo, SubmissionInfo>(`/submissions/${submissionId}`)
}

export function getSubmissionCasesApi(submissionId: number) {
  return request.get<SubmissionCaseInfo[], SubmissionCaseInfo[]>(`/submissions/${submissionId}/cases`)
}
