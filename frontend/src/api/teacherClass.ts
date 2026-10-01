import request from '@/utils/request'
import type { ClassInfo, UserInfo } from './types'

export interface ClassPayload {
  className: string
  classTypeId: number
  description?: string
}

export function createClassApi(data: ClassPayload) {
  return request.post<ClassInfo, ClassInfo>('/teacher/classes', data)
}

export function listClassesApi() {
  return request.get<ClassInfo[], ClassInfo[]>('/teacher/classes')
}

export function updateClassApi(classId: number, data: ClassPayload) {
  return request.put<ClassInfo, ClassInfo>(`/teacher/classes/${classId}`, data)
}

export function deleteClassApi(classId: number) {
  return request.delete<void, void>(`/teacher/classes/${classId}`)
}

export function addStudentApi(classId: number, studentKeyword: string) {
  const keyword = studentKeyword.trim()
  const data = /^\d+$/.test(keyword) ? { studentId: Number(keyword) } : { username: keyword }
  return request.post<void, void>(`/teacher/classes/${classId}/students`, data)
}

export function listClassStudentsApi(classId: number) {
  return request.get<UserInfo[], UserInfo[]>(`/teacher/classes/${classId}/students`)
}

export function removeStudentApi(classId: number, studentId: number) {
  return request.delete<void, void>(`/teacher/classes/${classId}/students/${studentId}`)
}
