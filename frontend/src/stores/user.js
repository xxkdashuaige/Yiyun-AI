import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi } from '@/api/auth'

function parseUserInfo() {
  try {
    const raw = localStorage.getItem('yy_user')
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('yy_token') || '')
  const userInfo = ref(parseUserInfo())

  const isLoggedIn = computed(() => !!token.value)

  function setAuth(data) {
    token.value = data.token
    userInfo.value = {
      userId: data.userId,
      username: data.username,
      nickname: data.nickname,
      avatarUrl: data.avatarUrl,
    }
    localStorage.setItem('yy_token', data.token)
    localStorage.setItem('yy_user', JSON.stringify(userInfo.value))
  }

  async function login(username, password) {
    const data = await loginApi(username, password)
    setAuth(data)
    return data
  }

  async function register({ username, password, confirmPassword, nickname }) {
    const data = await registerApi({ username, password, confirmPassword, nickname })
    setAuth(data)
    return data
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('yy_token')
    localStorage.removeItem('yy_user')
  }

  return { token, userInfo, isLoggedIn, login, register, logout }
})