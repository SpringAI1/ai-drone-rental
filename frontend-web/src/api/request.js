import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

const pendingRequests = new Map()

const getRequestKey = (config) => {
  return `${config.method || 'get'}:${config.url}:${JSON.stringify(config.params)}:${JSON.stringify(config.data)}`
}

service.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    const requestKey = getRequestKey(config)

    if (pendingRequests.has(requestKey)) {
      pendingRequests.get(requestKey).cancel()
    }

    const cancelToken = axios.CancelToken.source()
    pendingRequests.set(requestKey, cancelToken)
    config.cancelToken = cancelToken.token

    if (authStore.token) {
      config.headers['Authorization'] = `Bearer ${authStore.token}`
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  (response) => {
    const requestKey = getRequestKey(response.config)
    pendingRequests.delete(requestKey)

    const res = response.data

    if (res.code === 200) {
      return res
    }

    const errorMessage = res.message || '请求失败'

    switch (res.code) {
      case 401:
        handleUnauthorized()
        break
      case 403:
        ElMessage.error('无权限访问')
        break
      default:
        ElMessage.error(errorMessage)
    }

    return Promise.reject(new Error(errorMessage))
  },
  (error) => {
    if (axios.isCancel(error)) return Promise.reject(error)

    const config = error.config
    if (config) {
      const requestKey = getRequestKey(config)
      pendingRequests.delete(requestKey)
    }

    if (error.response) {
      const { status, data } = error.response

      switch (status) {
        case 401:
          handleUnauthorized()
          break
        case 403:
          ElMessage.error('无权限访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          if (!data?.code || data.code !== 401) {
            ElMessage.error(data?.message || '网络错误，请稍后重试')
          }
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else if (!axios.isCancel(error)) {
      ElMessage.error('网络错误，请检查网络连接')
    }

    return Promise.reject(error)
  }
)

let isShowingLoginDialog = false
const handleUnauthorized = () => {
  const authStore = useAuthStore()

  if (isShowingLoginDialog) return
  isShowingLoginDialog = true
  authStore.logout()

  ElMessageBox.confirm('登录状态已过期，请重新登录', '提示', {
    confirmButtonText: '重新登录',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      router.push('/login')
    })
    .finally(() => {
      isShowingLoginDialog = false
    })
}

export default service

export const get = (url, params, config = {}) => {
  return service.get(url, { params, ...config })
}

export const post = (url, data, config = {}) => {
  return service.post(url, data, config)
}

export const put = (url, data, config = {}) => {
  return service.put(url, data, config)
}

export const del = (url, params, config = {}) => {
  return service.delete(url, { params, ...config })
}

export const upload = (url, file, fieldName = 'file', config = {}) => {
  const formData = new FormData()
  formData.append(fieldName, file)

  return service.post(url, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    ...config
  })
}
