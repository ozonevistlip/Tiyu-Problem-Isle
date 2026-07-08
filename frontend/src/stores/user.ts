import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { loginApi, meApi, registerApi, type LoginRequest, type RegisterRequest } from '@/api/auth'
import type { UserInfo, UserRole } from '@/api/types'
import { clearToken, getStoredUser, getToken, setStoredUser, setToken } from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken())
  const userInfo = ref<UserInfo | null>(getStoredUser())
  const role = computed<UserRole | ''>(() => userInfo.value?.role || '')
  const isLogin = computed(() => Boolean(token.value))

  async function login(payload: LoginRequest) {
    const result = await loginApi(payload)
    token.value = result.token
    userInfo.value = result.user
    setToken(result.token)
    setStoredUser(result.user)
    return result.user
  }

  async function register(payload: RegisterRequest) {
    return registerApi(payload)
  }

  async function fetchMe() {
    const user = await meApi()
    userInfo.value = user
    setStoredUser(user)
    return user
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    clearToken()
  }

  return { token, userInfo, role, isLogin, login, register, fetchMe, logout }
})
