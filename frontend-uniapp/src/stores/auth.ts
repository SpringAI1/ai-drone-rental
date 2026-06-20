import { ref } from 'vue'

interface UserInfo {
  id: number
  username: string
  nickname?: string
  phone?: string
  email?: string
  avatar?: string
  balance?: number
  role?: number
}

const safeGetStorage = (key: string): string => {
  try {
    const raw = uni.getStorageSync(key)
    if (raw === null || raw === undefined) return ''
    if (typeof raw === 'string') return raw
    return String(raw)
  } catch (e) {
    return ''
  }
}

const parseUserInfo = (): UserInfo | null => {
  const raw = safeGetStorage('userInfo')
  if (!raw) return null
  try {
    const parsed = JSON.parse(raw)
    if (parsed && typeof parsed === 'object') {
      return {
        id: Number(parsed.id) || 0,
        username: parsed.username || '',
        nickname: parsed.nickname,
        phone: parsed.phone,
        email: parsed.email,
        avatar: parsed.avatar,
        balance: parsed.balance !== undefined ? Number(parsed.balance) : undefined,
        role: parsed.role !== undefined ? Number(parsed.role) : undefined
      }
    }
  } catch (e) {
    // ignore
  }
  return null
}

const token = ref<string>(safeGetStorage('token'))
const userInfo = ref<UserInfo | null>(parseUserInfo())
const isLoggedIn = ref<boolean>(!!safeGetStorage('token'))

export function useAuth() {
  const setToken = (newToken: string) => {
    token.value = newToken
    isLoggedIn.value = !!newToken
    uni.setStorageSync('token', newToken)
  }

  const login = (newToken: string, newUserInfo: UserInfo | null) => {
    token.value = newToken
    userInfo.value = newUserInfo
    isLoggedIn.value = !!newToken
    uni.setStorageSync('token', newToken)
    if (newUserInfo) {
      uni.setStorageSync('userInfo', JSON.stringify(newUserInfo))
    }
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
    isLoggedIn.value = false
    uni.removeStorageSync('token')
    uni.removeStorageSync('userInfo')
  }

  const updateUserInfo = (info: Partial<UserInfo>) => {
    if (!info) return
    const current = userInfo.value
    const merged: UserInfo = {
      id: Number(info.id) || current?.id || 0,
      username: info.username || current?.username || '',
      nickname: info.nickname || current?.nickname,
      phone: info.phone || current?.phone,
      email: info.email || current?.email,
      avatar: info.avatar || current?.avatar,
      balance: info.balance !== undefined ? Number(info.balance) : current?.balance,
      role: info.role !== undefined ? Number(info.role) : current?.role
    }
    userInfo.value = merged
    uni.setStorageSync('userInfo', JSON.stringify(merged))
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    setToken,
    login,
    logout,
    updateUserInfo
  }
}
