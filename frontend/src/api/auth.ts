import request from '@/utils/request'
import type { LoginResponse, UserInfo, UserRole } from './types'

export interface RegisterRequest {
  username: string
  password: string
  role: UserRole
  realName?: string
  nickname?: string
}

export interface LoginRequest {
  username: string
  password: string
}

export function registerApi(data: RegisterRequest) {
  return request.post<UserInfo, UserInfo>('/auth/register', data)
}

export function loginApi(data: LoginRequest) {
  return request.post<LoginResponse, LoginResponse>('/auth/login', data)
}

export function meApi() {
  return request.get<UserInfo, UserInfo>('/auth/me')
}

export function logoutApi() {
  return request.post<void, void>('/auth/logout')
}
