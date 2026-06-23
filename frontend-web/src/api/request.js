import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'
import { encryptRequest, decryptResponse, ENCRYPTION_ENABLED } from '@/utils/encryption'

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

// 判断是否需要加密：排除公开接口、GET 查询参数、文件上传
function shouldEncrypt(config) {
  if (!ENCRYPTION_ENABLED) return false
  // GET 请求一般不需要（走 query params，不走 body）
  if ((config.method || '').toLowerCase() === 'get') return false

  const url = (config.url || '').trim()

  // 公开接口（拿公钥本身等）跳过加密
  if (url.startsWith('/public/')) return false
  if (url.includes('/public/rsa-key')) return false

  // 文件上传：multipart/form-data 不走 JSON 加密
  if (url.includes('/common/upload')) return false
  if (url.includes('/uploads/')) return false

  // WebSocket 不走 HTTP 加密
  if (url.startsWith('/ws/')) return false

  // 有 data 才加密（空 body 的 POST/PUT 也跳过）
  if (config.data === undefined || config.data === null || config.data === '') return false

  return true
}

service.interceptors.request.use(
  async (config) => {
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

    // ========== RSA+AES 信封加密 ==========
    if (shouldEncrypt(config)) {
      try {
        const originalData = typeof config.data === 'string'
          ? config.data
          : JSON.stringify(config.data || {})

        const { encryptedKey, encryptedData } = await encryptRequest(originalData)

        // 替换 body 为加密结构
        config.data = { encryptedKey, encryptedData }
        // 标记头：方便后端日志调试
        config.headers['X-Encrypted'] = '1'
      } catch (err) {
        console.error('[encryption] 请求加密失败，降级为明文', err)
        // 加密失败不阻断，降级为明文，保证业务可用
      }
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

    let res = response.data

    // ========== RSA+AES 信封解密 ==========
    if (res && res.encrypted === true && res.data) {
      try {
        res = decryptResponse(res)
      } catch (err) {
        console.error('[encryption] 响应解密失败', err)
        ElMessage.error('响应解密失败')
        return Promise.reject(new Error('响应解密失败'))
      }
    }

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
    confirmButtonText: '确定',
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
