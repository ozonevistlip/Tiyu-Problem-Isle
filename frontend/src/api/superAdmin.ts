import request from '@/utils/request'
import type { AdminDashboard, Announcement, OnlineSession, PageResult, UserInfo, UserRole } from './types'

export interface UserQuery {
  page: number
  size: number
  keyword?: string
  role?: string
  status?: number
}

export interface UserPayload {
  username: string
  password?: string
  role: Exclude<UserRole, 'SUPER_ADMIN'>
  realName?: string
  nickname?: string
}

export interface AnnouncementPayload {
  title: string
  content: string
  status: number
}

export const dashboardApi = () => request.get<AdminDashboard, AdminDashboard>('/super-admin/dashboard')
export const usersApi = (params: UserQuery) => request.get<PageResult<UserInfo>, PageResult<UserInfo>>('/super-admin/users', { params })
export const userDetailApi = (id: number) => request.get<UserInfo, UserInfo>(`/super-admin/users/${id}`)
export const createUserApi = (data: UserPayload) => request.post<UserInfo, UserInfo>('/super-admin/users', data)
export const updateUserApi = (id: number, data: Omit<UserPayload, 'password'>) => request.put<UserInfo, UserInfo>(`/super-admin/users/${id}`, data)
export const updateUserStatusApi = (id: number, status: number) => request.put<void, void>(`/super-admin/users/${id}/status`, { status })
export const resetPasswordApi = (id: number, password: string) => request.put<void, void>(`/super-admin/users/${id}/password`, { password })
export const forceLogoutUserApi = (id: number) => request.post<void, void>(`/super-admin/users/${id}/force-logout`)
export const deleteUserApi = (id: number) => request.delete<void, void>(`/super-admin/users/${id}`)
export const onlineUsersApi = () => request.get<OnlineSession[], OnlineSession[]>('/super-admin/online-users')
export const terminateSessionApi = (id: string) => request.delete<void, void>(`/super-admin/sessions/${id}`)
export const settingsApi = () => request.get<{ registrationEnabled: boolean }, { registrationEnabled: boolean }>('/super-admin/settings')
export const updateRegistrationApi = (enabled: boolean) => request.put<void, void>('/super-admin/settings/registration', { enabled })
export const announcementsApi = () => request.get<Announcement[], Announcement[]>('/super-admin/announcements')
export const createAnnouncementApi = (data: AnnouncementPayload) => request.post<Announcement, Announcement>('/super-admin/announcements', data)
export const updateAnnouncementApi = (id: number, data: AnnouncementPayload) => request.put<Announcement, Announcement>(`/super-admin/announcements/${id}`, data)
export const deleteAnnouncementApi = (id: number) => request.delete<void, void>(`/super-admin/announcements/${id}`)
