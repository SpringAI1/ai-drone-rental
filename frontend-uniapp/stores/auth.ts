import { reactive } from 'vue'

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

interface AuthState {
  token: string
  userInfo: UserInfo | null
  isLoggedIn: boolean
}

const state = reactive<AuthState>({
  token: uni.getStorageSync('token') || '',
  userInfo: JSON.parse(uni.getStorageSync('userInfo') || 'null'),
  isLoggedIn: !!uni.getStorageSync('token')
})

export function useAuth() {
  const setToken = (token: string) => {
    state.token = token
    state.isLoggedIn = !!token
    uni.setStorageSync('token', token)
  }

  const login = (token: string, userInfo: UserInfo | null) => {
    state.token = token
    state.userInfo = userInfo
    state.isLoggedIn = !!token
    uni.setStorageSync('token', token)
    if (userInfo) {
      uni.setStorageSync('userInfo', JSON.stringify(userInfo))
    }
  }

  const logout = () => {
    state.token = ''
    state.userInfo = null
    state.isLoggedIn = false
    uni.removeStorageSync('token')
    uni.removeStorageSync('userInfo')
  }

  const updateUserInfo = (info: Partial<UserInfo>) => {
    if (info) {
      const merged: UserInfo = {
        id: (info as any).id || state.userInfo?.id || 0,
        username: (info as any).username || state.userInfo?.username || '',
        nickname: (info as any).nickname || state.userInfo?.nickname,
        phone: (info as any).phone || state.userInfo?.phone,
        email: (info as any).email || state.userInfo?.email,
        avatar: (info as any).avatar || state.userInfo?.avatar,
        balance: (info as any).balance !== undefined ? (info as any).balance : state.userInfo?.balance,
        role: (info as any).role !== undefined ? (info as any).role : state.userInfo?.role
      }
      state.userInfo = merged
      uni.setStorageSync('userInfo', JSON.stringify(merged))
    }
  }

  return {
    state,
    setToken,
    login,
    logout,
    updateUserInfo
  }
}
