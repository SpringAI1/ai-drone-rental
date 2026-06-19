import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, register, adminLogin, getCurrentUser, getAdminInfo } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(null)
  const isAdmin = ref(localStorage.getItem('isAdmin') === 'true')
  const isInitializing = ref(false)
  const fetchPromise = ref(null)

  const isLoggedIn = computed(() => !!token.value)
  const isVerified = computed(() => user.value?.status === 1)
  const canRent = computed(() => isLoggedIn.value && isVerified.value)

  const setToken = (newToken, admin = false) => {
    token.value = newToken
    isAdmin.value = admin
    localStorage.setItem('token', newToken)
    localStorage.setItem('isAdmin', String(admin))
  }

  const setUser = (userInfo) => {
    user.value = userInfo
  }

  const userLogin = async (credentials) => {
    const res = await login(credentials)
    const { token: newToken, role, ...userInfo } = res.data
    setToken(newToken, role === 1)
    setUser(userInfo)
    await fetchUserInfo()
    return res
  }

  const userRegister = async (data) => {
    const res = await register(data)
    const { token: newToken, ...userInfo } = res.data
    setToken(newToken, false)
    setUser(userInfo)
    await fetchUserInfo()
    return res
  }

  const adminUserLogin = async (credentials) => {
    const res = await adminLogin(credentials)
    const { token: newToken, ...userInfo } = res.data
    setToken(newToken, true)
    setUser(userInfo)
    await fetchUserInfo()
    return res
  }

  const fetchUserInfo = async () => {
    if (!token.value) return null
    if (fetchPromise.value) return fetchPromise.value

    fetchPromise.value = (async () => {
      try {
        const res = isAdmin.value ? await getAdminInfo() : await getCurrentUser()
        setUser(res.data)
        return user.value
      } catch {
        logout()
        return null
      } finally {
        fetchPromise.value = null
      }
    })()

    return fetchPromise.value
  }

  const updateUserInfo = (newInfo) => {
    if (user.value) {
      user.value = { ...user.value, ...newInfo }
    }
  }

  const logout = () => {
    token.value = ''
    user.value = null
    isAdmin.value = false
    fetchPromise.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('isAdmin')
  }

  const initAuth = async () => {
    if (isInitializing.value || !token.value) return
    isInitializing.value = true
    await fetchUserInfo()
    isInitializing.value = false
  }

  return {
    token,
    user,
    isAdmin,
    isLoggedIn,
    isVerified,
    canRent,
    setToken,
    setUser,
    userLogin,
    userRegister,
    adminUserLogin,
    fetchUserInfo,
    updateUserInfo,
    logout,
    initAuth
  }
}, {
  persist: false
})
