import axios, { type AxiosError, type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { clearToken, getToken } from './auth'
import type { ApiResult } from '@/api/types'

const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000
})

instance.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

const unwrapResponse = (response: AxiosResponse): unknown => {
  const result = response.data as ApiResult<unknown>
  if (result.code !== 0) {
    ElMessage.error(result.message || '请求失败')
    if (result.code === 40001) {
      clearToken()
      window.dispatchEvent(new Event('cppkid-auth-expired'))
      void router.replace('/login')
    }
    return Promise.reject(new Error(result.message || '请求失败'))
  }
  return result.data
}

instance.interceptors.response.use(
  unwrapResponse as unknown as (value: AxiosResponse) => AxiosResponse | Promise<AxiosResponse>,
  (error: AxiosError) => {
    const message = error.response?.status === 401 ? '登录已过期' : error.message || '网络请求失败'
    ElMessage.error(message)
    if (error.response?.status === 401) {
      clearToken()
      window.dispatchEvent(new Event('cppkid-auth-expired'))
      void router.replace('/login')
    }
    return Promise.reject(error)
  }
)

interface HttpClient {
  get<T = unknown, R = T>(url: string, config?: AxiosRequestConfig): Promise<R>
  post<T = unknown, R = T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<R>
  put<T = unknown, R = T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<R>
  delete<T = unknown, R = T>(url: string, config?: AxiosRequestConfig): Promise<R>
}

const request = instance as unknown as HttpClient

export default request
