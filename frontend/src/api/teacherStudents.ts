import request from '@/utils/request'
import type { UserInfo } from './types'
import type { Tier } from './learning'

export interface StudentPayload { username: string; password?: string; realName?: string; nickname?: string }
export interface LessonTier { classId: number; lessonId: number; studentId: number; tier: Tier }
export const listTeacherStudentsApi = () => request.get<UserInfo[], UserInfo[]>('/teacher/students')
export const createTeacherStudentApi = (data: StudentPayload) => request.post<UserInfo, UserInfo>('/teacher/students', data)
export const updateTeacherStudentApi = (id: number, data: Omit<StudentPayload, 'password'>) => request.put<UserInfo, UserInfo>(`/teacher/students/${id}`, data)
export const resetTeacherStudentPasswordApi = (id: number, password: string) => request.put<void, void>(`/teacher/students/${id}/password`, { password })
export const deleteTeacherStudentApi = (id: number) => request.delete<void, void>(`/teacher/students/${id}`)
export const lessonTiersApi = (classId: number) => request.get<LessonTier[], LessonTier[]>(`/teacher/students/classes/${classId}/lesson-tiers`)
export const setLessonTierApi = (classId: number, lessonId: number, studentId: number, tier: Tier) => request.put<void, void>(`/teacher/students/classes/${classId}/lessons/${lessonId}/students/${studentId}/tier`, { tier })
