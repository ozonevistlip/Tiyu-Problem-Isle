import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { loginApi, logoutApi, meApi, registerApi, type LoginRequest, type RegisterRequest } from '@/api/auth'
import type { UserInfo, UserRole } from '@/api/types'
import { clearToken, getStoredUser, getToken, setStoredUser, setToken } from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken())
  const userInfo = ref<UserInfo | null>(getStoredUser())
  const role = computed<UserRole | ''>(() => userInfo.value?.role || '')
  const isLogin = computed(() => Boolean(token.value))

  function clearLocalSession() {
    token.value = ''
    userInfo.value = null
    clearToken()
  }

  window.addEventListener('cppkid-auth-expired', clearLocalSession)

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

  async function logout() {
    try {
      if (token.value) await logoutApi()
    } catch {
      // Local logout must still succeed if the session has already expired.
    } finally {
      clearLocalSession()
    }
  }

  return { token, userInfo, role, isLogin, login, register, fetchMe, logout, clearLocalSession }
})
